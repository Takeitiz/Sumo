package com.viettel.sumo.model;

import lombok.Data;

@Data
public class StageOutputDTO {
    private long stageId;
    private String oldId;
    private int greenTime;
    private int redClearTime;
    private int yellowTime;
}
