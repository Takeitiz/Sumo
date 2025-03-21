package com.viettel.sumo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viettel.sumo.config.SumoConfig;
import com.viettel.sumo.model.IntersectionConfiguration;
import com.viettel.sumo.model.StageOutputDTO;
import com.viettel.sumo.model.WebsterInputDTO;
import com.viettel.sumo.model.WebsterOutputDTO;
import com.viettel.sumo.util.SumoDataConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sumo.libtraci.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SumoService {
    private final SumoConfig sumoConfig;
    private final SumoDataConverter dataConverter;
    private final ObjectMapper objectMapper;
    private final SumoDataConverter sumoDataConverter;

    @Getter
    private boolean simulationRunning = false;
    private final Map<String, IntersectionConfiguration> intersectionConfigs = new HashMap<>();
    private final Map<String, Queue<Map<String, Double>>> flowHistories = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadIntersectionConfigurations();
    }

    private void loadIntersectionConfigurations() {
        try {
            File configFile = new File(sumoConfig.getIntersectionConfigPath());
            IntersectionConfiguration[] configs = objectMapper.readValue(configFile, IntersectionConfiguration[].class);

            for (IntersectionConfiguration config : configs) {
                intersectionConfigs.put(config.getSumoId(), config);
                log.info("Loaded intersection configuration: " + config);
            }

            log.info("Loaded {} intersection configurations", intersectionConfigs.size());

            System.loadLibrary("libtracijni");

            log.info("Loaded libtracijni successfully");
        } catch (IOException e) {
            log.error("Failed to load intersection configurations", e);
        }
    }

    public void startSimulation() {
        if (simulationRunning) {
            log.warn("Simulation is already running");
            return;
        }

        try {
            System.loadLibrary("libtracijni");

            StringVector sumoCmd;
            if (sumoConfig.isGuiMode()) {
                sumoCmd = new StringVector(new String[]{
                        "sumo-gui",
                        "-c", sumoConfig.getConfigPath(),
                        "--step-length", String.valueOf(sumoConfig.getStepLength()),
                        "--start"
                });
            } else {
                sumoCmd = new StringVector(new String[]{
                        "sumo",
                        "-c", sumoConfig.getConfigPath(),
                        "--step-length", String.valueOf(sumoConfig.getStepLength()),
                        "--start"
                });
            }


            Simulation.start(sumoCmd);
            simulationRunning = true;
            log.info("SUMO simulation started");

            flowHistories.clear();
        } catch (Exception e) {
            log.error("Failed to start SUMO simulation", e);
        }
    }

    public void stopSimulation() {
        if (!simulationRunning) {
            log.warn("No simulation is running");
            return;
        }

        try {
            Simulation.close();
            simulationRunning = false;
            log.info("SUMO simulation stopped");
        } catch (Exception e) {
            log.error("Failed to stop SUMO simulation", e);
        }
    }

    public void stepSimulation() {
        if (!simulationRunning) {
            log.warn("Cannot step simulation: not running");
            return;
        }

        try {
            collectFlowData();

            Simulation.step();

            double simTime = Simulation.getTime();
            if (Math.round(simTime) % 60 == 0) {
                log.info("Simulation time: {} seconds, vehicles: {}",
                        simTime, Simulation.getMinExpectedNumber());
            }
        } catch (Exception e) {
            log.error("Error during simulation step", e);
            stopSimulation();
        }
    }

    private void collectFlowData() {
        // Get all traffic lights
        StringVector tlIDs = TrafficLight.getIDList();

        for (String tlID : tlIDs) {

            if (!intersectionConfigs.containsKey(tlID)) {
                continue;
            }

            Set<String> incomingLanes = sumoDataConverter.getIncomingLanes(tlID);

            Map<String, Double> currentFlows = new HashMap<>();

            for (String lane : incomingLanes) {
                StringVector vehicles = Lane.getLastStepVehicleIDs(lane);
                double count = vehicles.size();
                currentFlows.put(lane, count);
            }

            Queue<Map<String, Double>> history = flowHistories.computeIfAbsent(
                    tlID, k-> new LinkedList<>()
            );

            history.add(currentFlows);

            int windowSize = sumoConfig.getOptimizationInterval();
            while (history.size() > windowSize) {
                history.poll();
            }
        }
    }

    public WebsterInputDTO prepareWebsterInput(String tlID) {
        // Get intersection configuration
        IntersectionConfiguration config = intersectionConfigs.get(tlID);
        if (config == null) {
            log.warn("No configuration found for traffic light: {}", tlID);
            return null;
        }
        Queue<Map<String, Double>> flowHistory = flowHistories.get(tlID);
        if (flowHistory == null || flowHistory.isEmpty()) {
            log.warn("No flow history available for traffic light: {}", tlID);
            return null;
        }
        log.info("Flow history size: {}", flowHistory.size());
        return dataConverter.createWebsterInput(config, flowHistory);
    }

    public void applyWebsterOutput(String tlID, WebsterOutputDTO output) {
        try {
            IntersectionConfiguration config = intersectionConfigs.get(tlID);
            if (config == null) {
                log.warn("No configuration found for traffic light: {}", tlID);
                return;
            }

            Map<Long, Integer> stageToPhaseMap = new HashMap<>();
            for (IntersectionConfiguration.StageConfig stageConfig : config.getStages()) {
                stageToPhaseMap.put(stageConfig.getId(), stageConfig.getPhaseIndex());
            }

            String currentProgramID = TrafficLight.getProgram(tlID);
            TraCILogic currentLogic = TrafficLight.getCompleteRedYellowGreenDefinition(tlID).get(0);
            TraCIPhaseVector currentPhases = currentLogic.getPhases();

            for (StageOutputDTO stageOutput : output.getEffectiveGreenTimes()) {
                Long stageId = stageOutput.getStageId();
                Integer phaseIndex = stageToPhaseMap.get(stageId);

                if (phaseIndex == null) {
                    log.warn("No phase index found for stage ID: {}", stageId);
                    continue;
                }

                int greenPhaseIndex = phaseIndex * 3;

                if (greenPhaseIndex < currentPhases.size()) {
                    TraCIPhase currentPhase = currentPhases.get(greenPhaseIndex);
                    TraCIPhase newGreenPhase = new TraCIPhase(
                            stageOutput.getGreenTime(),
                            currentPhase.getState()
                    );
                    currentPhases.set(greenPhaseIndex, newGreenPhase);

                    log.info("Updated phase {} for stage {} with green time: {} seconds",
                            greenPhaseIndex, stageId, stageOutput.getGreenTime());
                } else {
                    log.warn("Phase index {} out of bounds for traffic light: {}", greenPhaseIndex, tlID);
                }
            }

            TraCILogic newLogic = new TraCILogic(
                    currentProgramID,
                    currentLogic.getType(),
                    currentLogic.getCurrentPhaseIndex(),
                    currentPhases
            );

            TrafficLight.setProgramLogic(tlID, newLogic);

            log.info("Successfully applied Webster output to traffic light: {}", tlID);

        } catch (Exception e) {
            log.error("Error applying Webster output to traffic light: {}", tlID, e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (simulationRunning) {
            stopSimulation();
        }
    }
}
