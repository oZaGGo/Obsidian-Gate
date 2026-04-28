const router = {
    async load(viewName) {
        const container = document.getElementById('view-container');

        try {
            const response = await fetch(`../view/section/${viewName}.html`);

            if (!response.ok) throw new Error("Section not found!");

            const html = await response.text();

            container.innerHTML = html;

        } catch (error) {
            container.innerHTML = `
                <div class="error-view">
                    <h2>Error 404</h2>
                    <p>Section not available: ${viewName}</p>
                </div>`;
        }

        this.initViewLogic(viewName);
        this.updateActiveMenu(viewName);
    },

    updateActiveMenu(viewName) {
        document.querySelectorAll('.menu-item').forEach(btn => {
            btn.classList.remove('active');
            const clickHandler = btn.getAttribute('onclick');
            if (clickHandler && clickHandler.includes(`'${viewName}'`)) {
                btn.classList.add('active');
            }
        });
    },

    initViewLogic(viewName) {
        if (viewName === 'server') {
            loadSettings();
        }
    }
};

let online = false;

document.addEventListener('DOMContentLoaded', () => {
    checkSession();
    document.getElementById('txt-username').innerText = localStorage.getItem('mc_user') || 'User';

    router.load('home');

    if (online){
        refreshMetrics()
    }
    checkServerStatus();
    refreshLogs();

    setInterval(() => {

        if (document.getElementById('cpu-usage')) {
            checkSession();
            if (online){
                refreshMetrics()
            }
            checkServerStatus();
            refreshLogs();
        }
    }, 1000);
});

function logout() {
    localStorage.clear();
    window.location.href = "../index.html";
}

// HOME

function updateCPU(percent) {
    const card = document.querySelector('.stat-card:nth-child(1)');
    const text = document.getElementById('cpu-usage');
    const fill = card.querySelector('.progress-fill');


    text.innerText = `${percent}%`;
    fill.style.width = `${percent}%`;

    if (percent > 80) fill.style.background = '#da6459';
    else if (percent > 50) fill.style.background = '#eed67e';
    else fill.style.background = 'var(--accent)';
}

function updateRAM(used, total) {
    const percent = (used / total) * 100;
    const card = document.querySelector('.stat-card:nth-child(2)'); // Segunda tarjeta
    const text = document.getElementById('ram-usage');
    const fill = card.querySelector('.progress-fill');

    text.innerHTML = `${used.toFixed(1)} GB <span class="total-ram">/ ${total} GB</span>`;
    fill.style.width = `${percent}%`;
}

async function refreshMetrics() {

    const token = localStorage.getItem('mc_token');

    if (!token) {
        window.location.href = "../index.html";
        return;
    }

    try {
        const response = await fetch('/api/system/metrics', {
            method: 'GET',
            headers: {
                'Authorization': token,
                'Content-Type': 'application/json'
            }
        });

        if (response.status === 401) {
            localStorage.clear();
            window.location.href = "../index.html";
            return;
        }

        const data = await response.json();
        updateCPU(data.cpuUsage);
        updateRAM(data.ramUsed, data.ramTotal);

    } catch (e) {
        console.error("Server error");
    }
}


// SERVER RUNTIME

async function controlServer(action) {
    const token = localStorage.getItem('mc_token');
    setControlButtonsDisabled(true);
    try {
        const response = await fetch(`/api/server/control?action=${action}`, {
            method: 'POST',
            headers: { 'Authorization': token }
        });

        if (response.ok) {
            checkServerStatus();
        } else {
            const errorText = await response.text();
            alert("Error: " + errorText);
        }
    } catch (e) {
        console.error("Server error", e);
    }finally {
        setControlButtonsDisabled(false);
    }
}

async function refreshLogs() {
    const consoleOutput = document.getElementById('console-output');
    if (!consoleOutput) return;

    try {
        const response = await fetch('/api/server/logs', {
            headers: { 'Authorization': localStorage.getItem('mc_token') }
        });
        const logs = await response.json();

        consoleOutput.innerHTML = logs.map(line => {
            let color = "#ccc";
            if (line.includes("ERROR")) color = "#da6459";
            if (line.includes("WARN")) color = "#eed67e";
            if (line.includes("INFO")) color = "#7ebbee";

            return `<p class="line" style="color: ${color}">${line}</p>`;
        }).join('');

    } catch (e) {
        console.error("Loggin error");
    }
}

