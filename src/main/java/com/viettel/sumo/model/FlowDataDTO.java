package com.viettel.sumo.model;

import lombok.Data;

@Data
public class FlowDataDTO {
    private String direction;
    private String route;
    private double flowData;
}
