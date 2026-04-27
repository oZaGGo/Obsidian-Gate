const router = {
    async load(viewName) {
        const container = document.getElementById('view-container');

        try {
            const response = await fetch(`../view/section/${viewName}.html`);

            if (!response.ok) throw new Error("Section not found!");

            const html = await response.text();

            container.innerHTML = html;

            this.updateActiveMenu(viewName);

            this.initViewLogic(viewName);

        } catch (error) {
            container.innerHTML = `
                <div class="error-view">
                    <h2>Error 404</h2>
                    <p>Section not available: ${viewName}</p>
                </div>`;
        }
    },

    updateActiveMenu(viewName) {
        document.querySelectorAll('.menu-item').forEach(btn => {
            btn.classList.remove('active');
            if (btn.getAttribute('onclick').includes(viewName)) {
                btn.classList.add('active');
            }
        });
    },

    initViewLogic(viewName) {
        if (viewName === 'servidores') {
            console.log("Iniciando carga de servidores...");
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