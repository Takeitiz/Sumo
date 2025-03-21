package com.viettel.sumo.model;

import lombok.Data;

import java.util.List;

@Data
public class RoadDTO {
    private String direction;
    private int numberOfLanes;
    private List<FlowDataDTO> flows;
}
