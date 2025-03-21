package com.viettel.sumo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class WebsterInputDTO {
    private double saturationVolume;
    private List<StageDTO> stages;
    private List<RoadDTO> roads;
}
