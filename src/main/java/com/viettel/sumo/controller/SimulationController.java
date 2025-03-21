package com.viettel.sumo.controller;

import com.viettel.sumo.service.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@Slf4j
@RequiredArgsConstructor
public class SimulationController {
    private final SimulationService simulationService;

    @PostMapping("/start")
    public ResponseEntity<String> startSimulation() {
        if (simulationService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body("Simulation is already running");
        }

        try {
            simulationService.startSimulation();
            return ResponseEntity.ok("Simulation started successfully");
        } catch (Exception e) {
            log.error("Failed to start simulation", e);
            return ResponseEntity.internalServerError().body("Failed to start simulation: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopSimulation() {
        if (!simulationService.isSimulationRunning()) {
            return ResponseEntity.badRequest().body("No simulation is running");
        }

        try {
            simulationService.stopSimulation();
            return ResponseEntity.ok("Simulation stopped successfully");
        } catch (Exception e) {
            log.error("Failed to stop simulation", e);
            return ResponseEntity.internalServerError().body("Failed to stop simulation: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getSimulationStatus() {
        boolean running = simulationService.isSimulationRunning();
        return ResponseEntity.ok(running ? "Simulation is running" : "Simulation is not running");
    }

    @PostMapping("/run/{steps}")
    public ResponseEntity<String> runSimulationForSteps(@PathVariable int steps) {
        if (steps <= 0) {
            return ResponseEntity.badRequest().body("Number of steps must be positive");
        }

        try {
            simulationService.runSimulationFor(steps);
            return ResponseEntity.ok("Simulation ran for " + steps + " steps");
        } catch (Exception e) {
            log.error("Failed to run simulation for steps", e);
            return ResponseEntity.internalServerError().body("Failed to run simulation: " + e.getMessage());
        }
    }
}
