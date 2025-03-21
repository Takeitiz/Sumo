package com.viettel.sumo.controller;

import org.eclipse.sumo.libtraci.IntStringPair;
import org.eclipse.sumo.libtraci.Simulation;
import org.eclipse.sumo.libtraci.StringVector;
import org.eclipse.sumo.libtraci.Vehicle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test-traci")
    public String testTraci() {
        try {
            // Try to load the native library explicitly
            System.loadLibrary("libtracijni");

            // Try to get libtraci version info
            String versionInfo = "libtraci loaded successfully!";

            try {
                // This will likely fail if SUMO isn't running, but the library should load
                StringVector sumoCmd = new StringVector(new String[]{
                        "sumo-gui",  // Use GUI version to see the simulation
                        "-c", "C:/Users/tuand/Downloads/2025-03-10-16-09-25/2025-03-10-16-09-25/osm.sumocfg",
                        "--start"  // Important: tells SUMO to start immediately
                });
                Simulation.start(sumoCmd);
                IntStringPair version = Simulation.getVersion();
                versionInfo += "\nConnected to SUMO successfully!";
                versionInfo += "\nSUMO version: " + version;

                for (int i = 0; i < 10; i++) {
                    Simulation.step();
                }

                // Close SUMO when done
                Simulation.close();
                versionInfo += "\nRan 10 simulation steps and closed SUMO.";
            } catch (Exception e) {
                versionInfo += "\nCouldn't connect to SUMO (normal if SUMO isn't running)";
                versionInfo += "\nError message: " + e.getMessage();
                versionInfo += "\nBut the library itself loaded correctly!";
            }

            return versionInfo;

        } catch (UnsatisfiedLinkError e) {
            return "FAILED: Native library couldn't be loaded!\n" +
                    "Error message: " + e.getMessage() + "\n" +
                    "Current java.library.path: " + System.getProperty("java.library.path") + "\n\n" +
                    "Please check that:\n" +
                    "1. The native library (libtracijni.dll/so/dylib) exists in your SUMO bin directory\n" +
                    "2. You've set the java.library.path correctly to include that directory\n" +
                    "3. You're using the correct architecture (32-bit vs 64-bit)";
        } catch (NoClassDefFoundError e) {
            return "FAILED: Java classes from libtraci JAR couldn't be found!\n" +
                    "Error message: " + e.getMessage() + "\n\n" +
                    "Please check that:\n" +
                    "1. The libtraci JAR file is in your classpath\n" +
                    "2. You're using the correct version of the JAR file";
        } catch (Exception e) {
            return "FAILED: Unexpected error occurred!\n" +
                    "Error type: " + e.getClass().getName() + "\n" +
                    "Error message: " + e.getMessage();
        }
    }
}
