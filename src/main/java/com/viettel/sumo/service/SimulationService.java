package com.viettel.sumo.service;

import com.viettel.sumo.config.SumoConfig;
import com.viettel.sumo.model.WebsterInputDTO;
import com.viettel.sumo.model.WebsterOutputDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sumo.libtraci.Simulation;
import org.eclipse.sumo.libtraci.StringVector;
import org.eclipse.sumo.libtraci.TrafficLight;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {
    private final SumoService sumoService;
    private final WebsterService websterService;
    private final SumoConfig sumoConfig;

    public void startSimulation() {
        sumoService.startSimulation();
    }

    public void stopSimulation() {
        sumoService.stopSimulation();
    }

    public boolean isSimulationRunning() {
        return sumoService.isSimulationRunning();
    }

    @Scheduled(fixedRate = 1000)
    public void runSimulationStep() {
        if (!sumoService.isSimulationRunning()) {
            return;
        }

        sumoService.stepSimulation();

        double simTime = Simulation.getTime();
        if (simTime > 0 && simTime % sumoConfig.getOptimizationInterval() == 0) {
            optimizeTrafficSignals();
        }
    }

    private void optimizeTrafficSignals() {
        log.info("Optimizing traffic signals at simulation time: {}", Simulation.getTime());

        StringVector tlIDs = TrafficLight.getIDList();

        for (String tlID : tlIDs) {

            WebsterInputDTO input = sumoService.prepareWebsterInput(tlID);
            if (input == null) {
                continue;
            }

            WebsterOutputDTO output = websterService.calculateWebster(input);

            sumoService.applyWebsterOutput(tlID, output);
        }
    }

    public void runSimulationFor(int steps) {
        if (!sumoService.isSimulationRunning()) {
            sumoService.startSimulation();
        }

        for (int i = 0; i < steps; i++) {
            sumoService.stepSimulation();

            double simTime = Simulation.getTime();
            if (simTime > 0 && simTime % sumoConfig.getOptimizationInterval() == 0) {
                optimizeTrafficSignals();
            }
        }
    }
}