async function sendCommand() {
    const input = document.getElementById('cmd-input');
    const command = input.value.trim();
    const token = localStorage.getItem('mc_token');

    if (!command || !online) return;

    try {
        const response = await fetch('/api/server/command', {
            method: 'POST',
            headers: {
                'Authorization': token,
                'Content-Type': 'text/plain'
            },
            body: command
        });

        if (response.ok) {
            input.value = '';
            refreshLogs();
        } else {
            console.error("Error enviando comando");
        }
    } catch (e) {
        console.error("Connection error", e);
    }
}

async function checkServerStatus() {
    const dot = document.getElementById('server-status-dot');
    const text = document.getElementById('server-status-text');
    const uptimeText = document.getElementById('status');

    if (!dot || !text) return;

    try {
        const response = await fetch('/api/server/status', {
            headers: { 'Authorization': localStorage.getItem('mc_token') }
        });
        const data = await response.json();

        if (data.running) {
            dot.style.background = "#2ecc71";
            text.innerText = "ONLINE";
            text.style.color = "#2ecc71";
            online = true;

            if (uptimeText) {
                uptimeText.innerText = data.uptime;
            }

        } else {
            dot.style.background = "#e74c3c";
            text.innerText = "OFFLINE";
            text.style.color = "#e74c3c";
            online = false;
        }
    } catch (e) {
        dot.style.background = "#95a5a6";
    }
}

function setControlButtonsDisabled(disabled) {
    const buttons = document.querySelectorAll('.control-actions .btn');
    buttons.forEach(btn => {
        btn.disabled = disabled;
        btn.style.opacity = disabled ? "0.5" : "1";
        btn.style.cursor = disabled ? "not-allowed" : "pointer";
    });
}


// SERVER CONFIG

async function loadSettings() {
    const token = localStorage.getItem('mc_token');
    try {
        const response = await fetch('/api/config/get', {
            headers: { 'Authorization': token }
        });
        const data = await response.json();

        document.getElementById('set-name').value = data.name;
        document.getElementById('set-description').value = data.description;
        document.getElementById('set-ram').value = data.maxGbRam;
        document.getElementById('set-render').value = data.renderDistance;
        document.getElementById('set-sim').value = data.simulationDistance;
        document.getElementById('set-players').value = data.maxPlayers;
        document.getElementById('set-rcon').value = data.rconPort;

        document.querySelectorAll('output').forEach(out => {
            const input = out.previousElementSibling;
            if(input.type === 'range') out.value = input.value;
        });
    } catch (e) {
        console.error("Error loading settings");
    }
}

function handleIconSelection(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        document.getElementById('settings-icon-preview').src = e.target.result;
    };
    reader.readAsDataURL(file);

    uploadIcon(file);
}

async function uploadIcon() {
    const fileInput = document.getElementById('icon-input');
    const file = fileInput.files[0];

    if (!file) {
        alert("File missing.");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    const token = localStorage.getItem('mc_token');

    try {
        const response = await fetch('/api/config/icon/upload', {
            method: 'POST',
            headers: { 'Authorization': token },
            body: formData
        });

        if (response.ok) {
            alert("Icon succsessfully added");
            const timestamp = new Date().getTime();
            const newUrl = `/api/config/icon?t=${timestamp}`;

            if(document.getElementById('settings-icon-preview'))
                document.getElementById('settings-icon-preview').src = newUrl;
            if(document.getElementById('dashboard-icon'))
                document.getElementById('dashboard-icon').src = newUrl;

        } else {
            alert("Error uploading icon");
        }
    } catch (e) {
        console.error("Error updating", e);
    }
}

async function saveSettings() {
    const token = localStorage.getItem('mc_token');
    const settings = {
        name: document.getElementById('set-name').value,
        description: document.getElementById('set-description').value,
        maxGbRam: parseInt(document.getElementById('set-ram').value),
        renderDistance: parseInt(document.getElementById('set-render').value),
        simulationDistance: parseInt(document.getElementById('set-sim').value),
        maxPlayers: parseInt(document.getElementById('set-players').value),
        rconPort: parseInt(document.getElementById('set-rcon').value)
    };

    try {
        const response = await fetch('/api/config/save', {
            method: 'POST',
            headers: {
                'Authorization': token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(settings)
        });

        if (response.ok) {
            loadSettings()
            alert("Settings saved! Restart the server to apply changes.");
        }
    } catch (e) {
        alert("Error saving settings");
    }
}