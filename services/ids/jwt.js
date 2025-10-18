import * as jose from 'jose';
import { randomUUID } from 'crypto';
import { sessionStore } from './session.js';
import { config } from './config.js';

export const { privateKey, publicKey, jwk, jwks } = await (async () => {
    const { publicKey: pbk, privateKey: pvk } = await jose.generateKeyPair('RS256', { modulusLength: 2048 });
    console.warn('[token] Using ephemeral dev RSA key pair. Configure APP_PRIVATE_KEY_PEM/APP_PUBLIC_KEY_PEM for production!');

    const jk = await jose.exportJWK(pbk);
    jk.kid = 'app-rs256-1';
    jk.alg = 'RS256';

    return {
        privateKey: pvk,
        publicKey: pbk,
        jwk: jk,
        jwks: {
            keys: [jk]
        }
    };
})();

/**
 * Generates a signed application token (JWT) for the user.
 * @param {Record<string, any>} userClaims 
 * @returns {Promise<{token: string, jti: string}>} The signed JWT token and its JTI.
 * @throws {Error} If the token generation fails.
 */
export async function generateToken(userClaims) {
    const jti = randomUUID();
    const ttl = `${config.tokenTtlMin}m`;

    const token = await new jose.SignJWT(userClaims)
        .setProtectedHeader({ alg: 'RS256', kid: jwk.kid, typ: 'JWT' })
        .setIssuer(config.issuer)
        .setAudience(config.audience)
        .setSubject(userClaims.sub)
        .setJti(jti)
        .setIssuedAt()
        .setExpirationTime(ttl)
        .sign(privateKey);

    // Link ke kc_sid untuk revoke via back-channel logout
    if (userClaims.kc_sid) {
        sessionStore.link(userClaims.kc_sid, jti);
    }

    return { token, jti };
}

/**
 * 
 * @param {string} token 
 * @returns 
 */
export async function verifyToken(token) {
    const { payload, protectedHeader } = await jose.jwtVerify(token, publicKey, {
        issuer: config.issuer,
        audience: config.audience,
    });
    if (payload.jti && sessionStore.isRevoked(payload.jti)) {
        throw new Error('Token revoked');
    }

    return { payload, protectedHeader };
}

export async function getClaim(token) {
    const payload = jose.decodeJwt(token);

    return { payload };
}

/**
 * 
 * @returns {Promise<string>} 
 */
export async function getPublicPEM() {
    return await jose.exportSPKI(publicKey);
}


/**
 * Middleware to protect API routes and ensure the user is authenticated.
 * 
 * @param {import('express').Request} req 
 * @param {import('express').Response} res 
 * @param {import('express').NextFunction} next 
 * @returns {Promise<void>}
 */
export async function authGuard(req, res, next) {
    try {
        const raw = (req.headers.authorization || '').replace(/^Bearer\s+/i, '') || req.cookies.app_token;
        if (!raw)
            return res.status(401).json({ error: 'No token' });

        const { payload } = await verifyToken(raw);
        req.user = payload;
        next();
    } catch (e) {
        res.status(401).json({ error: 'Unauthorized' });
    }
}

/**
 * 
 * @param {import('openid-client').IDToken} claim 
 */
export function buildClaim(claims) {
    const user = {};
    for (const key of Object.keys(claims)) {
        if (claims[key]) {
            user[key] = claims[key];
        }
    }

    return {
        ...user,
        sub: claims.sub,
        email: claims.email,
        name: claims.name || claims.preferred_username,
        roles: claims.realm_access?.roles || [],
        kc_iss: claims.iss,
        kc_sid: claims.sid
    };
}