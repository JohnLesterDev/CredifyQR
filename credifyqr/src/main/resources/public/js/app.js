// Base URL for API
const API = '/api';

// Helper to get cookie value
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

// Check if user is logged in by calling /api/session
async function checkSession() {
    try {
        const resp = await fetch(`${API}/session`, { credentials: 'same-origin' });
        if (resp.ok) {
            const user = await resp.json();
            return user;
        }
    } catch (e) {
        console.error('Session check failed', e);
    }
    return null;
}

// Login form handler
async function login(username, password) {
    const formData = new URLSearchParams();
    formData.append('username', username);
    formData.append('password', password);

    const resp = await fetch(`${API}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData,
        credentials: 'same-origin'
    });
    if (resp.ok) {
        return await resp.text();
    } else {
        throw new Error(await resp.text());
    }
}

// Register form handler
async function register(username, password, role) {
    const formData = new URLSearchParams();
    formData.append('username', username);
    formData.append('password', password);
    formData.append('role', role);

    const resp = await fetch(`${API}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData,
        credentials: 'same-origin'
    });
    if (resp.ok) {
        return await resp.text();
    } else {
        throw new Error(await resp.text());
    }
}

// Logout
async function logout() {
    const resp = await fetch(`${API}/logout`, {
        method: 'POST',
        credentials: 'same-origin'
    });
    if (resp.ok) {
        window.location.href = '/login.html';
    }
}