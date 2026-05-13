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

// Global UI Restrictions
document.addEventListener('DOMContentLoaded', () => {
    document.body.style.userSelect = 'none';
    document.body.style.webkitUserSelect = 'none';

    document.querySelectorAll('input').forEach(el => {
        ['copy', 'paste', 'cut', 'dragstart', 'drop'].forEach(ev => {
            el.addEventListener(ev, e => e.preventDefault());
        });
    });
});


// --- Phase 3/4 Credentials Pipeline Context ---

document.addEventListener('DOMContentLoaded', () => {
    // Determine active role based on DOM markers (e.g. testing if the table exists)
    if (document.getElementById('student-docs-body')) loadStudentDocs();
    if (document.getElementById('registrar-uploads-body')) loadRegistrarDocs();
    if (document.getElementById('director-docs-body')) loadDirectorDocs();
});

// Student
async function requestDocument() {
    const type = document.getElementById('doc-request-type').value;
    const formData = new URLSearchParams();
    formData.append('type', type);
    
    try {
        const resp = await fetch('/api/docs/request', { method: 'POST', body: formData, credentials: 'same-origin' });
        if (resp.ok) { loadStudentDocs(); } 
        else { alert(await resp.text()); }
    } catch(e) { alert("Network Error"); }
}

async function loadStudentDocs() {
    const tbody = document.getElementById('student-docs-body');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="3">Loading...</td></tr>';
    const resp = await fetch('/api/docs/student');
    if (resp.ok) {
        const docs = await resp.json();
        tbody.innerHTML = docs.map(d => `
            <tr style="border-bottom: 1px solid var(--border);">
                <td style="padding: 10px; font-weight:600;">${d.type}</td>
                <td style="padding: 10px;">
                    <span class="role-badge" style="background: ${d.status === 'APPROVED' ? 'var(--success)' : 'var(--text-secondary)'}">${d.status}</span>
                </td>
                <td style="padding: 10px;">
                    ${d.status === 'APPROVED' ? `<a href="/api/docs/${d.id}/download" class="role-badge" style="background:var(--accent); text-decoration:none;">Download</a>` : '<span style="color:var(--text-secondary); font-size:0.8rem;">Processing...</span>'}
                </td>
            </tr>
        `).join('');
    }
}

// Registrar
async function loadRegistrarDocs() {
    const tbody = document.getElementById('registrar-uploads-body');
    if(!tbody) return;
    const resp = await fetch('/api/docs/registrar/pending');
    if (resp.ok) {
        const docs = await resp.json();
        tbody.innerHTML = docs.map(d => `
            <tr style="border-bottom: 1px solid var(--border);">
                <td style="padding: 10px; font-family:monospace;">${d.studentId}</td>
                <td style="padding: 10px; font-weight:600;">${d.type}</td>
                <td style="padding: 10px; display:flex; gap:10px;">
                    <input type="file" id="file-${d.id}" accept="application/pdf" style="padding:6px; background:var(--bg-primary);">
                    <button onclick="uploadPdf('${d.id}')" style="margin:0; padding:6px 12px; font-size:0.8rem;">Upload</button>
                </td>
            </tr>
        `).join('');
    }
}

async function uploadPdf(docId) {
    const fileInput = document.getElementById(`file-${docId}`);
    if (!fileInput.files.length) { alert("Please select a PDF."); return; }
    
    const formData = new FormData();
    formData.append("pdf", fileInput.files[0]);

    const btn = event.target;
    btn.textContent = "Uploading...";
    btn.disabled = true;

    try {
        const resp = await fetch(`/api/docs/${docId}/upload`, { method: 'POST', body: formData, credentials: 'same-origin' });
        if (resp.ok) { loadRegistrarDocs(); } 
        else { alert(await resp.text()); btn.textContent = "Upload"; btn.disabled = false; }
    } catch(e) { alert("Network Error"); btn.textContent = "Upload"; btn.disabled = false; }
}

// Director
async function loadDirectorDocs() {
    const tbody = document.getElementById('director-docs-body');
    if(!tbody) return;
    const resp = await fetch('/api/docs/director/pending');
    if (resp.ok) {
        const docs = await resp.json();
        tbody.innerHTML = docs.map(d => `
            <tr style="border-bottom: 1px solid var(--border);">
                <td style="padding: 10px; font-family:monospace;">${d.studentId}</td>
                <td style="padding: 10px; font-weight:600;">${d.type}</td>
                <td style="padding: 10px;">
                    <button onclick="approvePdf('${d.id}', this)" style="margin:0; padding:6px 12px; font-size:0.8rem; background:var(--success);">Stamp & Approve</button>
                </td>
            </tr>
        `).join('');
    }
}

async function approvePdf(docId, btn) {
    btn.textContent = "Stamping...";
    btn.disabled = true;
    try {
        const resp = await fetch(`/api/docs/${docId}/approve`, { method: 'POST', credentials: 'same-origin' });
        if (resp.ok) { loadDirectorDocs(); } 
        else { alert(await resp.text()); btn.textContent = "Stamp & Approve"; btn.disabled = false; }
    } catch(e) { alert("Network Error"); btn.textContent = "Stamp & Approve"; btn.disabled = false; }
}

// Dashboard Tabs Patch for new elements
const originalSwitchTab = window.switchTab;
window.switchTab = function(tabId) {
    if(originalSwitchTab) originalSwitchTab(tabId);
    
    // Handle the new tabs
    const uploads = document.getElementById('tab-uploads');
    const docs = document.getElementById('tab-docs');
    
    if(uploads) uploads.style.display = tabId === 'uploads' ? 'block' : 'none';
    if(docs) docs.style.display = tabId === 'docs' ? 'block' : 'none';

    if (tabId === 'uploads') loadRegistrarDocs();
    if (tabId === 'docs') loadDirectorDocs();
};

