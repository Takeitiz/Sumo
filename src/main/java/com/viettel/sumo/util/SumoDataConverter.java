package com.viettel.sumo.util;

import com.viettel.sumo.model.*;
import org.eclipse.sumo.libtraci.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SumoDataConverter {

    // Get incoming lanes controlled by a traffic light.
    public Set<String> getIncomingLanes(String tlID) {
        Set<String> incomingLanes = new HashSet<>();

        TraCILinkVectorVector controlledLinks = TrafficLight.getControlledLinks(tlID);

        for (TraCILinkVector linkVector : controlledLinks) {
            for (TraCILink link : linkVector) {
                incomingLanes.add(link.getFromLane());
            }
        }

        return incomingLanes;
    }

    public WebsterInputDTO createWebsterInput(IntersectionConfiguration config, Queue<Map<String, Double>> flowHistory) {
        WebsterInputDTO input = new WebsterInputDTO();

        input.setSaturationVolume(config.getSaturationVolume());

        List<StageDTO> stages = new ArrayList<>();
        for (IntersectionConfiguration.StageConfig stageConfig : config.getStages()) {
            StageDTO stage = new StageDTO();
            stage.setId(stageConfig.getId());
            stage.setOldId(stageConfig.getOldId());
            stage.setMinGreenTime(stageConfig.getMinGreenTime());
            stage.setMaxGreenTime(stageConfig.getMaxGreenTime());
            stage.setYellow(stageConfig.getYellow());
            stage.setRedClear(stageConfig.getRedClear());
            stage.setWeight(stageConfig.getWeight());

            List<LampDTO> lamps = new ArrayList<>();
            for (IntersectionConfiguration.LampConfig lampConfig : stageConfig.getLamps()) {
                LampDTO lamp = new LampDTO();
                lamp.setDirection(lampConfig.getDirection());
                lamp.setRoute(lampConfig.getRoute());
                lamps.add(lamp);
            }
            stage.setLamps(lamps);
            stages.add(stage);
        }
        input.setStages(stages);

        List<RoadDTO> roads = new ArrayList<>();
        for (IntersectionConfiguration.RoadConfig roadConfig : config.getRoads()) {
            RoadDTO road = new RoadDTO();
            road.setDirection(roadConfig.getDirection());
            road.setNumberOfLanes(roadConfig.getNumberOfLanes());

            List<FlowDataDTO> flows = new ArrayList<>();
            for (IntersectionConfiguration.FlowConfig flowConfig : roadConfig.getFlows()) {
                FlowDataDTO flow = new FlowDataDTO();
                flow.setDirection(flowConfig.getDirection());
                flow.setRoute(flowConfig.getRoute());

                double flowRate = calculateAverageFlow(flowConfig.getLane(), flowHistory);
                flow.setFlowData(flowRate * 3600);

                flows.add(flow);
            }
            road.setFlows(flows);
            roads.add(road);
        }
        input.setRoads(roads);

        System.out.println(input);
        return input;
    }

    // TODO: Sau thảo luận lại cách tính flow với anh Kiên.
    private double calculateAverageFlow(String lane, Queue<Map<String, Double>> flowHistory) {
        double sum = 0;
        int count = 0;

        for (Map<String, Double> flows : flowHistory) {
            if (flows.containsKey(lane)) {
                sum += flows.get(lane);
                count++;
            }
        }

        return count > 0 ? sum / count : 0;
    }

}
