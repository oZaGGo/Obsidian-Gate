const API_BASE = "/api/auth";

async function login(username, password) {
    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, isAdmin: true })
        });

        const data = await response.json();

        if (response.ok && data.status === "ok") {
            localStorage.setItem('mc_token', data.token);
            localStorage.setItem('mc_user', username);
            window.location.href = "../view/panel.html";
        } else {
            alert(data.message || "Incorrect username or password");
        }
    } catch (error) {
        alert("Server error");
    }
}

async function register(username, password) {
    try {
        const response = await fetch(`${API_BASE}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, isAdmin: true })
        });

        const data = await response.json();

        if (response.ok && data.status === "ok") {
            window.location.href = "../index.html";
        } else {
            alert(data.message || "Registration failed");
        }
    } catch (error) {
        alert("Server error");
    }
}

async function checkSession() {
    const token = localStorage.getItem('mc_token');
    if (!token) {
        window.location.href = "../index.html";
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/validate?token=${token}`);
        if (!response.ok) {
            localStorage.clear();
            window.location.href = "index.html";
        }
    } catch (error) {
        console.error("Session validation failed. Try to log in again.");
    }
}