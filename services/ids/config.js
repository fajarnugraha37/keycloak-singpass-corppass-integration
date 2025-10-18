export const config = (() => {
    console.log('Loading IDS configuration from environment variables: ', process.env);
    return {
        host: process.env.HOST || 'http://localhost:8080',
        issuer: process.env.ISSUER_BASE || 'http://localhost:8080/ids',
        audience: process.env.AUDIENCE || 'cpds-api',
        scopes: (process.env.SCOPES || 'openid profile email cpds-api').split(' '),
        sessionKeys: (process.env.SESSION_KEYS || 'k1,k2').split(','),
        codeChallengeMethod: 'S256',
        tokenTtlMin: parseInt(process.env.APP_TOKEN_TTL_MIN || '30', 10),

        prefix: '/ids',
        loginPath: `/auth/login`,
        redirectPath: `/auth/callback`,
        refreshPath: `/auth/refresh`,
        logoutPath: `/auth/logout`,
        postLogoutPath: `/auth/post-logout`,
        backchannelLogoutPath: `/auth/backchannel-logout`,
        mePath: `/me`,
        jwksPath: `/.well-known/jwks.json`,

        kcDiscoveryUrl: `${process.env.KEYCLOAK_ISSUER || 'http://localhost:8081/auth/realms/agency-realm'}/.well-known/openid-configuration`,
        kcIssuer: process.env.KEYCLOAK_ISSUER || 'http://localhost:8081/auth/realms/agency-realm',
        kcAuth: process.env.KEYCLOAK_AUTH || 'http://keycloak:8080/auth/realms/agency-realm/protocol/openid-connect/auth',
        kcToken: process.env.KEYCLOAK_TOKEN || 'http://keycloak:8080/auth/realms/agency-realm/protocol/openid-connect/token',
        kcUserinfo: process.env.KEYCLOAK_USERINFO || 'http://keycloak:8080/auth/realms/agency-realm/protocol/openid-connect/userinfo',
        kcClientId: process.env.KEYCLOAK_CLIENT_ID || 'cpds-spa',
        kcClientSecret: process.env.KEYCLOAK_CLIENT_SECRET || 'cpds-oidc-secret',
    };
})();