import express from 'express';
import cors from 'cors';
import pino from 'pino';
import { createRemoteJWKSet, jwtVerify } from 'jose';

const log = pino({ level: process.env.LOG_LEVEL || 'info' });
const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;
const ISSUER = process.env.ISSUER; // http://localhost:8081/realms/agency-realm
const AUDIENCE = process.env.AUDIENCE || 'aceas-api';
const JWKS_URL = process.env.JWKS_URL; // http://keycloak:8080/.../certs

console.log(process.env);
const JWKS = createRemoteJWKSet(new URL(JWKS_URL), { 
  cacheMaxAge: 60000,
});

app.get('/health', (req, res) => res.json({ ok: true }));

async function auth(req, res, next) {
  try {
    const h = req.headers.authorization || '';
    const [, token] = h.split(' ');
    if (!token) return res.status(401).json({ error: 'missing bearer token' });
    const { payload } = await jwtVerify(token, JWKS, { issuer: ISSUER, audience: AUDIENCE, algorithms: ['RS256'] });
    req.jwt = payload;
    next();
  } catch (e) {
    log.warn({ err: e }, 'jwt verification failed');
    res.status(401).json({ error: 'invalid token' });
  }
}

app.get('/aceas/api/hello', auth, (req, res) => {
  const p = req.jwt || {};
  res.json({
    message: 'Hello from ACEAS API',
    sub: p.sub,
    preferred_username: p.preferred_username,
    email: p.email,
    iss: p.iss,
    aud: p.aud
  });
});

app.listen(PORT, () => log.info({ PORT }, 'ACEAS API listening'));