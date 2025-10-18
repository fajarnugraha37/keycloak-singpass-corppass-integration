import express from 'express';
import cookieParser from 'cookie-parser';
import cors from 'cors';
import pino from 'pino';
import { createRemoteJWKSet, jwtVerify } from 'jose';

const log = pino({ level: process.env.LOG_LEVEL || 'info' });
const app = express();
app.use(cookieParser());
app.use(express.urlencoded({ extended: true }));
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3001;
const ISSUER = process.env.ISSUER; // http://localhost:8080/ids
const AUDIENCE = process.env.AUDIENCE || 'cpds-api';
const JWKS_URL = process.env.JWKS_URL; // http://ids:7000/ids/jwks

console.log(process.env);
const JWKS = createRemoteJWKSet(new URL(JWKS_URL), { 
  cacheMaxAge: 60000,
});

app.get('/health', (req, res) => res.json({ ok: true }));


/**
 * Middleware to protect API routes and ensure the user is authenticated.
 * 
 * @param {import('express').Request} req 
 * @param {import('express').Response} res 
 * @param {import('express').NextFunction} next 
 * @returns {Promise<void>}
 */
async function auth(req, res, next) {
    try {
        const token = (req.headers.authorization || '').replace(/^Bearer\s+/i, '') || req.cookies.app_token;
        if (!token)
            return res.status(401).json({ error: 'No token' });

        const { payload } = await jwtVerify(token, JWKS, { issuer: ISSUER, audience: AUDIENCE, algorithms: ['RS256'] });
        req.jwt = payload;
        next();
    } catch (e) {
      console.error('error: ', e);
      res.status(401).json({ error: 'invalid token' });
    }
}

function hasAdminRole(p) {
  const r = p.role;
  if (!r) return false;
  if (Array.isArray(r)) return r.includes('cpds-admin');
  return r === 'cpds-admin';
}

app.get('/cpds/api/hello', auth, (req, res) => {
  const p = req.jwt || {};
  res.json({
    message: 'Hello from CPDS API',
    sub: p.sub,
    preferred_username: p.preferred_username,
    email: p.email,
    iss: p.iss,
    aud: p.aud,
    role: p.role
  });
});

app.get('/admin', auth, (req, res) => {
  if (!hasAdminRole(req.jwt)) return res.status(403).json({ error: 'requires cpds-admin' });
  res.json({ ok: true, message: 'Admin access granted', sub: req.jwt.sub });
});

app.listen(PORT, () => log.info({ PORT }, 'CPDS API listening'));