package com.viettel.sumo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntersectionConfiguration {
    private String sumoId;
    private String intersectionId;
    private Double saturationVolume;
    private List<StageConfig> stages;
    private List<RoadConfig> roads;

    @Data
    public static class StageConfig {
        private Long id;
        private String oldId;
        private Integer phaseIndex;
        private Integer minGreenTime;
        private Integer maxGreenTime;
        private Integer yellow;
        private Integer redClear;
        private Double weight;
        private List<LampConfig> lamps;
    }

    @Data
    public static class LampConfig {
//        private Long id;
//        private String direction;
//        private String route;
//        private String lane;
        private String direction;
        private String route;
    }

    @Data
    public static class RoadConfig {
//        private Long id;
//        private String name;
//        private List<FlowConfig> flows;
        private String direction;
        private int numberOfLanes;
        private List<FlowConfig> flows;
    }

    @Data
    public static class FlowConfig {
//        private Long id;
//        private String direction;
//        private String route;
//        private String lane;
        private String direction;
        private String route;
        private double flowData;
        private String lane;
    }
}
