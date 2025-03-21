package com.viettel.sumo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sumo")
@Data
public class SumoConfig {
    private String executablePath = "sumo";
    private String configPath;
    private int port = 8813;
    private boolean guiMode = true;
    private double stepLength = 1.0;
    private int optimizationInterval = 300; // seconds
    private String intersectionConfigPath;
}
