import { request, refreshCsrf, showNotice } from './api.js';

const form = document.querySelector('#login-form');
const button = document.querySelector('#sign-in');
const status = document.querySelector('#login-status');
if (new URLSearchParams(location.search).has('expired')) {
    showNotice(status, 'Your session has ended. Please sign in again.');
}

form.addEventListener('submit', async event => {
    event.preventDefault();
    button.disabled = true;
    button.textContent = 'Signing in…';
    showNotice(status, '');
    try {
        await refreshCsrf();
        await request('/api/auth/login', { method: 'POST', body: new URLSearchParams(new FormData(form)) });
        location.replace('/management.html');
    } catch (error) {
        showNotice(status, error.message, 'error');
        button.disabled = false;
        button.textContent = 'Sign in';
    }
});

try {
    await request('/api/auth/me');
    location.replace('/management.html');
} catch (error) {
    if (error.status !== 401) showNotice(status, error.message, 'error');
}
