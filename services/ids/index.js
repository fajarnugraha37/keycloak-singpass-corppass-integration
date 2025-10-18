import express from 'express';
import cookieParser from 'cookie-parser';
import * as jose from 'jose';
import * as client from 'openid-client';
import cors from 'cors';
import { config } from './config.js';
import { oidc, pkce } from './oidc.js';
import { sessionStore } from './session.js';
import { setCookie, clearAuthCookies, base64Encode } from './util.js';
import { authGuard, jwks, generateToken, buildClaim } from './jwt.js';


const oidcConfig = await oidc();

const router = express.Router();

// ====== API PROTECTED ======
router.get(config.mePath, authGuard, (req, res) => res.json({ ...req.user }));

// ====== JWKS untuk verifikasi publik token App ======
router.get(config.jwksPath, (req, res) => res.json(jwks));

// 1) Start login
router.get(config.loginPath, async (req, res) => {
  const { code_verifier, code_challenge, state, nonce } = await pkce();
  const redirectUri = config.host + config.prefix + config.redirectPath;
  const codeChallengeMethod = config.codeChallengeMethod;
  console.log(`Start oidc login with redirect=${redirectUri} code_verifier=${code_verifier} code_challenge=${code_challenge} state=${state} nonce=${nonce}`);

  const url = client.buildAuthorizationUrl(oidcConfig, {
    scope: 'openid profile email',
    response_type: 'code',
    code_challenge,
    code_challenge_method: codeChallengeMethod,
    state, nonce,
    redirect_uri: redirectUri
  })

  setCookie(res, 'oidc_state', state);
  setCookie(res, 'oidc_nonce', nonce);
  setCookie(res, 'oidc_verifier', code_verifier);
  setCookie(res, 'oidc_challenge', code_challenge);
  if (req.query.redirect) {
    setCookie(res, 'oidc_redirect', base64Encode(req.query.redirect));
  }

  res.redirect(url);
});

// 2) Callback dari KC
router.get(config.redirectPath, async (req, res, next) => {
  try {
    const state = req.cookies.oidc_state;
    const nonce = req.cookies.oidc_nonce;
    const code_verifier = req.cookies.oidc_verifier;
    const { state: req_state, session_state, code } = req.query;
    if (state != req_state) {
      return res.status(400).json({ message: 'State mismatch' });
    }
    setCookie(res, 'oidc_session_state', session_state);
    setCookie(res, 'oidc_code', code);

    const currentUrl = new URL(config.host + config.prefix + req.url);
    const tokenSet = await client.authorizationCodeGrant(
      oidcConfig,
      currentUrl,
      {
        pkceCodeVerifier: code_verifier,
        expectedState: state,
        expectedNonce: nonce,
      },
    )
    console.log('Token Endpoint Response', Object.keys(tokenSet));

    // Simpan refresh token KC (untuk refresh di server)
    if (tokenSet.refresh_token)
      setCookie(res, 'kc_rt', tokenSet.refresh_token);
    if (tokenSet.id_token)
      setCookie(res, 'kc_id', tokenSet.id_token);

    const claims = tokenSet.claims();
    const user = buildClaim(claims);
    const { token } = await generateToken(user);
    setCookie(res, 'app_token', token, Number(config.tokenTtlMin));
    if (claims.sid) {
      setCookie(res, 'kc_sid', claims.sid);
    }

    if (req.cookies.oidc_redirect) {
      const redirect = base64Encode(req.cookies.oidc_redirect);
      res.redirect(config.host + `/cpds/#authenticated?redirect=${redirect}`);
    } else {
      res.redirect(config.host + `/cpds/#authenticated`);
    }
  } catch (err) {
    console.error('error: ', err);
    next(err);
  }
});

// 3) Refresh – gunakan KC refresh token untuk re-mint app token
router.post(config.refreshPath, async (req, res) => {
  try {
    const rt = req.cookies.kc_rt;
    if (!rt) return res.status(401).json({ error: 'No KC refresh token' });

    const refreshed = await client.refreshTokenGrant(oidcConfig, rt);
    if (refreshed.refresh_token && refreshed.refresh_token !== rt) {
      setCookie(res, 'kc_rt', refreshed.refresh_token);
    }

    const claims = refreshed.claims();
    const user = buildClaim(claims);

    const { token } = await generateToken(user);
    setCookie(res, 'app_token', token, Number(config.tokenTtlMin));
    res.json({ access_token: token, refresh_token: refreshed.refresh_token });
  } catch (e) {
    console.error(e);
    res.status(401).json({ error: 'Refresh failed' }); `1`
  }
});

// 4) Front-channel logout
router.get(config.logoutPath, (req, res) => {
  const postLogout = `${config.host}${config.prefix}${config.postLogoutPath}`;
  const idTokenHint = req.cookies.kc_id;

  const endSessionUrl = new URL(`${config.kcIssuer}/protocol/openid-connect/logout`);
  if (idTokenHint) {
    endSessionUrl.searchParams.set('id_token_hint', idTokenHint);
    endSessionUrl.searchParams.set('post_logout_redirect_uri', postLogout);
    res.redirect(endSessionUrl.toString());
  } else {
    res.redirect(postLogout);
  }
});

// 5) Setelah logout di KC, bersihkan sesi lokal
router.get(config.postLogoutPath, (req, res) => {
  clearAuthCookies(res);
  res.redirect('/cpds/?logged_out=1');
});

// 6) Back-channel logout (Keycloak → App). Set di KC Admin → Backchannel logout URL.
router.post(config.backchannelLogoutPath, async (req, res) => {
  try {
    const logoutToken = req.body.logout_token || req.body.token || req.body.id_token_hint;
    if (!logoutToken)
      return res.status(400).send('No logout_token');

    // Verifikasi JWT logout_token pakai JWKS Keycloak
    const kcJwksUri = `${config.kcIssuer}/protocol/openid-connect/certs`;
    const jwks = jose.createRemoteJWKSet(new URL(kcJwksUri));
    const { payload } = await jose.jwtVerify(logoutToken, jwks, { issuer: config.kcIssuer });

    // Validasi event backchannel logout
    const ev = payload.events || {};
    const isBack = !!ev['http://schemas.openid.net/event/backchannel-logout'];
    if (!isBack)
      return res.status(400).send('Not a backchannel logout token');

    const sid = payload.sid;
    if (sid)
      sessionStore.revokeBySid(sid);

    // 200 OK tanpa body
    res.status(200).end();
  } catch (e) {
    res.status(400).send('Invalid logout token');
  }
});


const app = express();
app.use(cookieParser());
app.use(cors());
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use((req, res, next) => {
  console.log(`===> ${req.method} ${req.path}: ${JSON.stringify(req.query)} - ${JSON.stringify(req.body || {})}\n`);
  next();
});
app.use(config.prefix, router);
app.use((req, res, next) => {
  res.status(404).send(`Resource ${req.method} ${req.url} is not exists`);
});
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).send('Internal Server Error');
});

const port = Number(process.env.PORT || 7000);
app.listen(port, () => {
  console.log(`App on ${config.host}:${port}`);
});