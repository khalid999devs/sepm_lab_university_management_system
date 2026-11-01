let csrf;

export class ApiError extends Error {
    constructor(status, body) {
        super(body.message || 'The request could not be completed.');
        this.status = status;
        this.code = body.code;
        this.fields = body.fields || {};
    }
}

export async function refreshCsrf() {
    csrf = await request('/api/auth/csrf');

    return csrf;
}

export async function request(path, options = {}) {
    const method = options.method || 'GET';
    const headers = new Headers(options.headers);
    headers.set('Accept', 'application/json');
    if (!['GET', 'HEAD'].includes(method)) {
        if (!csrf) {
            await refreshCsrf();
        }

        headers.set(csrf.headerName, csrf.token);
    }

    let response;
    try {
        response = await fetch(path, { ...options, method, headers, credentials: 'same-origin' });
    } catch {
        throw new Error('Cannot reach the server. Check your connection and try again.');
    }

    if (response.status === 204) {
        return null;
    }

    const body = await response.json();
    if (!response.ok) {
        if (body.code === 'CSRF_ERROR') {
            csrf = undefined;
        }

        throw new ApiError(response.status, body);
    }

    return body;
}

export function write(path, method, body) {
    return request(path, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
}

export function showNotice(element, message, kind = '') {
    element.textContent = message;
    element.className = `notice ${kind}`;
    element.hidden = !message;
}
