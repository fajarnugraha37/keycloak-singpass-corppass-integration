/**
 * Sets a cookie in the response.
 * 
 * @param {import('express').Response} res - The response object.
 * @param {string} name - The name of the cookie.
 * @param {string} value - The value of the cookie.
 * @param {number} [minutes] - The expiration time in minutes.
 */
export function setCookie(res, name, value, minutes = null) {
    const opts = {
        httpOnly: true,
        sameSite: 'lax',
        secure: false, // set true di production (HTTPS)
        path: '/'
    };
    if (minutes) opts.maxAge = minutes * 60 * 1000;
    res.cookie(name, value, opts);
}

/**
 * Clears authentication-related cookies from the response.
 * 
 * @param {import('express').Response} res - The response object.
 */
export function clearAuthCookies(res) {
    for (const name of ['app_token', 'oidc_state', 'oidc_nonce', 'oidc_verifier', 'kc_rt', 'kc_id', 'kc_sid']) {
        res.clearCookie(name, { path: '/' });
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
