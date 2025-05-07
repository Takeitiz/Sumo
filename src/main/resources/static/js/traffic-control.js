// Global settings
const API_BASE = window.location.origin + '/api';
let simulationRunning = false;

// Debug function - log initialization steps
function debugLog(message) {
    console.log(`[DEBUG] ${message}`);
    // Also log to the UI if the log element exists
    const log = document.getElementById('log');
    if (log) {
        const entry = document.createElement('div');
        entry.className = 'log-entry info';
        entry.textContent = `[DEBUG] ${message}`;
        log.insertBefore(entry, log.firstChild);
    }
}

// Function to update simulation status
async function updateSimulationStatus() {
    try {
        debugLog('Fetching simulation status...');
        const response = await fetch(`${API_BASE}/simulation/status`);
        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const statusText = await response.text();
        const isRunning = statusText.includes("running");
        simulationRunning = isRunning;
        debugLog(`Simulation status: ${statusText} (isRunning=${isRunning})`);

        // Update status display
        const statusElement = document.getElementById('simulationStatus');
        if (statusElement) {
            statusElement.textContent = statusText;

            if (isRunning) {
                statusElement.classList.add('running');
                statusElement.classList.remove('stopped');
                enableTrafficControls();
            } else {
                statusElement.classList.add('stopped');
                statusElement.classList.remove('running');
                disableTrafficControls();
            }
        }

        // IMPORTANT: Explicitly update button states with direct assignments
        const startButton = document.getElementById('startSimulation');
        const stopButton = document.getElementById('stopSimulation');
        const runStepsButton = document.getElementById('runSteps');

        if (startButton) {
            startButton.disabled = isRunning;
            debugLog(`Start button disabled set to: ${isRunning}`);
        }
        if (stopButton) {
            stopButton.disabled = !isRunning;
        }
        if (runStepsButton) {
            runStepsButton.disabled = !isRunning;
        }
    } catch (error) {
        console.error('Failed to fetch simulation status:', error);
        addLogEntry(`Failed to fetch simulation status: ${error.message}`, 'error');
    }
}

// Function to start simulation
function startSimulation() {
    debugLog('Start simulation function called');
    addLogEntry('Starting simulation...', 'info');

    fetch(`${API_BASE}/simulation/start`, {
        method: 'POST'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error: ${response.status}`);
            }
            return response.text();
        })
        .then(result => {
            addLogEntry(result, 'success');
            updateSimulationStatus();
        })
        .catch(error => {
            console.error('Failed to start simulation:', error);
            addLogEntry(`Failed to start simulation: ${error.message}`, 'error');
        });
}

// Function to stop simulation
function stopSimulation() {
    debugLog('Stop simulation function called');
    addLogEntry('Stopping simulation...', 'info');

    fetch(`${API_BASE}/simulation/stop`, {
        method: 'POST'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error: ${response.status}`);
            }
            return response.text();
        })
        .then(result => {
            addLogEntry(result, 'success');
            updateSimulationStatus();
        })
        .catch(error => {
            console.error('Failed to stop simulation:', error);
            addLogEntry(`Failed to stop simulation: ${error.message}`, 'error');
        });
}

