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

document.addEventListener('DOMContentLoaded', () => {
    checkSession();

    document.getElementById('txt-username').innerText = localStorage.getItem('mc_user') || 'User';

    router.load('home');
});

function logout() {
    localStorage.clear();
    window.location.href = "../index.html";
}

// HOME

/**
 * Update CPU usage display
 * @param {number} percent - 0 - 100
 */
function updateCPU(percent) {
    const card = document.querySelector('.stat-card:nth-child(1)');
    const text = document.getElementById('cpu-usage');
    const fill = card.querySelector('.progress-fill');


    text.innerText = `${percent}%`;
    fill.style.width = `${percent}%`;

    if (percent > 80) fill.style.background = '#e74c3c';
    else if (percent > 50) fill.style.background = '#f1c40f';
    else fill.style.background = 'var(--accent)';
}

/**
 * Update RAM usage display
 * @param {number} used - GB used
 * @param {number} total - GB total
 */
function updateRAM(used, total) {
    const percent = (used / total) * 100;
    const card = document.querySelector('.stat-card:nth-child(2)'); // Segunda tarjeta
    const text = document.getElementById('ram-usage');
    const fill = card.querySelector('.progress-fill');

    text.innerHTML = `${used.toFixed(1)} GB <span class="total-ram">/ ${total} GB</span>`;
    fill.style.width = `${percent}%`;
}