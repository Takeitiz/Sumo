package com.viettel.sumo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WebsterOutputDTO {
    private double cycleLength;
    private List<StageOutputDTO> effectiveGreenTimes;
}
