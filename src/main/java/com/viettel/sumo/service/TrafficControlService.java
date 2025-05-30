package com.viettel.sumo.service;

import com.viettel.sumo.model.TrafficControlMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sumo.libtraci.StringVector;
import org.eclipse.sumo.libtraci.TraCILogic;
import org.eclipse.sumo.libtraci.TraCILogicVector;
import org.eclipse.sumo.libtraci.TrafficLight;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing different traffic control modes.
 * This service acts as a facade to control all traffic lights in the simulation
 * according to the selected operational mode.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficControlService {
    private final SumoService sumoService;

    private TrafficControlMode currentMode = TrafficControlMode.NORMAL_MODE;

    /**
     * Switch to a new traffic control mode.
     * This method handles the transition between different operational modes.
     */
    public void setControlMode(TrafficControlMode mode) {
        if (!sumoService.isSimulationRunning()) {
            throw new IllegalStateException("Simulation must be running to change control mode");
        }

        log.info("Switching from {} to {}", currentMode, mode);
        this.currentMode = mode;

        switch (mode) {
            case RED_MODE:
                setAllLightsToRed();
                break;
            case YELLOW_MODE:
                setAllLightsToYellow();
                break;
            case NORMAL_MODE:
                resetToNormalOperation();
                break;
            case NEXT_PHASE_MODE:
                advanceToNextPhase();
                // Switch back to previous mode after advancing
                this.currentMode = TrafficControlMode.NORMAL_MODE;
                break;
            case LIGHTS_OFF_MODE:
                turnOffAllLights();
                break;
            case ADAPTIVE_MODE:
                enableAdaptiveControl();
                break;
        }
    }

    /**
     * Get the current control mode
     */
    public TrafficControlMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Set all traffic lights to red.
     * This creates an immediate stop for all traffic in the simulation.
     */
    private void setAllLightsToRed() {
        StringVector tlIDs = TrafficLight.getIDList();
        for (String tlID : tlIDs) {
            String currentState = TrafficLight.getRedYellowGreenState(tlID);
            // Replace all characters with 'r' (red)
            String allRedState = "r".repeat(currentState.length());
            TrafficLight.setRedYellowGreenState(tlID, allRedState);
            log.debug("Set traffic light {} to all red", tlID);
        }
    }

    /**
     * Set all traffic lights to yellow.
     * This indicates caution at all intersections.
     */
    private void setAllLightsToYellow() {
        StringVector tlIDs = TrafficLight.getIDList();
        for (String tlID : tlIDs) {
            String currentState = TrafficLight.getRedYellowGreenState(tlID);
            // Replace all characters with 'y' (yellow)
            String allYellowState = "y".repeat(currentState.length());
            TrafficLight.setRedYellowGreenState(tlID, allYellowState);
            log.debug("Set traffic light {} to all yellow", tlID);
        }
    }

    /**
     * Reset traffic lights to their normal programmed operation.
     */
    private void resetToNormalOperation() {
        StringVector tlIDs = TrafficLight.getIDList();
        for (String tlID : tlIDs) {
            try {
                // Get the current program ID
                String programID = TrafficLight.getProgram(tlID);

                // Get the complete definition
                TraCILogicVector logics = TrafficLight.getCompleteRedYellowGreenDefinition(tlID);
                if (!logics.isEmpty()) {
                    // Reset the program logic
                    TrafficLight.setProgramLogic(tlID, logics.get(0));

                    // Reset the phase (optionally go to phase 0)
                    TrafficLight.setPhase(tlID, 0);

                    // Set the phase duration to default
                    TrafficLight.setPhaseDuration(tlID, -1);

                    log.debug("Reset traffic light {} to normal operation", tlID);
                }
            } catch (Exception e) {
                log.error("Error resetting traffic light {}: {}", tlID, e.getMessage());
            }
        }
    }

    /**
     * Advance all traffic lights to their next phase.
     */
    private void advanceToNextPhase() {
        StringVector tlIDs = TrafficLight.getIDList();
        for (String tlID : tlIDs) {
            try {
                // Get current phase and program information
                int currentPhase = TrafficLight.getPhase(tlID);
                TraCILogic logic = TrafficLight.getCompleteRedYellowGreenDefinition(tlID).get(0);
                int numberOfPhases = logic.getPhases().size();

                if (numberOfPhases <= 1) {
                    log.info("Traffic light {} has only {} phase(s), cannot advance", tlID, numberOfPhases);
                    continue;
                }

                // Determine the current major phase (0, 3, or 6 in your case)
                int currentMajorPhase = currentPhase - (currentPhase % 3);

                // Calculate the next major phase (with wraparound)
                int nextMajorPhase = (currentMajorPhase + 3) % 9;

                // Set the next phase
                TrafficLight.setPhase(tlID, nextMajorPhase);
                log.debug("Advanced traffic light {} from phase {} to major phase {}",
                        tlID, currentPhase, nextMajorPhase);
            } catch (Exception e) {
                log.error("Error advancing phase for traffic light {}: {}", tlID, e.getMessage());
            }
        }
    }

    /**
     * Turn off all traffic lights.
     * This sets all lights to off (essentially, all dark)
     */
    private void turnOffAllLights() {
        StringVector tlIDs = TrafficLight.getIDList();
        for (String tlID : tlIDs) {
            String currentState = TrafficLight.getRedYellowGreenState(tlID);
            // Replace all characters with 'O' (off state)
            String allOffState = "O".repeat(currentState.length());
            TrafficLight.setRedYellowGreenState(tlID, allOffState);
            log.debug("Turned off traffic light {}", tlID);
        }
    }

    /**
     * Enable adaptive control using Webster algorithm.
     * This is the existing behavior of the system.
     */
    private void enableAdaptiveControl() {
        // This mode is handled by the existing SimulationService logic
        log.info("Adaptive control enabled - using Webster algorithm");
    }
}
