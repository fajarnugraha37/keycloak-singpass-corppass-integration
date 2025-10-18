export function safeParse(s) {
    try { return s == null ? null : JSON.parse(s); }
    catch { return null; }
}

export function read(key, fallback) {
    try {
        const raw = localStorage.getItem(key);
        return raw == null ? fallback : JSON.parse(raw);
    } catch { return fallback; }
}

export function throttle(fn, ms) {
    let id = 0, pending = false;
    return function () {
        if (pending) return;
        pending = true;
        clearTimeout(id);
        id = setTimeout(() => { pending = false; fn(); }, ms);
    };
}

export function load(key, fallback) {
    try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : structuredClone(fallback);
    } catch {
        return structuredClone(fallback);
    }
}

export function saveIdle(key, value) {
    const doSave = () => localStorage.setItem(key, JSON.stringify(value));
    (window.requestIdleCallback || setTimeout)(doSave, 0);
}

export function hideLoading() {
    const el = document.getElementById('loading-overlay');
    if (el) el.classList.add('hidden');
}

export function showLoading() {
    const el = document.getElementById('loading-overlay');
    if (el) el.classList.remove('hidden');
}

export async function callApi(path, token, method = "GET") {
    const res = await fetch(path, {
        method: method,
        headers: token ? { Authorization: `Bearer ${token}` } : {}
    });
    const text = await res.text();
    return { ok: res.ok, status: res.status, body: text };
}

export function log(el, msg) {
    const pre = document.getElementById(el);
    pre.textContent = (pre.textContent + "\n" + msg).trim();
}

export function b64decode(str) {
    try {
        return JSON.parse(atob(str.replace(/-/g, '+').replace(/_/g, '/')));
    } catch {
        return null;
    }
}


/**
 * Encode a string or object into a base64 string.
 * If an object is provided it will be JSON.stringified before encoding.
 *
 * @param {string|object} input
 * @returns {string} base64 encoded string
 */
export function base64Encode(input) {
    if (input === undefined || input === null) return '';
    const str = typeof input === 'string' ? input : JSON.stringify(input);
    return Buffer.from(str, 'utf8').toString('base64');
}

/**
 * Decode a base64 string into either a string or an object.
 * If the decoded payload is valid JSON, the parsed object is returned,
 * otherwise the raw UTF-8 string is returned.
 *
 * @param {string} b64
 * @returns {string|object}
 */
export function base64Decode(b64) {
    if (!b64) return '';
    const str = Buffer.from(b64, 'base64').toString('utf8');
    try {
        return JSON.parse(str);
    } catch (e) {
        return str;
    }
}
