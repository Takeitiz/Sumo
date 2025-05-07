package com.viettel.sumo.controller;

import com.viettel.sumo.model.TrafficControlMode;
import com.viettel.sumo.model.TrafficControlResponse;
import com.viettel.sumo.service.TrafficControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing traffic light control modes.
 * Provide endpoints to switch between different operational modes
 * and query the current mode.
 */
@RestController
@RequestMapping("/api/traffic-control")
@Slf4j
@RequiredArgsConstructor
public class TrafficControlController {
    private final TrafficControlService trafficControlService;

    /**
     * Switch to a specific traffic control mode
     * @param mode The desired control mode
     * @return Response indicating success or failure
     */
    @PostMapping("/mode/{mode}")
    public ResponseEntity<TrafficControlResponse> setControlMode(@PathVariable TrafficControlMode mode) {
        try {
            trafficControlService.setControlMode(mode);
            return ResponseEntity.ok(TrafficControlResponse.success(
                    "Successfully switched to " + mode, mode));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(TrafficControlResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to set control mode", e);
            return ResponseEntity.internalServerError()
                    .body(TrafficControlResponse.error("Failed to set control mode: " + e.getMessage()));
        }
    }

    /**
     * Get the current traffic control mode
     * @return The current mode
     */
    @GetMapping("/mode")
    public ResponseEntity<TrafficControlMode> getCurrentMode() {
        return ResponseEntity.ok(trafficControlService.getCurrentMode());
    }

    /**
     * Convenience endpoint to activate red mode (emergency stop)
     */
    @PostMapping("/emergency-stop")
    public ResponseEntity<TrafficControlResponse> activateEmergencyStop() {
        return setControlMode(TrafficControlMode.RED_MODE);
    }

    /**
     * Convenience endpoint to return to normal operation
     */
    @PostMapping("/resume-normal")
    public ResponseEntity<TrafficControlResponse> resumeNormal() {
        return setControlMode(TrafficControlMode.NORMAL_MODE);
    }

    /**
     * Convenience endpoint to advance to next phase
     */
    @PostMapping("/next-phase")
    public ResponseEntity<TrafficControlResponse> advanceToNextPhase() {
        return setControlMode(TrafficControlMode.NEXT_PHASE_MODE);
    }
}