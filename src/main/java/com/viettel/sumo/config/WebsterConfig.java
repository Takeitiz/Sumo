package com.viettel.sumo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "webster")
@Data
public class WebsterConfig {
    private double defaultSaturationVolume = 1900;
    private int baseLostTime = 15;
    private int minGreenTimePerStage =15;
    private int flowDataCollectionWindow = 60; // seconds
}
