package com.viettel.sumo.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StageDTO {
    private long id;
    private String oldId;
    private double weight;
    private int minGreenTime;
    private int maxGreenTime;
    private int yellow;
    private int redClear;
    private List<LampDTO> lamps = new ArrayList<>();
}
