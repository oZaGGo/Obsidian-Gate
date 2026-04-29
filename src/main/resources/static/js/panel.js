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
        if (viewName === 'home') {
            refreshLogs();
            moveScroll();
        }
        if (viewName === 'server') {
            loadSettings();
        }
        if (viewName === 'world') {
            loadWorldView()

            let debounceTimer;
            document.getElementById('world-name').addEventListener('input', function(e) {
                const name = e.target.value.trim();
                const inputElement = e.target;

                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(async () => {
                    if (!name) {
                        inputElement.style.color = '';
                        return;
                    }

                    const response = await fetch(`/api/world/${encodeURIComponent(name)}`, {
                        headers: { 'Authorization': localStorage.getItem('mc_token') }
                    });

                    const data = await response.json();
                    inputElement.style.color = data.world ? '#5fc78f' : '#dbb26b';
                    inputElement.style.fontWeight = data.world ? 'bold' : 'normal';
                }, 300);

            });
        }
        if (viewName === 'security') {
            loadUsers()
        }
    }
};

let online = false;

document.addEventListener('DOMContentLoaded', () => {

    checkSession();

    document.getElementById('txt-username').innerText = localStorage.getItem('mc_user') || 'User';

    router.load('home');

    if (online){
        refreshMetrics();
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

function moveScroll(){
    const consoleOutput = document.getElementById('console-output');
    consoleOutput.scrollTo({
        top: consoleOutput.scrollHeight,
        behavior: 'smooth'
    });
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

function copyToClipboard(text, element) {
    navigator.clipboard.writeText(text).then(() => {

        const label = element.querySelector('#copy-label');
        const originalText = label.innerText;
        const originalColor = element.style.borderColor;

        label.innerText = "Copied!";
        label.style.color = "#ffffff";
        element.style.borderColor = "#ffffff";

        setTimeout(() => {
            label.innerText = originalText;
            label.style.color = "#00ffff";
            element.style.borderColor = "rgba(0, 255, 255, 0.4)";
        }, 1500);

    }).catch(err => {
        console.error('Copy error: ', err);
    });
}

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


// World

async function loadWorldView() {
    const token = localStorage.getItem('mc_token');

    try {
        const currentRes = await fetch('/api/world/current', {
            headers: { 'Authorization': token }
        });

        const currentWorld = await currentRes.json().catch(() => null);

        if (currentWorld) {
            document.getElementById('world-name').value = currentWorld.nombre;
            document.getElementById('world-seed').value = currentWorld.seed || '';
            document.getElementById('world-difficulty').value = currentWorld.difficulty;
            document.getElementById('world-gamemode').value = currentWorld.gamemode;
            document.getElementById('world-hardcore').checked = currentWorld.hardcore;
        } else {
            const nameInput = document.getElementById('world-name');
            const seedInput = document.getElementById('world-seed');

            nameInput.value = '';
            nameInput.placeholder = 'Type a name for your world...';

            seedInput.value = '';
            seedInput.placeholder = 'Leave empty for random seed';

            document.getElementById('world-difficulty').value = 'NORMAL';
            document.getElementById('world-gamemode').value = 'SURVIVAL';
            document.getElementById('world-hardcore').checked = false;
        }

        const allRes = await fetch('/api/world/all', {
            headers: { 'Authorization': token }
        });
        const allWorlds = await allRes.json();
        renderWorldTable(allWorlds);

    } catch (error) {
        console.error("Error loading worlds:", error);
    }
}

function renderWorldTable(worlds) {
    const tbody = document.getElementById('world-list-body');
    tbody.innerHTML = '';

    // XSS protection
    const escapeHTML = (str) => {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    };

    worlds.forEach(w => {
        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${escapeHTML(w.nombre)}</td>
            <td><span class="badge badge-gm">${escapeHTML(w.gamemode)}</span></td>
            <td><span class="badge badge-diff">${escapeHTML(w.difficulty)}</span></td>
            <td>${escapeHTML(w.hardcore)}</td>
            <td>
                <span class="${w.current ? 'status-active' : 'status-inactive'}">
                    ${w.current ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="actions-cell">
                <div style="display: flex; gap: 8px;" class="btn-container">
                    </div>
            </td>
        `;

        const btnContainer = row.querySelector('.btn-container');

        if (w.current) {
            const btnDefault = document.createElement('button');
            btnDefault.className = 'btn-select-disabled';
            btnDefault.disabled = true;
            btnDefault.textContent = 'Default';
            btnContainer.appendChild(btnDefault);
        } else {
            const btnSet = document.createElement('button');
            btnSet.className = 'btn-select';
            btnSet.textContent = 'Set Default';
            btnSet.onclick = () => setWorldDefault(w.nombre);
            btnContainer.appendChild(btnSet);

            const btnDel = document.createElement('button');
            btnDel.className = 'btn-delete';
            btnDel.textContent = 'Delete';
            btnDel.onclick = () => deleteWorld(w.nombre);
            btnContainer.appendChild(btnDel);
        }

        tbody.appendChild(row);
    });
}

async function deleteWorld(worldName) {
    if (!confirm(`Are you sure you want to delete the world "${worldName}"?`)) return;

    try {
        const response = await fetch(`/api/world/delete?name=${worldName}`, {
            method: 'DELETE',
            headers: { 'Authorization': localStorage.getItem('mc_token') }
        });

        if (response.ok) {
            loadWorldView(); // Recargar tabla
        } else {
            alert("Error deleting world");
        }
    } catch (error) {
        console.error("Delete error:", error);
    }
}

async function saveWorldConfig() {
    const nameInput = document.getElementById('world-name');
    const seedInput = document.getElementById('world-seed');
    const difficultyInput = document.getElementById('world-difficulty');
    const gamemodeInput = document.getElementById('world-gamemode');
    const hardcoreInput = document.getElementById('world-hardcore');

    if (!nameInput.value.trim()) {
        alert("Please enter a world name.");
        nameInput.focus();
        return;
    }

    if (!difficultyInput.value.trim()) {
        alert("Please enter a difficulty.");
        difficultyInput.focus();
        return;
    }

    if (!gamemodeInput.value.trim()) {
        alert("Please enter a gamemode.");
        gamemodeInput.focus();
        return;
    }

    const worldData = {
        nombre: nameInput.value.trim().replace(/\s+/g, '_'),
        seed: seedInput.value.trim(),
        difficulty: difficultyInput.value,
        gamemode: gamemodeInput.value,
        hardcore: hardcoreInput.checked,
        current: true
    };

    try {
        const token = localStorage.getItem('mc_token');

        const response = await fetch('/api/world/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token
            },
            body: JSON.stringify(worldData)
        });

        if (response.ok) {
            loadWorldView();
            checkWorldExists()
        } else {
            const errorData = await response.text();
            alert("Error saving world: " + errorData);
        }
    } catch (error) {
        alert("Server error");
    }
}

async function setWorldDefault(worldName) {
    try {
        const response = await fetch(`/api/world/set-active?name=${worldName}`, {
            method: 'POST',
            headers: {
                'Authorization': localStorage.getItem('mc_token')
            }
        });

        if (response.ok) {
            loadWorldView();
            checkWorldExists()
        }
    } catch (error) {
        console.error("Error setting default world:", error);
    }
}

async function checkWorldExists() {
    const input = document.getElementById('world-name');
    const name = input.value.trim();
    const token = localStorage.getItem('mc_token');

    if (!name) {
        input.style.color = '';
        return;
    }

    try {
        const response = await fetch(`/api/world/${name}`, {
            headers: { 'Authorization': token }
        });

        const data = await response.json();

        input.style.color = data.world ? '#5fc78f' : '#dbb26b'
        input.style.fontWeight = data.world ? 'bold' : 'normal';

    } catch (e) {
        console.error("Validation error");
    }
}

// Security

async function loadUsers() {
    const userListBody = document.getElementById('user-list-body');
    if (!userListBody) return;

    // XSS Protection
    const escapeHTML = (str) => {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    };

    try {
        const response = await fetch('/api/auth/users', {
            headers: { 'Authorization': localStorage.getItem('mc_token') }
        });

        if (!response.ok) throw new Error("Could not fetch users");

        const users = await response.json();

        userListBody.innerHTML = '';

        users.forEach(user => {
            const row = document.createElement('tr');

            const safeUsername = escapeHTML(user.username);
            const lastConn = user.lastConn
                ? new Date(user.lastConn).toLocaleString()
                : '<span style="color: #666">Never</span>';

            const roleBadge = user.admin
                ? `<span class="badge" style="background: #da6459; color: white;">ADMIN</span>`
                : `<span class="badge" style="background: #7ebbee; color: white;">MANAGER</span>`;

            row.innerHTML = `
                <td>${user.admin ? `<strong>${safeUsername}</strong>` : safeUsername}</td>
                <td>${roleBadge}</td>
                <td>${lastConn}</td>
                <td style="text-align: right;" class="actions-cell"></td>
            `;

            const actionsCell = row.querySelector('.actions-cell');

            if (user.admin) {
                const btnProtected = document.createElement('button');
                btnProtected.className = 'btn-select-disabled';
                btnProtected.disabled = true;
                btnProtected.textContent = 'Protected';
                actionsCell.appendChild(btnProtected);
            } else {
                const btnDelete = document.createElement('button');
                btnDelete.className = 'btn-delete';

                btnDelete.innerHTML = '<i class="fas fa-trash"></i> Remove';

                btnDelete.onclick = () => deleteUser(user.username);

                actionsCell.appendChild(btnDelete);
            }

            userListBody.appendChild(row);
        });

    } catch (e) {
        console.error("Error loading users:", e);
    }
}

async function createNewManager() {
    const nameInput = document.getElementById('new-user-name');
    const passInput = document.getElementById('new-user-password');

    const username = nameInput.value.trim();
    const password = passInput.value.trim();

    if (!username || !password) {
        alert("Please fill in all fields");
        return;
    }

    if (password.length < 8) {
        alert("Password must be at least 8 characters long");
        return;
    }

    try {
        const response = await fetch('/api/auth/manager', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': localStorage.getItem('mc_token')
            },
            body: JSON.stringify({ username, password, isAdmin: false })
        });

        const data = await response.json();

        if (response.ok) {
            // Limpiar inputs
            nameInput.value = '';
            passInput.value = '';
            // Recargar tabla
            loadUsers();
            alert("Manager created successfully");
        } else {
            alert(data.message || "Error creating manager");
        }
    } catch (e) {
        console.error("Error:", e);
        alert("Connection error");
    }
}

async function deleteUser(username) {
    if (!confirm(`Are you sure you want to remove access for ${username}?`)) {
        return;
    }

    try {
        const response = await fetch(`/api/auth/user/${username}`, {
            method: 'DELETE',
            headers: { 'Authorization': localStorage.getItem('mc_token') }
        });

        const data = await response.json();

        if (response.ok) {
            loadUsers();
        } else {
            alert(data.message || "Error deleting user");
        }
    } catch (e) {
        console.error("Error:", e);
        alert("Connection error");
    }
}