package com.viettel.sumo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TrafficControlResponse {
    private boolean success;
    private String message;
    private TrafficControlMode currentMode;
    private LocalDateTime timestamp;

    public static TrafficControlResponse success(String message, TrafficControlMode mode) {
        return new TrafficControlResponse(true, message, mode, LocalDateTime.now());
    }

    public static TrafficControlResponse error(String message) {
        return new TrafficControlResponse(false, message, null, LocalDateTime.now());
    }
}
