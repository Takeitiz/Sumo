package com.viettel.sumo.service;

import com.viettel.sumo.config.WebsterConfig;
import com.viettel.sumo.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsterService {
    private final WebsterConfig websterConfig;

    public WebsterOutputDTO calculateWebster(WebsterInputDTO input) {
        log.info("Starting Webster calculation for intersection with {} stages",
                input.getStages().size());

        int numberOfStages = input.getStages().size();
        double saturationVolume = input.getSaturationVolume();
        log.debug("Saturation volume: {}", saturationVolume);

        List<StageDTO> stages = input.getStages();
        double totalYellowAndAllRedTime = calculateTotalYellowAndRedClear(stages);
        log.debug("Total yellow and all red-clear time: {}", totalYellowAndAllRedTime);

        Map<Long, Double> stageVolumes = calculateStageVolumes(stages, input.getRoads());

        double totalVolumeOfAllStages = 0;
        for (Map.Entry<Long, Double> entry : stageVolumes.entrySet()) {
            totalVolumeOfAllStages += entry.getValue();
        }

        log.info("Total critical volume across all stages: {}", totalVolumeOfAllStages);

        for (Map.Entry<Long, Double> entry : stageVolumes.entrySet()) {
            log.debug("Stage ID: {}, Critical volume: {}", entry.getKey(), entry.getValue());
        }

        double cycle = calculateCycleTime(numberOfStages, totalVolumeOfAllStages, saturationVolume);
        log.info("Initial calculated cycle time: {} seconds", cycle);

        List<StageOutputDTO> effectiveGreenTimes = new ArrayList<>(numberOfStages);

        if (cycle <= 0) {
            log.warn("Calculated cycle time is invalid ({}). Using fallback values.", cycle);
            cycle = 0;

            for (int i = 0; i < numberOfStages; i++) {
                StageDTO stage = stages.get(i);
                StageOutputDTO stageOutputDTO = createDefaultStageOutput(stage);
                effectiveGreenTimes.add(stageOutputDTO);

                cycle += stage.getMaxGreenTime() + stage.getRedClear() + stage.getYellow();
                log.debug("Stage {}: using max green time of {} seconds",
                        stage.getId(), stage.getMaxGreenTime());
            }

            log.info("Fallback cycle time: {} seconds", cycle);
            return new WebsterOutputDTO(cycle, effectiveGreenTimes);
        }

        double totalMinGreenTime = numberOfStages * websterConfig.getMinGreenTimePerStage();
        log.debug("Total minimum green time: {} seconds", totalMinGreenTime);

        double finalCycle = calculateFinalCycleAndGreenTimes(
                effectiveGreenTimes,
                stages,
                cycle,
                totalVolumeOfAllStages,
                totalMinGreenTime,
                totalYellowAndAllRedTime,
                stageVolumes
        );

        log.info("Final calculated cycle time: {} seconds", finalCycle);

        for (StageOutputDTO stage : effectiveGreenTimes) {
            log.debug("Stage ID: {}, Green: {}s, Yellow: {}s, Red-clear: {}s",
                    stage.getStageId(), stage.getGreenTime(),
                    stage.getYellowTime(), stage.getRedClearTime());
        }

        return new WebsterOutputDTO(finalCycle, effectiveGreenTimes);
    }

    private int calculateTotalYellowAndRedClear(List<StageDTO> stages) {
        return stages.stream()
                .mapToInt(stage -> stage.getRedClear() + stage.getYellow())
                .sum();
    }

    private Map<Long, Double> calculateStageVolumes(List<StageDTO> stages, List<RoadDTO> roads) {
        Map<Long, Double> result = new HashMap<>();

        Map<Long, Set<String>> stageLampDirections = new HashMap<>();

        for (StageDTO stage : stages) {
            Set<String> lampDirections = stage.getLamps().stream()
                    .map(lamp -> lamp.getDirection() + " " + lamp.getRoute())
                    .collect(Collectors.toSet());
            stageLampDirections.put(stage.getId(), lampDirections);
        }

        Map<String, List<FlowDataDTO>> directionToFlowMap = new HashMap<>();

        for (RoadDTO road : roads) {
            for (FlowDataDTO flowData : road.getFlows()) {
                String key = flowData.getDirection() + " " + flowData.getRoute();
                directionToFlowMap.computeIfAbsent(key, k -> new ArrayList<>()).add(flowData);
            }
        }

        for (StageDTO stage : stages) {
            Set<String> lampDirections = stageLampDirections.get(stage.getId());
            double criticalFlow = 0.0;

            for (String direction : lampDirections) {
                List<FlowDataDTO> matchingFlows = directionToFlowMap.getOrDefault(direction, Collections.emptyList());

                Optional<Double> maxFlow = matchingFlows.stream()
                        .map(FlowDataDTO::getFlowData)
                        .max(Double::compareTo);

                if (maxFlow.isPresent() && maxFlow.get() > criticalFlow) {
                    criticalFlow = maxFlow.get();
                }
            }

            result.put(stage.getId(), criticalFlow);
        }

        return result;
    }

    private double calculateLostTime(int numberOfStages) {
        double lostTime = 2 * numberOfStages + websterConfig.getBaseLostTime();
        log.debug("Lost time calculated as {} seconds for {} stages", lostTime, numberOfStages);
        return lostTime;
    }

    private double calculateCycleTime(int numberOfStages, double totalVolumeOfAllStages, double saturationVolume) {
        double lostTime = calculateLostTime(numberOfStages);
        double yCritical = totalVolumeOfAllStages / saturationVolume;

        log.debug("Critical flow ratio (Y): {}", yCritical);

        if (yCritical >= 1.0) {
            log.warn("Critical flow ratio exceeds 1.0 ({}), indicating oversaturation", yCritical);
        }

        return (1.5 * lostTime + 5) / (1 - yCritical);
    }

    private double calculateFinalCycleAndGreenTimes(
            List<StageOutputDTO> effectiveGreenTimes,
            List<StageDTO> stages,
            double cycle,
            double totalVolumeOfAllStages,
            double totalMinimumGreenTime,
            double totalYellowAndAllRedTime,
            Map<Long, Double> stageVolumes) {
        double finalCycle = totalYellowAndAllRedTime;
        double availableGreenTime = cycle - totalYellowAndAllRedTime - totalMinimumGreenTime;

        log.debug("Available green time for distribution: {} seconds", availableGreenTime);

        for (StageDTO stage : stages) {
            StageOutputDTO stageOutputDTO = new StageOutputDTO();
            stageOutputDTO.setStageId(stage.getId());
            stageOutputDTO.setRedClearTime(stage.getRedClear());
            stageOutputDTO.setYellowTime(stage.getYellow());
            stageOutputDTO.setOldId(stage.getOldId());

            double minGreenTime = totalMinimumGreenTime * stage.getWeight();
            double currentStageVolume = stageVolumes.getOrDefault(stage.getId(), 0.0);

            double greenTime;
            if (totalVolumeOfAllStages > 0) {
                greenTime = availableGreenTime * currentStageVolume / totalVolumeOfAllStages + minGreenTime;
            } else {
                greenTime = minGreenTime;
                log.warn("Total volume is zero, using minimum green time for stage {}", stage.getId());
            }

            if (Double.isNaN(greenTime) || Double.isInfinite(greenTime)) {
                greenTime = (stage.getMinGreenTime() + stage.getMaxGreenTime()) / 2.0;
                log.warn("Invalid green time calculation for stage {}. Using average of min/max: {}", stage.getId(), greenTime);
            }

            int finalGreenTime = (int) Math.round(
                    Math.min(Math.max(greenTime, stage.getMinGreenTime()), stage.getMaxGreenTime())
            );

            stageOutputDTO.setGreenTime(finalGreenTime);
            effectiveGreenTimes.add(stageOutputDTO);
            finalCycle += finalGreenTime;
        }

        return finalCycle;
    }

    private StageOutputDTO createDefaultStageOutput(StageDTO stage) {
        StageOutputDTO output = new StageOutputDTO();
        output.setStageId(stage.getId());
        output.setRedClearTime(stage.getRedClear());
        output.setYellowTime(stage.getYellow());
        output.setOldId(stage.getOldId());
        output.setGreenTime(stage.getMaxGreenTime());
        return output;
    }
}
