// Portal animation
async function initPortal() {
    const entryScreen = document.getElementById('entry-screen');
    const portal = document.getElementById('portal-overlay');
    const panel = document.querySelector('.panel-layout');
    const sound = document.getElementById('portal-sound');
    const soundLoaded = document.getElementById('portal-sound-loaded');
    sound.volume = 0.3;
    soundLoaded.volume = 0.3;

    sound.play();

    portal.style.display = 'block';
    entryScreen.style.opacity = '0';

    const versionsPromise = fetch('/api/welcome/versions').then(res => res.json());

    setTimeout(async () => {
        entryScreen.style.display = 'none';
        panel.style.display = 'block';
        createParticles();

        try {
            const versions = await versionsPromise;
            renderVersions(versions);
        } catch (e) {
            document.getElementById('version-grid').innerHTML = "Failed to connect to Mojang.";
        }

        setTimeout(() => {
            portal.style.transition = 'opacity 1s ease';
            portal.style.opacity = '0';
            setTimeout(() => {
                portal.style.display = 'none';
            }, 500);
            soundLoaded.play();
        }, 3500);

    }, 800);
}

// Particles

function createParticles() {
    const container = document.getElementById('particles-container');
    const particleCount = 70;

    for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div');
        particle.classList.add('particle');

        const posX = Math.floor(Math.random() * 100);
        const delay = Math.random() * 10;
        const duration = 5 + Math.random() * 10;
        const size = Math.random() * 8 + 4;

        particle.style.left = posX + 'vw';
        particle.style.animationDelay = delay + 's';
        particle.style.animationDuration = duration + 's';
        particle.style.width = size + 'px';
        particle.style.height = size + 'px';
        particle.style.background = Math.random() > 0.5 ? '#7d5fff' : '#a29bfe';

        container.appendChild(particle);
    }
}

// Version rendering

function renderVersions(versions) {
    const select = document.getElementById('version-select');
    if (!select) return;

    select.innerHTML = '<option value="" disabled selected>— Choose a Version —</option>';

    versions.forEach(v => {
        const option = document.createElement('option');
        option.value = v;
        option.textContent = `Minecraft ${v}`;
        select.appendChild(option);
    });
}

// Initialize config

async function startInitialConfig() {
    const versionSelect = document.getElementById('version-select');
    const geminiInput = document.getElementById('gemini-api-key');
    const startBtn = document.querySelector('.start-config-btn');

    if (!versionSelect.value) {
        alert("Please select a target version for the portal.");
        return;
    }

    startBtn.disabled = true;
    startBtn.innerHTML = "SYNCING PORTAL...";
    startBtn.style.boxShadow = "0 0 30px rgba(125, 95, 255, 0.8)";

    const payload = {
        version: versionSelect.value,
        geminiApiKey: geminiInput.value
    };

    try {
        const response = await fetch('/api/welcome/start-config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': localStorage.getItem('mc_token')
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (response.ok) {
            console.log("Success:", data.message);
            document.querySelector('.settings-container').style.opacity = "0";
            setTimeout(() => {
                window.location.href = "../view/panel.html";
            }, 1000);
        } else {
            throw new Error(data.message || "Portal error");
        }
    } catch (error) {
        console.error("Error:", error);
        alert("Failed to start configuration: " + error.message);
        startBtn.disabled = false;
        startBtn.innerHTML = "START CONFIGURATION";
    }
}