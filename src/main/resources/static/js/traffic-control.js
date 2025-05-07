// Global settings
const API_BASE = window.location.origin + '/api';
let simulationRunning = false;

// Function to update simulation status
async function updateSimulationStatus() {
    try {
        const response = await fetch(`${API_BASE}/simulation/status`);
        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const statusText = await response.text();
        const isRunning = statusText.includes("running");
        simulationRunning = isRunning;

        // Update status display
        const statusElement = document.getElementById('simulationStatus');
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

        // Update button states
        document.getElementById('startSimulation').disabled = isRunning;
        document.getElementById('stopSimulation').disabled = !isRunning;
        document.getElementById('runSteps').disabled = !isRunning;
    } catch (error) {
        console.error('Failed to fetch simulation status:', error);
        addLogEntry(`Failed to fetch simulation status: ${error.message}`, 'error');
    }
}

// Function to start simulation
async function startSimulation() {
    try {
        addLogEntry('Starting simulation...', 'info');

        const response = await fetch(`${API_BASE}/simulation/start`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const result = await response.text();
        addLogEntry(result, 'success');
        await updateSimulationStatus();
    } catch (error) {
        console.error('Failed to start simulation:', error);
        addLogEntry(`Failed to start simulation: ${error.message}`, 'error');
    }
}

// Function to stop simulation
async function stopSimulation() {
    try {
        addLogEntry('Stopping simulation...', 'info');

        const response = await fetch(`${API_BASE}/simulation/stop`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const result = await response.text();
        addLogEntry(result, 'success');
        await updateSimulationStatus();
    } catch (error) {
        console.error('Failed to stop simulation:', error);
        addLogEntry(`Failed to stop simulation: ${error.message}`, 'error');
    }
}

// Function to run simulation for specific steps
async function runSimulationSteps(steps) {
    try {
        addLogEntry(`Running simulation for ${steps} steps...`, 'info');

        const response = await fetch(`${API_BASE}/simulation/run/${steps}`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const result = await response.text();
        addLogEntry(result, 'success');
        await updateSimulationStatus();
    } catch (error) {
        console.error('Failed to run simulation steps:', error);
        addLogEntry(`Failed to run simulation steps: ${error.message}`, 'error');
    }
}

// Function to update traffic control mode status
async function updateTrafficControlStatus() {
    try {
        const response = await fetch(`${API_BASE}/traffic-control/mode`);
        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const mode = await response.text();
        document.getElementById('currentStatus').textContent = `Traffic Control Mode: ${mode}`;

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
async function setTrafficControlMode(mode) {
    if (!simulationRunning) {
        addLogEntry('Cannot change traffic control mode: Simulation is not running', 'warning');
        return;
    }

    try {
        addLogEntry(`Setting traffic control mode to ${mode}...`, 'info');

        const response = await fetch(`${API_BASE}/traffic-control/mode/${mode}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const result = await response.json();
        addLogEntry(result.message || 'Mode changed successfully', result.success ? 'success' : 'error');

        await updateTrafficControlStatus();
    } catch (error) {
        console.error('Failed to set traffic control mode:', error);
        addLogEntry(`Failed to set traffic control mode: ${error.message}`, 'error');
    }
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

// Initialize dashboard
function initDashboard() {
    // Add event listeners to simulation control buttons
    document.getElementById('startSimulation').addEventListener('click', startSimulation);
    document.getElementById('stopSimulation').addEventListener('click', stopSimulation);
    document.getElementById('runSteps').addEventListener('click', () => {
        const steps = parseInt(document.getElementById('stepsInput').value, 10);
        if (isNaN(steps) || steps < 1) {
            addLogEntry('Invalid number of steps. Please enter a positive number.', 'warning');
            return;
        }
        runSimulationSteps(steps);
    });

    // Add event listeners to traffic control buttons
    document.querySelectorAll('.control-panel .control-button').forEach(button => {
        button.addEventListener('click', function() {
            const mode = this.getAttribute('data-mode');
            setTrafficControlMode(mode);
        });
    });

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

    addLogEntry('Dashboard initialized', 'info');
}

// Run initialization when DOM is loaded
document.addEventListener('DOMContentLoaded', initDashboard);