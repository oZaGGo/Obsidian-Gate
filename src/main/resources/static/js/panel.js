const router = {
    async load(viewName) {
        const container = document.getElementById('view-container');

        try {
            const response = await fetch(`../view/section/${viewName}.html`);

            if (!response.ok) throw new Error("Vista no encontrada");

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