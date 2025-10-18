import * as client from 'openid-client';
import { setTimeout } from 'timers/promises';
import { config } from './config.js';

/**
 * Initializes the OpenID Connect client configuration.
 * 
 * @param {string} serverUrl 
 * @param {string} clientId 
 * @param {string} clientSecret 
 * @returns {Promise<import('openid-client').Configuration>}
 */
export async function oidc(discoveryUrl = config.kcDiscoveryUrl, clientId = config.kcClientId, clientSecret = config.kcClientSecret) {
    console.log(`discoveryUrl=${discoveryUrl} clientId=${clientId} clientSecret=${clientSecret}`);
    while (true) {
        try {

            const config = await client.discovery(
                new URL(discoveryUrl), clientId,
                {
                    use_mtls_endpoint_aliases: false,
                },
                client.ClientSecretPost(clientSecret),
                {
                    execute: [client.allowInsecureRequests],
                });

            return config;
        } catch (e) {
            await setTimeout(1000);
            continue;
        }
    }
}

/**
 * Generates PKCE parameters for OIDC authentication.
 * 
 * @returns {Promise<{code_verifier: string, code_challenge: string, state: string, nonce: string}>}
 */
export async function pkce() {
    const code_verifier = client.randomPKCECodeVerifier();
    const code_challenge = await client.calculatePKCECodeChallenge(code_verifier);
    const state = client.randomState();
    const nonce = client.randomNonce();

    return { code_verifier, code_challenge, state, nonce };
}
