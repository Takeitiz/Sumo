package com.viettel.sumo.model;

/**
 * Represents different operational modes for traffic light control.
 * Each mode defines a specific behavior for all traffic lights in the system.
 */
public enum TrafficControlMode {

    /**
     * All traffic lights display red signal - used for emergency situations
     */
    RED_MODE,

    /**
     * All traffic lights display yellow signal - indicates caution
     */
    YELLOW_MODE,

    /**
     * Normal operation - traffic lights follow their configured programs
     */
    NORMAL_MODE,

    /**
     * Manual control to advance to the next phase in the cycle
     */
    NEXT_PHASE_MODE,

    /**
     * All traffic lights are turned off - used for maintenance or special events
     */
    LIGHTS_OFF_MODE,

    /**
     * Adaptive control using Webster algorithm to optimize signal timings
     */
    ADAPTIVE_MODE
}
