async function openModal(templateName, parameters = {}) {
    const dialog = document.getElementById('dynamicModal')
    const modalContent = document.getElementById('modalContent')

    if (!dialog || !modalContent) {
        console.error("Modal elements not found")
        return
    }

    try {
        const response = await fetch(`../view/modal/${templateName}.html`)

        if (!response.ok) throw new Error("Section not found!")

        let html = await response.text()
        modalContent.innerHTML = html

    } catch (error) {
        modalContent.innerHTML = `
            <div class="error-view">
                <h2>Error 404</h2>
                <p>Error loading fields.</p>
            </div>`
    }

    await initModalStructure(templateName, dialog, parameters)

    const closeBtn = document.getElementById('closeDynamicModal')
    if (closeBtn) {
        closeBtn.onclick = () => dialog.close()
    }
}

async function initModalStructure(templateName, dialog, parameters) {

    if (templateName=='changePassword') {
        const userName = document.getElementById("userSelected")
        userName.innerText = parameters.username

        dialog.showModal()

        const changeBtn = document.getElementById('btnChangePassword')
        if (changeBtn) {
            changeBtn.onclick = () => changePassword(parameters.username)
        }
    }

}

async function changePassword(username) {
    const passwordInput = document.getElementById('newPassword') || document.querySelector('input[type="password"]')
    const password = passwordInput ? passwordInput.value.trim() : ''

    if (!password) {
        alert("Please enter a new password")
        return
    }

    if (password.length < 8) {
        alert("Password must be at least 8 characters long")
        return
    }

    if (!confirm(`Are you sure you want to change password for ${username}?`)) {
        return
    }

    try {
        const response = await fetch(`${API_BASE}/password`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': localStorage.getItem('mc_token')
            },
            body: JSON.stringify({ username, password, isAdmin: false, setupCompleted: true })
        });

        const data = await response.json()

        if (response.ok) {
            alert(data.message || "Password changed successfully")
            document.getElementById('dynamicModal').close()
        } else {
            alert(data.message || "Error changing password")
        }
    } catch (e) {
        console.error("Error:", e)
        alert("Connection error.")
    }

}