// Function to run simulation for specific steps
function runSimulationSteps() {
    const stepsInput = document.getElementById('stepsInput');
    if (!stepsInput) {
        console.error('Steps input element not found');
        return;
    }

    const steps = parseInt(stepsInput.value, 10);
    if (isNaN(steps) || steps < 1) {
        addLogEntry('Invalid number of steps. Please enter a positive number.', 'warning');
        return;
    }

    debugLog(`Running simulation for ${steps} steps`);
    addLogEntry(`Running simulation for ${steps} steps...`, 'info');

    fetch(`${API_BASE}/simulation/run/${steps}`, {
        method: 'POST'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error: ${response.status}`);
            }
            return response.text();
        })
        .then(result => {
            addLogEntry(result, 'success');
            updateSimulationStatus();
        })
        .catch(error => {
            console.error('Failed to run simulation steps:', error);
            addLogEntry(`Failed to run simulation steps: ${error.message}`, 'error');
        });
}

// Function to update traffic control mode status
async function updateTrafficControlStatus() {
    try {
        const response = await fetch(`${API_BASE}/traffic-control/mode`);
        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const mode = await response.text();
        const statusElement = document.getElementById('currentStatus');
        if (statusElement) {
            statusElement.textContent = `Traffic Control Mode: ${mode}`;
        }

        // Highlight the active button
        document.querySelectorAll('.control-panel .control-button').forEach(button => {
            button.classList.remove('active');
        });

        // Find the button for this mode and highlight it
        const modeValue = mode.replace(/"/g, '');
        document.querySelectorAll('.control-panel .control-button').forEach(button => {
            if (button.getAttribute('data-mode') === modeValue) {
                button.classList.add('active');
            }
        });
    } catch (error) {
        console.error('Failed to fetch traffic control mode:', error);
        addLogEntry(`Failed to fetch traffic control mode: ${error.message}`, 'error');
    }
}

// Function to set traffic control mode
function setTrafficControlMode(mode) {
    if (!simulationRunning) {
        addLogEntry('Cannot change traffic control mode: Simulation is not running', 'warning');
        return;
    }

    addLogEntry(`Setting traffic control mode to ${mode}...`, 'info');

    fetch(`${API_BASE}/traffic-control/mode/${mode}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error: ${response.status}`);
            }
            return response.json();
        })
        .then(result => {
            addLogEntry(result.message || 'Mode changed successfully', result.success ? 'success' : 'error');
            updateTrafficControlStatus();
        })
        .catch(error => {
            console.error('Failed to set traffic control mode:', error);
            addLogEntry(`Failed to set traffic control mode: ${error.message}`, 'error');
        });
}

// Function to enable traffic control buttons
function enableTrafficControls() {
    document.querySelectorAll('.control-panel .control-button').forEach(button => {
        button.disabled = false;
    });
}

// Function to disable traffic control buttons
function disableTrafficControls() {
    document.querySelectorAll('.control-panel .control-button').forEach(button => {
        button.disabled = true;
    });
}

// Function to add log entry
function addLogEntry(message, type = 'info') {
    const log = document.getElementById('log');
    if (!log) {
        console.error('Log element not found');
        return;
    }

    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;

    const timestamp = new Date().toLocaleTimeString();
    entry.textContent = `[${timestamp}] ${message}`;

    log.insertBefore(entry, log.firstChild);

    // Limit log entries
    const entries = log.querySelectorAll('.log-entry');
    if (entries.length > 100) {
        log.removeChild(entries[entries.length - 1]);
    }
}

// Function to manually enable start button (for debugging)
function forceEnableStartButton() {
    const startButton = document.getElementById('startSimulation');
    if (startButton) {
        startButton.disabled = false;
        startButton.onclick = startSimulation;
        debugLog('Start button manually enabled');
    } else {
        debugLog('Start button not found for manual enabling');
    }
}

// Primary initialization function
function initDashboard() {
    debugLog('Initializing dashboard...');

    // Get buttons and check if they exist
    const startButton = document.getElementById('startSimulation');
    const stopButton = document.getElementById('stopSimulation');
    const runStepsButton = document.getElementById('runSteps');

    debugLog(`Found start button: ${!!startButton}`);
    debugLog(`Found stop button: ${!!stopButton}`);
    debugLog(`Found run steps button: ${!!runStepsButton}`);

    // Use direct assignment for onclick handlers
    if (startButton) {
        startButton.onclick = startSimulation;
        startButton.disabled = false; // Explicitly enable
        debugLog('Attached onclick handler to start button');
    }

    if (stopButton) {
        stopButton.onclick = stopSimulation;
        debugLog('Attached onclick handler to stop button');
    }

    if (runStepsButton) {
        runStepsButton.onclick = runSimulationSteps;
        debugLog('Attached onclick handler to run steps button');
    }

    // Add event listeners to traffic control buttons
    document.querySelectorAll('.control-panel .control-button').forEach(button => {
        button.onclick = function() {
            const mode = this.getAttribute('data-mode');
            setTrafficControlMode(mode);
        };
    });
    debugLog('Attached onclick handlers to control panel buttons');

    // Initial status updates
    updateSimulationStatus();
    updateTrafficControlStatus();

    // Set up periodic updates
    setInterval(() => {
        updateSimulationStatus();
        if (simulationRunning) {
            updateTrafficControlStatus();
        }
    }, 5000);

    // Force enable start button with a slight delay
    setTimeout(forceEnableStartButton, 1000);

    addLogEntry('Dashboard initialized', 'info');
    debugLog('Dashboard initialization complete');
}

// Multiple initialization hooks to ensure the dashboard is initialized
document.addEventListener('DOMContentLoaded', function() {
    debugLog('DOMContentLoaded event fired');
    initDashboard();
});

// Fallback initialization
window.onload = function() {
    debugLog('Window onload event fired');
    // Check if buttons have been initialized
    const startButton = document.getElementById('startSimulation');
    if (startButton && !startButton.onclick) {
        debugLog('Buttons not initialized from DOMContentLoaded, initializing now');
        initDashboard();
    } else {
        debugLog('Buttons already initialized, skipping duplicate init');
        forceEnableStartButton(); // Still force enable the start button
    }
};

// Last resort initialization - run after a delay
setTimeout(function() {
    debugLog('Delayed initialization check');
    const startButton = document.getElementById('startSimulation');
    if (startButton && !startButton.onclick) {
        debugLog('Buttons still not initialized, forcing initialization');
        initDashboard();
    } else if (startButton) {
        debugLog('Button appears initialized, forcing enable just in case');
        forceEnableStartButton();
    }
}, 2000);

// Expose debug function globally for troubleshooting from console
window.debugDashboard = function() {
    const startButton = document.getElementById('startSimulation');
    debugLog(`Start button exists: ${!!startButton}`);
    if (startButton) {
        debugLog(`Start button disabled: ${startButton.disabled}`);
        debugLog(`Start button has onclick: ${!!startButton.onclick}`);
        forceEnableStartButton();
    }
    updateSimulationStatus();
};

// Initial debug log
debugLog('Script loaded');