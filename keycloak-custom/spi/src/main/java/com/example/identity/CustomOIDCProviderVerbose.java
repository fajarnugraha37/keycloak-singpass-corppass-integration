package com.example.identity;

import com.example.config.CustomOIDCIdentityProviderConfig;
import com.example.utils.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.*;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;

import static com.example.utils.JwtUtil.decryptAccessTokenResponse;

/**
 * Custom OIDC Provider implementation.
 * <p>
 * This class extends the Keycloak OIDCIdentityProvider to provide custom
 * handling of the OIDC authentication flow, token processing, and user
 * identity extraction for the specific requirements of the deploying
 * environment or application.
 *
 * <h3>OIDC Flow Execution Order:</h3>
 * <ol>
 *   <li>{@link #performLogin(AuthenticationRequest)} - Initial login redirect</li>
 *   <li>{@link #callback(RealmModel, AuthenticationCallback, EventBuilder)} - Creates endpoint to handle IdP response</li>
 *   <li>{@link CustomOIDCEndpoint#authResponse(String, String, String, String)} - Processes authorization code</li>
 *   <li>{@link CustomOIDCEndpoint#generateTokenRequest(String)} - Creates token exchange request</li>
 *   <li>{@link #authenticateTokenRequest(SimpleHttp)} - Adds client authentication</li>
 *   <li>{@link #getFederatedIdentity(String)} - Processes token response and creates identity</li>
 *   <li>{@link #extractIdentity(AccessTokenResponse, String, JsonWebToken)} - Maps tokens to user attributes</li>
 *   <li>{@link #preprocessFederatedIdentity(KeycloakSession, RealmModel, BrokeredIdentityContext)} - Pre-process identity</li>
 *   <li>{@link #importNewUser(KeycloakSession, RealmModel, UserModel, BrokeredIdentityContext)} OR {@link #updateBrokeredUser(KeycloakSession, RealmModel, UserModel, BrokeredIdentityContext)} - Create/update user</li>
 *   <li>{@link #authenticationFinished(AuthenticationSessionModel, BrokeredIdentityContext)} - Final cleanup</li>
 * </ol>
 *
 * <h3>Customization Points:</h3>
 * <ul>
 *   <li><strong>Token Processing:</strong> Override token validation, decryption, and parsing methods</li>
 *   <li><strong>User Mapping:</strong> Customize how claims are mapped to user attributes</li>
 *   <li><strong>Authentication:</strong> Modify client authentication methods (JWT, Basic, Form)</li>
 *   <li><strong>Flow Control:</strong> Add custom logic at various lifecycle points</li>
 * </ul>
 */
public class CustomOIDCProviderVerbose extends OIDCIdentityProvider {
    private static final Logger logger = Logger.getLogger(CustomOIDCProviderVerbose.class);

    public final CustomOIDCIdentityProviderConfig configuration;

    /**
     * Construct a new CustomOIDCProvider.
     * <p>
     * <strong>Purpose:</strong> Initialize the provider instance with the Keycloak session and
     * the custom OIDC identity provider configuration.
     * <p>
     * <strong>When invoked:</strong> Called by Keycloak during provider registration/initialization
     * (for example when Keycloak creates the identity provider instance at startup
     * or when the SPI is instantiated for a realm).
     * <p>
     * <strong>Execution Order:</strong> First method called during provider lifecycle
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Initialize custom configuration objects</li>
     *   <li>Set up logging or monitoring</li>
     *   <li>Validate configuration parameters</li>
     *   <li>Initialize external service connections</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Always call super(session, config) first</li>
     *   <li>Store any custom configuration for later use</li>
     *   <li>Avoid heavy initialization - prefer lazy loading</li>
     * </ul>
     *
     * @param session The Keycloak session providing access to realm, users, and other services
     * @param config  Custom configuration containing provider-specific settings
     */
    public CustomOIDCProviderVerbose(KeycloakSession session, CustomOIDCIdentityProviderConfig config) {
        super(session, config);
        this.configuration = config;
    }

    /**
     * Create the callback endpoint handler for this identity provider.
     * <p>
     * <strong>Purpose:</strong> Return an endpoint implementation that handles the OAuth/OIDC
     * callback (authorization code flow response) for this provider.
     * <p>
     * <strong>When invoked:</strong> Called by Keycloak when the broker callback URL is accessed
     * and Keycloak needs an endpoint instance to process the incoming request.
     * <p>
     * <strong>Execution Order:</strong> Called after user is redirected back from external IdP
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Return custom endpoint implementation</li>
     *   <li>Add request validation logic</li>
     *   <li>Implement custom error handling</li>
     *   <li>Add security checks or rate limiting</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Return an endpoint that extends OIDCEndpoint</li>
     *   <li>Pass all required parameters to the endpoint constructor</li>
     *   <li>Ensure endpoint handles both success and error cases</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return a valid endpoint object</li>
     *   <li>Endpoint must handle HTTP requests properly</li>
     * </ul>
     *
     * @param realm    The realm where the identity provider is configured
     * @param callback The authentication callback to notify of results
     * @param event    Event builder for auditing and logging
     * @return Custom endpoint instance to handle the callback
     */
    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new CustomOIDCEndpoint(callback, realm, event, this);
    }

    /**
     * Process the access token response after it is received and parsed.
     * <p>
     * <strong>Purpose:</strong> Hook to modify or react to the AccessTokenResponse and the
     * BrokeredIdentityContext after the token exchange.
     * <p>
     * <strong>When invoked:</strong> Called by the parent class after a successful token
     * exchange and before creating a federated identity.
     * <p>
     * <strong>Execution Order:</strong> Called after token exchange, before identity extraction
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Validate token response structure</li>
     *   <li>Add custom claims to the context</li>
     *   <li>Implement additional security checks</li>
     *   <li>Log token details for debugging</li>
     *   <li>Modify response before further processing</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.processAccessTokenResponse() unless completely overriding</li>
     *   <li>Validate that required tokens are present</li>
     *   <li>Handle error cases gracefully</li>
     * </ul>
     * <p>
     * <strong>Common use cases:</strong>
     * <ul>
     *   <li>Token validation and security checks</li>
     *   <li>Custom logging for compliance</li>
     *   <li>Adding context data for later processing</li>
     * </ul>
     *
     * @param context  The brokered identity context being built
     * @param response The access token response from the identity provider
     */
    @Override
    protected void processAccessTokenResponse(BrokeredIdentityContext context, AccessTokenResponse response) {
        var isJwt = JwtUtil.isJwt(response.getToken());
        logger.infof("Process Access Token is jwt = %s %s %s %s", isJwt, response.getToken(), response.getTokenType(), response.toString());
        if (isJwt && getConfig().isAccessTokenJwt()) {
            logger.infof("Access token is JWT, validating token");
            JsonWebToken access = validateToken(response.getToken(), true);
            context.getContextData().put(VALIDATED_ACCESS_TOKEN, access);
        } else {
            logger.infof("Access token is not JWT, skipping validation");
        }
    }

    /**
     * Convert a raw token response JSON into a BrokeredIdentityContext.
     * <p>
     * <strong>Purpose:</strong> Decrypt/parse the token response, validate tokens (access token,
     * id token), extract user identity and attributes and return a
     * BrokeredIdentityContext representing the federated user.
     * <p>
     * <strong>When invoked:</strong> Called by the identity broker flow when exchanging tokens
     * with the external identity provider and Keycloak needs to obtain the
     * federated identity for login/registration.
     * <p>
     * <strong>Execution Order:</strong> Core method called after token exchange, before user creation/update
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom token decryption logic</li>
     *   <li>Additional token validation rules</li>
     *   <li>Custom claim extraction and mapping</li>
     *   <li>Conditional user attribute setting</li>
     *   <li>Custom nonce validation</li>
     *   <li>Token storage logic</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Validate all tokens before processing</li>
     *   <li>Handle decryption errors gracefully</li>
     *   <li>Ensure required claims are present</li>
     *   <li>Set appropriate identity context data</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return a valid BrokeredIdentityContext</li>
     *   <li>Must handle token parsing errors</li>
     *   <li>Should validate token signatures if enabled</li>
     * </ul>
     * <p>
     * <strong>Security considerations:</strong>
     * <ul>
     *   <li>Always validate token signatures in production</li>
     *   <li>Check token expiration times</li>
     *   <li>Validate issuer and audience claims</li>
     *   <li>Handle nonce validation properly</li>
     * </ul>
     *
     * @param response The raw token response JSON from the identity provider
     * @return BrokeredIdentityContext containing the federated user identity
     * @throws IdentityBrokerException if token processing fails
     */
    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        logger.infof("Getting federated identity in CustomOIDCProvider: %s", response);

        var tokenResponse = decryptAccessTokenResponse(response, session, this.configuration.isEncryptedIdTokenFlag());
        var accessToken = JwtUtil.verifyAccessToken(tokenResponse);
        var encodedIdToken = tokenResponse.getIdToken();
        logger.infof("Access Token: %s; EncodedIdToken: %s;", accessToken, encodedIdToken);

        try {
            var idToken = this.validateToken(encodedIdToken);
            var identity = this.extractIdentity(tokenResponse, accessToken, idToken);
            var nonce = idToken.getOtherClaims().get("nonce");
            logger.infof("Extracted identity: %s with nonce: %s", identity.getUsername(), nonce);
            identity.getContextData().put("BROKER_NONCE", nonce);
            if (this.getConfig().isStoreToken()) {
                if (tokenResponse.getExpiresIn() > 0L) {
                    var accessTokenExpiration = (long) Time.currentTime() + tokenResponse.getExpiresIn();
                    tokenResponse.getOtherClaims().put("accessTokenExpiration", accessTokenExpiration);
                    response = JsonSerialization.writeValueAsString(tokenResponse);
                    logger.infof("Access token expiration set to: %s and response: %s", accessTokenExpiration, response);
                } else {
                    logger.infof("Token expiration not set in response.");
                }
                identity.setToken(response);
            } else {
                logger.infof("Not storing token as per configuration.");
            }
            return identity;
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not fetch attributes from userinfo endpoint.", e);
        }
    }

    /**
     * Validate a JSON Web Token (JWS).
     * <p>
     * <strong>Purpose:</strong> Verify the token signature and validate standard claims
     * such as expiration, issuer, and audience.
     * <p>
     * <strong>When invoked:</strong> Called when the provider needs to validate
     * an incoming JWS (for example ID token validation during token exchange).
     * <p>
     * <strong>Execution Order:</strong> Called during token validation within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom signature verification algorithms</li>
     *   <li>Key retrieval and caching strategies</li>
     *   <li>Certificate validation logic</li>
     *   <li>Algorithm-specific verification</li>
     *   <li>Error handling and fallback mechanisms</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Always verify signatures in production</li>
     *   <li>Handle different signature algorithms properly</li>
     *   <li>Validate key metadata (kid, alg)</li>
     *   <li>Return false for any verification failures</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return true only if signature is valid</li>
     *   <li>Should handle algorithm-specific verification</li>
     *   <li>Must not throw exceptions (return false instead)</li>
     * </ul>
     *
     * @param encodedToken   The encoded JWS string to validate
     * @param ignoreAudience Whether to skip audience claim validation
     * @return The validated JsonWebToken
     */
    @Override
    protected JsonWebToken validateToken(String encodedToken, boolean ignoreAudience) {
        logger.infof("Validating token in CustomOIDCProvider with ignoreAudience=%s: %s", ignoreAudience, encodedToken);
        JsonWebToken token;
        try {
            var parseToken = this.parseTokenInput(encodedToken, true);
            token = JsonSerialization.readValue(parseToken, JsonWebToken.class);
        } catch (IOException e) {
            throw new IdentityBrokerException("Invalid token", e);
        }

        var iss = token.getIssuer();
        var allowedTimeSkew = getConfig().getAllowedClockSkew();
        if (!token.isActive(allowedTimeSkew)) {
            throw new IdentityBrokerException("Token is no longer valid");
        }

        var clientId = getConfig().getClientId();
        var aud = token.getAudience();
        if (!ignoreAudience && !token.hasAudience(clientId)) {
            throw new IdentityBrokerException("Wrong audience from token.");
        }

        var issuerFor = token.getIssuedFor();
        if (!ignoreAudience && (issuerFor != null && !getConfig().getClientId().equals(issuerFor))) {
            throw new IdentityBrokerException("Token issued for does not match client id");
        }

        var trustedIssuers = getConfig().getIssuer();
        if (trustedIssuers != null && !trustedIssuers.isEmpty()) {
            var issuers = trustedIssuers.split(",");
            for (var trustedIssuer : issuers) {
                if (iss != null && iss.equals(trustedIssuer.trim())) {
                    return token;
                }
            }

            throw new IdentityBrokerException("Wrong issuer from token. Got: " + iss + " expected: " + getConfig().getIssuer());
        }

        return token;
    }

    @Override
    protected String parseTokenInput(String encodedToken, boolean shouldBeSigned) {
        if (encodedToken == null) {
            throw new IdentityBrokerException("No token from server.");
        }

        try {
            JWSInput jws;
            var joseToken = JOSEParser.parse(encodedToken);
            if (joseToken instanceof JWE) {
                var jwe = JWEObject.parse(encodedToken);
                var hdr = jwe.getHeader();
                var kid = hdr.getKeyID();
                var alg = String.valueOf(hdr.getAlgorithm()); // e.g. ECDH-ES+A256KW
                var enc = String.valueOf(hdr.getEncryptionMethod()); // e.g. A256CBC-HS512
                var cty = hdr.getContentType(); // often "JWT" for nested JWS
                logger.infof("JWE hdr: kid=%s alg=%s enc=%s cty=%s", kid, alg, enc, cty);

                KeyWrapper key;
                if (kid != null && !kid.isBlank()) {
                    logger.infof("No kid in header, looking for active decryption key");
                    key = session.keys()
                            .getKeysStream(session.getContext().getRealm())
                            .peek(kw -> logger.infof("key: kid=%s use=%s alg=%s status=%s type=%s",
                                    kw.getKid(),
                                    kw.getUse(),
                                    kw.getAlgorithm(),
                                    kw.getStatus(),
                                    kw.getPrivateKey() != null ? kw.getPrivateKey().getAlgorithm() : "null"))
                            .filter(kw -> kw.getStatus() == KeyStatus.ACTIVE)
                            .filter(kw -> kw.getUse() == KeyUse.ENC)
                            .filter(kw -> kid.equals(kw.getKid()))
                            .peek(kw -> logger.infof("Found key with kid: %s with use %s and algorithm %s", kw.getKid(), kw.getUse(), kw.getAlgorithm()))
                            .findFirst()
                            .orElse(null);
                } else {
                    logger.infof("Kid is exists, Looking for decryption key with kid: %s", kid);
                    key = session.keys()
                            .getKeysStream(session.getContext().getRealm())
                            .peek(kw -> logger.infof("key: kid=%s use=%s alg=%s status=%s type=%s",
                                    kw.getKid(),
                                    kw.getUse(),
                                    kw.getAlgorithm(),
                                    kw.getStatus(),
                                    kw.getPrivateKey() != null ? kw.getPrivateKey().getAlgorithm() : "null"))
                            .filter(kw -> kw.getStatus() == KeyStatus.ACTIVE)
                            .filter(kw -> kw.getUse() == KeyUse.ENC)
                            .filter(kw -> alg.equalsIgnoreCase(kw.getAlgorithm()))
                            .peek(kw -> logger.infof("Found key with kid: %s with use %s and algorithm %s", kw.getKid(), kw.getUse(), kw.getAlgorithm()))
                            .findFirst()
                            .orElse(null);
                }
                if (key == null || key.getPrivateKey() == null) {
                    throw new IdentityBrokerException("No ENC private key to decrypt JWE (alg=" + alg + ", kid=" + kid + ")");
                }
                if (alg.startsWith("ECDH-ES") && !(key.getPrivateKey() instanceof ECPrivateKey)) {
                    throw new IdentityBrokerException("ENC key is not EC private key required by " + alg + " (kid=" + key.getKid() + ") actually " + key.getPrivateKey().getClass().getName());
                }

                var decrypter = new ECDHDecrypter((ECPrivateKey) key.getPrivateKey());
                jwe.decrypt(decrypter);
                var content = jwe.getPayload().toString();

                try {
                    joseToken = JOSEParser.parse(content);
                } catch (Exception e) {
                    if (shouldBeSigned) {
                        throw new IdentityBrokerException("Token is not a signed JWS", e);
                    }
                    logger.infof("Decrypted content is not a JOSE token, returning raw content");
                    return content;
                }

                if (!(joseToken instanceof JWSInput)) {
                    throw new IdentityBrokerException("Invalid token type");
                }

                jws = (JWSInput) joseToken;
            } else if (joseToken instanceof JWSInput jwsInput) {
                logger.infof("Token is a JWS");
                jws = jwsInput;
            } else {
                throw new IdentityBrokerException("Invalid token type");
            }

            if (!verify(jws)) {
                throw new IdentityBrokerException("token signature validation failed");
            }

            return new String(jws.getContent(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IdentityBrokerException("Invalid token", e);
        }
    }

    /**
     * Validate a JSON Web Token (JWS).
     * <p>
     * <strong>Purpose:</strong> Verify the token signature and validate standard claims
     * such as expiration, issuer, and audience.
     * <p>
     * <strong>When invoked:</strong> Called when the provider needs to validate
     * an incoming JWS (for example ID token validation during token exchange).
     * <p>
     * <strong>Execution Order:</strong> Called during token validation within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom signature verification algorithms</li>
     *   <li>Key retrieval and caching strategies</li>
     *   <li>Certificate validation logic</li>
     *   <li>Algorithm-specific verification</li>
     *   <li>Error handling and fallback mechanisms</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Always verify signatures in production</li>
     *   <li>Handle different signature algorithms properly</li>
     *   <li>Validate key metadata (kid, alg)</li>
     *   <li>Return false for any verification failures</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return true only if signature is valid</li>
     *   <li>Should handle algorithm-specific verification</li>
     *   <li>Must not throw exceptions (return false instead)</li>
     * </ul>
     *
     * @param encodedToken The encoded JWS string to validate
     * @return The validated JsonWebToken
     */
    @Override
    public JsonWebToken validateToken(String encodedToken) {
        logger.infof("Validating token in CustomOIDCProvider: %s", encodedToken);
        try {
            var jwt = super.validateToken(encodedToken);
            return jwt;
        } catch (Exception e) {
            logger.errorf(e, "Token validation failed: %s", e.getMessage());
            throw e;
        }
    }

    /**
     * Get the user profile endpoint URL for validation.
     * <p>
     * <strong>Purpose:</strong> Return the URL of the user info endpoint
     * used to validate tokens and fetch user profile data.
     * <p>
     * <strong>When invoked:</strong> Called during token validation
     * when the provider needs to fetch user info from the IdP.
     * <p>
     * <strong>Execution Order:</strong> Called during token validation within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Return custom user info endpoint URL</li>
     *   <li>Add query parameters if needed</li>
     *   <li>Implement multi-endpoint logic</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Return a valid URL string</li>
     *   <li>Ensure endpoint is reachable</li>
     * </ul>
     *
     * @param event The event builder for logging/auditing
     * @return The user profile endpoint URL
     */
    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        var profileEndpoint = super.getProfileEndpointForValidation(event);
        logger.infof("Getting profile endpoint for validation in CustomOIDCProvider: %s", profileEndpoint);
        return profileEndpoint;
    }

    /**
     * Validate an external token through the user info endpoint.
     * <p>
     * <strong>Purpose:</strong> Validate the provided external token
     * by calling the user info endpoint and extracting user identity.
     * <p>
     * <strong>When invoked:</strong> Called during token exchange
     * when validating an external token via user info.
     * <p>
     * <strong>Execution Order:</strong> Called during token validation within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom user info request logic</li>
     *   <li>Additional validation checks</li>
     *   <li>Error handling and logging</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.validateExternalTokenThroughUserInfo()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param event            The event builder for logging/auditing
     * @param subjectToken     The external token to validate
     * @param subjectTokenType The type of the external token
     * @return BrokeredIdentityContext representing the validated user
     */
    @Override
    protected BrokeredIdentityContext validateExternalTokenThroughUserInfo(EventBuilder event, String subjectToken, String subjectTokenType) {
        logger.infof("Validating external token through user info in CustomOIDCProvider: %s, %s", subjectTokenType, subjectToken);
        return super.validateExternalTokenThroughUserInfo(event, subjectToken, subjectTokenType);
    }

    /**
     * Exchange external user info for validation only.
     * <p>
     * <strong>Purpose:</strong> Exchange the provided parameters
     * to validate the external user info without creating a full identity.
     * <p>
     * <strong>When invoked:</strong> Called during token exchange
     * when only validation of external user info is needed.
     * <p>
     * <strong>Execution Order:</strong> Called during token validation within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom parameter handling</li>
     *   <li>Additional validation logic</li>
     *   <li>Error handling and logging</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.exchangeExternalUserInfoValidationOnly()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param event  The event builder for logging/auditing
     * @param params The parameters for the exchange
     * @return BrokeredIdentityContext representing the validated user
     */
    @Override
    protected BrokeredIdentityContext exchangeExternalUserInfoValidationOnly(EventBuilder event, MultivaluedMap<String, String> params) {
        logger.infof("Exchanging external user info validation only in CustomOIDCProvider with params: %s", params);
        return super.exchangeExternalUserInfoValidationOnly(event, params);
    }

    /**
     * Extract identity information from the user profile JSON.
     * <p>
     * <strong>Purpose:</strong> Map user profile claims from the user info
     * response into the BrokeredIdentityContext.
     * <p>
     * <strong>When invoked:</strong> Called after fetching user info
     * to extract and map user attributes.
     * <p>
     * <strong>Execution Order:</strong> Called during identity extraction within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom claim-to-attribute mapping</li>
     *   <li>Conditional attribute setting</li>
     *   <li>Claim transformation logic</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.extractIdentityFromProfile()</li>
     *   <li>Handle missing/null claims gracefully</li>
     * </ul>
     *
     * @param event    The event builder for logging/auditing
     * @param userInfo The JSON node containing user profile claims
     * @return BrokeredIdentityContext with mapped user attributes
     */
    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode userInfo) {
        logger.infof("Extracting identity from profile in CustomOIDCProvider: %s", userInfo);
        return super.extractIdentityFromProfile(event, userInfo);
    }

    /**
     * Exchange an error response during token exchange.
     * <p>
     * <strong>Purpose:</strong> Handle error responses received
     * during token exchange and return appropriate HTTP responses.
     * <p>
     * <strong>When invoked:</strong> Called when an error occurs
     * during the token exchange process.
     * <p>
     * <strong>Execution Order:</strong> Called during token exchange error handling
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom error response formatting</li>
     *   <li>Logging of error details</li>
     *   <li>Additional error handling logic</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.exchangeErrorResponse()</li>
     *   <li>Log error details for debugging</li>
     * </ul>
     *
     * @param uriInfo          The URI info of the request
     * @param authorizedClient The authorized client model
     * @param tokenUserSession The user session model for the token
     * @param errorCode        The error code received
     * @param reason           The reason for the error
     * @return HTTP Response representing the error
     */
    @Override
    protected Response exchangeErrorResponse(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, String errorCode, String reason) {
        logger.infof("Exchanging error response in CustomOIDCProvider: %s, %s", errorCode, reason);
        return super.exchangeErrorResponse(uriInfo, authorizedClient, tokenUserSession, errorCode, reason);
    }

    /**
     * Get the username from the user info JSON.
     * <p>
     * <strong>Purpose:</strong> Extract the username claim from the user info
     * response for use in the BrokeredIdentityContext.
     * <p>
     * <strong>When invoked:</strong> Called during identity extraction
     * to obtain the username from user info.
     * <p>
     * <strong>Execution Order:</strong> Called during identity extraction within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom username extraction logic</li>
     *   <li>Fallback mechanisms if username claim is missing</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.getUsernameFromUserInfo()</li>
     *   <li>Handle missing/null username gracefully</li>
     * </ul>
     *
     * @param userInfo The JSON node containing user profile claims
     * @return The extracted username
     */
    @Override
    protected String getUsernameFromUserInfo(JsonNode userInfo) {
        logger.infof("Getting username from user info in CustomOIDCProvider: %s", userInfo.asText());
        var usernameNode = userInfo.path("sub");
        if (usernameNode.isMissingNode() || usernameNode.isNull()) {
            usernameNode = userInfo.path("uinfin").path("value");
        }

        return usernameNode.asText();
    }

    /**
     * Exchange a stored token during token exchange.
     * <p>
     * <strong>Purpose:</strong> Handle the exchange of a stored token
     * for validation and user identity extraction.
     * <p>
     * <strong>When invoked:</strong> Called when exchanging a stored token
     * during the token exchange process.
     * <p>
     * <strong>Execution Order:</strong> Called during token exchange within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom token exchange logic</li>
     *   <li>Additional validation checks</li>
     *   <li>Error handling and logging</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.exchangeStoredToken()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param uriInfo          The URI info of the request
     * @param event            The event builder for logging/auditing
     * @param authorizedClient The authorized client model
     * @param tokenUserSession The user session model for the token
     * @param tokenSubject     The user model for the token subject
     * @return HTTP Response representing the exchanged token
     */
    @Override
    protected Response exchangeStoredToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        logger.infof("Exchanging stored token in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        return super.exchangeStoredToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject);
    }

    /**
     * Update an existing brokered user in Keycloak.
     * <p>
     * <strong>Purpose:</strong> Update the Keycloak user based on the
     * provided BrokeredIdentityContext during the identity broker flow.
     * <p>
     * <strong>When invoked:</strong> Called when an existing federated user
     * is being updated in Keycloak.
     * <p>
     * <strong>Execution Order:</strong> Called during user update within the broker flow
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom user attribute mapping</li>
     *   <li>Additional user update logic</li>
     *   <li>Error handling and logging</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.updateBrokeredUser()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param session The Keycloak session
     * @param realm   The realm where the user is being updated
     * @param user    The user model to be updated
     * @param context The brokered identity context
     */
    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        logger.infof("Updating brokered user in CustomOIDCProvider: %s", user.getUsername());
        super.updateBrokeredUser(session, realm, user, context);
    }

    /**
     * Import a new user into Keycloak.
     * <p>
     * <strong>Purpose:</strong> Create a new Keycloak user based on the
     * provided BrokeredIdentityContext during the identity broker flow.
     * <p>
     * <strong>When invoked:</strong> Called when a new federated user
     * is being imported into Keycloak.
     * <p>
     * <strong>Execution Order:</strong> Called during user import within the broker flow
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom user attribute mapping</li>
     *   <li>Additional user setup logic</li>
     *   <li>Error handling and logging</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.importNewUser()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param session The Keycloak session
     * @param realm   The realm where the user is being imported
     * @param user    The user model to be created
     * @param context The brokered identity context
     */
    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        logger.infof("Importing new user in CustomOIDCProvider: %s", user.getUsername());
        super.importNewUser(session, realm, user, context);
    }

    /**
     * Complete the exchange of an external token.
     * <p>
     * <strong>Purpose:</strong> Finalize the exchange of an external token
     * and perform any necessary post-exchange processing.
     * <p>
     * <strong>When invoked:</strong> Called after successfully exchanging
     * an external token during the token exchange process.
     * <p>
     * <strong>Execution Order:</strong> Called after token exchange within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom post-exchange processing</li>
     *   <li>Logging of exchange details</li>
     *   <li>Error handling and validation</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.exchangeExternalComplete()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param userSession The user session model
     * @param context     The brokered identity context
     * @param params      The parameters for the exchange
     */
    @Override
    public void exchangeExternalComplete(UserSessionModel userSession, BrokeredIdentityContext context, MultivaluedMap<String, String> params) {
        logger.infof("Exchange external complete in CustomOIDCProvider for user session: %s", userSession.getId());
        params.forEach((key, value) -> logger.infof("Param: %s = %s", key, value));
        super.exchangeExternalComplete(userSession, context, params);
    }

    /**
     * Retrieve the token for the given federated identity.
     * <p>
     * <strong>Purpose:</strong> Fetch the access token associated with
     * the provided federated identity model.
     * <p>
     * <strong>When invoked:</strong> Called when the provider needs to
     * retrieve the token for a federated identity.
     * <p>
     * <strong>Execution Order:</strong> Called during token retrieval within the broker flow
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom token retrieval logic</li>
     *   <li>Error handling and logging</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.retrieveToken()</li>
     *   <li>Handle any exceptions gracefully</li>
     * </ul>
     *
     * @param session  The Keycloak session
     * @param identity The federated identity model
     * @return HTTP Response containing the retrieved token
     */
    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        logger.infof("Retrieving token in CustomOIDCProvider for identity: %s", identity.getUserId());
        return super.retrieveToken(session, identity);
    }

    /**
     * Map tokens into a BrokeredIdentityContext (user attributes, username, id).
     * <p>
     * <strong>Purpose:</strong> Called to extract and map claims from the idToken and userinfo
     * (via the AccessTokenResponse) into the BrokeredIdentityContext which is
     * later used to link or create a Keycloak user.
     * <p>
     * <strong>When invoked:</strong> Called from getFederatedIdentity (or parent flow) after
     * tokens are validated.
     * <p>
     * <strong>Execution Order:</strong> Called during getFederatedIdentity processing
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom claim-to-attribute mapping logic</li>
     *   <li>Conditional attribute setting based on claim values</li>
     *   <li>Claim transformation (e.g., format conversion)</li>
     *   <li>Multi-value claim handling</li>
     *   <li>Custom username generation logic</li>
     *   <li>Role or group mapping from claims</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.extractIdentity() first to get base mapping</li>
     *   <li>Handle null/missing claims gracefully</li>
     *   <li>Validate extracted data before setting</li>
     *   <li>Log important claim mappings for debugging</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return a valid BrokeredIdentityContext</li>
     *   <li>Should set username and user ID properly</li>
     *   <li>Must handle IOException from userinfo requests</li>
     * </ul>
     * <p>
     * <strong>Common patterns:</strong>
     * <ul>
     *   <li>Map standard OIDC claims (sub, name, email, etc.)</li>
     *   <li>Transform claim formats (dates, phone numbers)</li>
     *   <li>Set user attributes for later mapper processing</li>
     *   <li>Extract roles from custom claims</li>
     * </ul>
     *
     * @param tokenResponse The complete token response from identity provider
     * @param accessToken   The access token string (may be used for userinfo)
     * @param idToken       The parsed and validated ID token
     * @return BrokeredIdentityContext with mapped user attributes
     * @throws IOException if userinfo endpoint request fails
     */
    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        logger.infof("Extracting identity in CustomOIDCProvider");

        var identityContext = super.extractIdentity(tokenResponse, accessToken, idToken);
        identityContext
                .getContextData()
                .forEach((key, value) -> logger.infof("Context data: %s = %s", key, value));
        var sub = idToken.getSubject();
        var identityUsername = identityContext.getUsername();
        logger.infof("Initial extracted identity: %s with subject: %s", identityUsername, sub);

        identityContext.setId(idToken.getSubject());
        identityContext.setUsername(identityUsername);
        idToken.getOtherClaims().forEach((key, value) -> {
            logger.infof("Mapping claim to attribute: %s = %s", key, value);
            identityContext.setUserAttribute(key, value != null ? value.toString() : null);
        });

        return identityContext;
    }

    /**
     * Prepare authentication for token endpoint (client authentication) before sending the token request.
     * <p>
     * <strong>Purpose:</strong> Modify the token endpoint request to include client authentication
     * using either JWT client assertion or basic/form credentials based on config.
     * <p>
     * <strong>When invoked:</strong> Called by the token exchange flow just before the request
     * to the identity provider's token endpoint is executed.
     * <p>
     * <strong>Execution Order:</strong> Called during token exchange, before sending HTTP request
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>JWT client assertion creation and signing</li>
     *   <li>Custom client authentication methods</li>
     *   <li>Additional request parameters</li>
     *   <li>Custom key selection for signing</li>
     *   <li>Vault integration for secrets</li>
     *   <li>Request headers and authentication schemes</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Follow OAuth 2.0 client authentication standards</li>
     *   <li>Handle key retrieval errors gracefully</li>
     *   <li>Validate configuration before proceeding</li>
     *   <li>Use secure credential storage (vault)</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return a properly authenticated SimpleHttp request</li>
     *   <li>Should handle different auth methods (JWT, Basic, Form)</li>
     *   <li>Must include required OAuth 2.0 parameters</li>
     * </ul>
     * <p>
     * <strong>Authentication methods supported:</strong>
     * <ul>
     *   <li><strong>JWT:</strong> client_assertion with signed JWT</li>
     *   <li><strong>Basic:</strong> HTTP Basic authentication</li>
     *   <li><strong>Form:</strong> client_id and client_secret in form body</li>
     * </ul>
     * <p>
     * <strong>Security considerations:</strong>
     * <ul>
     *   <li>Never log client secrets or JWTs</li>
     *   <li>Use appropriate key algorithms</li>
     *   <li>Validate key availability before use</li>
     *   <li>Handle key rotation scenarios</li>
     * </ul>
     *
     * @param tokenRequest The HTTP request builder to be authenticated
     * @return The authenticated HTTP request ready to send
     */
    @Override
    public SimpleHttp authenticateTokenRequest(SimpleHttp tokenRequest) {
        logger.infof("Authenticating token request in CustomOIDCProvider");
        if (getConfig().isJWTAuthentication()) {
            String jws = null;
            if (configuration.getSigningKeyId() == null || configuration.getSigningKeyId().isEmpty()) {
                jws = new JWSBuilder().type(OAuth2Constants.JWT).jsonContent(generateToken()).sign(getSignatureContext());
            } else {
                var keyWrapper = session.keys().getKeysStream(session.getContext().getRealm()).filter(key -> key.getKid().equalsIgnoreCase(this.configuration.getSigningKeyId())).findFirst();
                if (keyWrapper.isPresent()) {
                    var key = keyWrapper.get();
                    var algorithm = JWSAlgorithm.parse(key.getAlgorithm());
                    logger.infof("Signing key id:", this.configuration.getSigningKeyId());
                    logger.infof("Found key for signing the request: %s", key.getAlgorithm());
                    logger.infof("Using key with KID: %s and algorithm: %s", key.getKid(), algorithm);
                    jws = JwtUtil.createBearer(configuration, key, algorithm);
                } else {
                    logger.errorf("Unable to find matching key for signing the request %s", this.configuration.getSigningKeyId());
                }
            }
            logger.infof("Client ID: %s", getConfig().getClientId());
            logger.infof("Client Assertion Type: %s", OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
            logger.infof("JWS: %s", jws);
            return tokenRequest.param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId()).param(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT).param(OAuth2Constants.CLIENT_ASSERTION, jws);
        }

        try (var vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            if (getConfig().isBasicAuthentication()) {
                logger.infof("Using basic authentication for client ID: %s", getConfig().getClientId());
                logger.infof("Client Secret from Vault: %s", vaultStringSecret.get().orElse("Not found in vault"));
                return tokenRequest.authBasic(getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()));
            }

            logger.infof("Using form authentication for client ID: %s", getConfig().getClientId());
            logger.infof("Client Secret from Vault: %s", vaultStringSecret.get().orElse("Not found in vault"));
            return tokenRequest.param(OAUTH2_PARAMETER_CLIENT_ID,
                    getConfig().getClientId()).param(OAUTH2_PARAMETER_CLIENT_SECRET,
                    vaultStringSecret.get().orElse(getConfig().getClientSecret()));
        }
    }

    /**
     * Hook to update or alter the federated identity context before it is linked or created.
     * <p>
     * <strong>Purpose:</strong> Allow modifications to BrokeredIdentityContext (attributes, mapper decisions)
     * before the user is linked or created in Keycloak.
     * <p>
     * <strong>When invoked:</strong> Called by the broker flow prior to linking/creating the Keycloak user.
     * <p>
     * <strong>Execution Order:</strong> Called after identity extraction, before user operations
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Final attribute validation and transformation</li>
     *   <li>Business rule application</li>
     *   <li>User existence checks and decisions</li>
     *   <li>Context data addition for mappers</li>
     *   <li>Conditional identity modifications</li>
     *   <li>Integration with external systems</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.preprocessFederatedIdentity() unless completely overriding</li>
     *   <li>Validate all required attributes are present</li>
     *   <li>Apply business-specific transformation rules</li>
     *   <li>Handle edge cases and data quality issues</li>
     * </ul>
     * <p>
     * <strong>Common use cases:</strong>
     * <ul>
     *   <li>Apply organizational business rules</li>
     *   <li>Validate user eligibility</li>
     *   <li>Enrich context with external data</li>
     *   <li>Set conditional attributes based on claims</li>
     * </ul>
     * <p>
     * <strong>What not to do:</strong>
     * <ul>
     *   <li>Don't create or update users here (use importNewUser/updateBrokeredUser)</li>
     *   <li>Don't perform heavy external API calls</li>
     *   <li>Don't throw exceptions for data validation (use context flags instead)</li>
     * </ul>
     *
     * @param session The Keycloak session for accessing services
     * @param realm   The realm where the user will be created/linked
     * @param context The federated identity context to be processed
     */
    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        logger.infof("Preprocessing federated identity in CustomOIDCProvider: %s", context.getUsername());
        super.preprocessFederatedIdentity(session, realm, context);
    }

    /**
     * Verify a JWS input (ID token or other signed JWT).
     * <p>
     * <strong>Purpose:</strong> Validate the token signature using ECDSA or RSA depending on
     * the token algorithm and available keys.
     * <p>
     * <strong>When invoked:</strong> Called when the provider needs to validate the signature
     * on an incoming JWS (for example ID token validation during token exchange).
     * <p>
     * <strong>Execution Order:</strong> Called during token validation within getFederatedIdentity
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Custom signature verification algorithms</li>
     *   <li>Key retrieval and caching strategies</li>
     *   <li>Certificate validation logic</li>
     *   <li>Algorithm-specific verification</li>
     *   <li>Error handling and fallback mechanisms</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Always verify signatures in production</li>
     *   <li>Handle different signature algorithms properly</li>
     *   <li>Validate key metadata (kid, alg)</li>
     *   <li>Return false for any verification failures</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return true only if signature is valid</li>
     *   <li>Should handle algorithm-specific verification</li>
     *   <li>Must not throw exceptions (return false instead)</li>
     * </ul>
     * <p>
     * <strong>Supported algorithms:</strong>
     * <ul>
     *   <li><strong>ECDSA:</strong> ES256, ES384, ES512</li>
     *   <li><strong>RSA:</strong> RS256, RS384, RS512, PS256, PS384, PS512</li>
     * </ul>
     * <p>
     * <strong>Security considerations:</strong>
     * <ul>
     *   <li>Never skip signature validation in production</li>
     *   <li>Validate key ID matches expected values</li>
     *   <li>Handle key rotation scenarios</li>
     *   <li>Log verification failures for security monitoring</li>
     * </ul>
     *
     * @param jws The JSON Web Signature to verify
     * @return true if signature is valid, false otherwise
     */
    @Override
    protected boolean verify(JWSInput jws) {
        var hdr = jws.getHeader();
        logger.infof("Verifying JWS with Algorithm: %s, Key ID: %s", hdr.getAlgorithm().name(), hdr.getKeyId());
        if (!getConfig().isValidateSignature()) {
            logger.warnf("Skipping signature validation as per configuration.");
            return true;
        }
        if (!getConfig().isUseJwksUrl()) {
            logger.infof("Using default signature verification in superclass.");
            return super.verify(jws);
        }

        try {
            var jwksUrl = getConfig().getJwksUrl();
            var jwksJson = SimpleHttp.doGet(jwksUrl, session).asJson();
            var jwkSet = JWKSet.parse(jwksJson.toString());
            logger.infof("Fetched JWKS from URL: %s with result %s", jwksUrl, jwksJson.toString());

            var kid = hdr.getKeyId();
            var jwk = jwkSet.getKeyByKeyId(kid);
            if (jwk == null) {
                logger.errorf("No matching key found in JWKS for Key ID: %s", kid);
                return false;
            }

            var compact = jws.getWireString();
            var sjwt = SignedJWT.parse(compact);
            if (!hdr.getAlgorithm().name().equalsIgnoreCase(sjwt.getHeader().getAlgorithm().getName())) {
                logger.errorf("JWS alg header %s does not match SignedJWT alg %s",
                        hdr.getAlgorithm().name(),
                        sjwt.getHeader().getAlgorithm().getName());
                return false;
            }
            if (jwk.getKeyUse() != null && !"sig".equalsIgnoreCase(jwk.getKeyUse().identifier())) {
                logger.errorf("JWK key use is not 'sig' for Key ID: %s", kid);
                return false;
            }

            var ok = false;
            if ("EC".equals(jwk.getKeyType().getValue())) {
                var ecJwk = jwk.toECKey();
                ok = sjwt.verify(new ECDSAVerifier(ecJwk));
            } else if ("RSA".equals(jwk.getKeyType().getValue())) {
                ok = sjwt.verify(new RSASSAVerifier(jwk.toRSAKey()));
            } else {
                logger.errorf("unsupported kty=%s", jwk.getKeyType());
                return false;
            }

            if (!ok) {
                logger.error("signature verification failed");
                return false;
            } else {
                logger.info("signature successfully verified");
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to verify token", e);
            return false;
        }
    }

    /**
     * Build the authorization URL for redirecting the user to the external IdP.
     * <p>
     * <strong>Purpose:</strong> Provide a hook to customize the authorization URL construction
     * (for example to add extra parameters) prior to redirecting the user.
     * <p>
     * <strong>When invoked:</strong> Called during the authentication flow when redirecting the
     * user to the identity provider's authorization endpoint.
     * <p>
     * <strong>Execution Order:</strong> Called during performLogin, before user redirect
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Additional query parameters (prompt, max_age, etc.)</li>
     *   <li>Custom scope values</li>
     *   <li>State parameter modifications</li>
     *   <li>PKCE parameters</li>
     *   <li>Custom claims requests</li>
     *   <li>Locale or UI hints</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.createAuthorizationUrl() first</li>
     *   <li>Add parameters using UriBuilder methods</li>
     *   <li>Validate parameter values before adding</li>
     *   <li>Handle encoding properly for special characters</li>
     * </ul>
     * <p>
     * <strong>Common customizations:</strong>
     * <ul>
     *   <li>Add prompt=login for forced re-authentication</li>
     *   <li>Set max_age for session timeout control</li>
     *   <li>Add custom scopes for additional claims</li>
     *   <li>Set ui_locales for language preferences</li>
     * </ul>
     *
     * @param request The authentication request containing redirect info
     * @return UriBuilder for the authorization URL
     */
    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        logger.infof("Creating authorization URL in CustomOIDCProvider: %s", request.getRedirectUri());
        return super.createAuthorizationUrl(request);
    }

    /**
     * Build the userinfo HTTP request used to retrieve user attributes from the IdP.
     * <p>
     * <strong>Purpose:</strong> Allow customization of the userinfo request (headers, token form)
     * before fetching additional user information.
     * <p>
     * <strong>When invoked:</strong> Called after token exchange when the provider needs to fetch
     * additional user information from the userinfo endpoint.
     * <p>
     * <strong>Execution Order:</strong> Called during extractIdentity if userinfo is needed
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Authorization header format</li>
     *   <li>Additional request headers</li>
     *   <li>Request timeout settings</li>
     *   <li>HTTP method (GET vs POST)</li>
     *   <li>Request body parameters</li>
     *   <li>SSL/TLS configuration</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Call super.buildUserInfoRequest() for base setup</li>
     *   <li>Ensure proper authorization header</li>
     *   <li>Handle token format requirements</li>
     *   <li>Set appropriate timeouts</li>
     * </ul>
     * <p>
     * <strong>Common patterns:</strong>
     * <ul>
     *   <li>Add Bearer token to Authorization header</li>
     *   <li>Set Accept header for JSON response</li>
     *   <li>Add User-Agent for identification</li>
     *   <li>Configure timeout for reliability</li>
     * </ul>
     *
     * @param subjectToken The access token to use for authorization
     * @param userInfoUrl  The userinfo endpoint URL
     * @return Configured HTTP request for userinfo
     */
    @Override
    protected SimpleHttp buildUserInfoRequest(String subjectToken, String userInfoUrl) {
        logger.infof("Building user info request in CustomOIDCProvider: %s %s", userInfoUrl, subjectToken);
        return super.buildUserInfoRequest(subjectToken, userInfoUrl);
    }

    /**
     * Obtain federated identity using an access token.
     * <p>
     * Purpose: Provide a hook to customize how the federated identity is
     * retrieved given an access token.
     * When invoked: Called by flows that already have an access token and need
     * to fetch or refresh the BrokeredIdentityContext (e.g., token refresh or
     * account linking flows).
     */
    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        logger.infof("Getting federated identity in CustomOIDCProvider: %s", accessToken);
        return super.doGetFederatedIdentity(accessToken);
    }

    /**
     * Extract a named token value from a JSON response string.
     * <p>
     * Purpose: Helper hook used when parsing the access token response payload.
     * When invoked: Called during token response processing to obtain values like
     * access_token, id_token, refresh_token from the raw response.
     */
    @Override
    protected String extractTokenFromResponse(String response, String tokenName) {
        logger.infof("Extracting token from response in CustomOIDCProvider: %s %s", tokenName, response);
        return super.extractTokenFromResponse(response, tokenName);
    }

    /**
     * Called when brokered authentication is finished.
     * <p>
     * Purpose: Final hook after federated authentication completes. Can be used
     * to perform cleanup or custom logic after the user is authenticated.
     * When invoked: Invoked by the broker flow after the federated login is
     * completed and before the final redirect back to the application.
     */
    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        logger.infof("Authentication finished in CustomOIDCProvider: %s %s", authSession.getTabId(), context.getUsername());
        super.authenticationFinished(authSession, context);
    }

    /**
     * Perform the login redirect to the external identity provider.
     * <p>
     * <strong>Purpose:</strong> Hook to customize the login redirect process (for example
     * logging or modifying the request) before redirecting users to external IdP.
     * <p>
     * <strong>When invoked:</strong> Called by Keycloak when initiating the login flow and
     * redirecting the user to the identity provider's authorization endpoint.
     * <p>
     * <strong>Execution Order:</strong> First method in user-facing authentication flow
     * <p>
     * <strong>What you can customize:</strong>
     * <ul>
     *   <li>Pre-redirect validation and logging</li>
     *   <li>Custom redirect URI parameters</li>
     *   <li>State parameter modifications</li>
     *   <li>Session data preparation</li>
     *   <li>Error handling for invalid requests</li>
     *   <li>Custom response formatting</li>
     * </ul>
     * <p>
     * <strong>What you should do:</strong>
     * <ul>
     *   <li>Validate the authentication request</li>
     *   <li>Log the login attempt</li>
     *   <li>Call super.performLogin() for standard flow</li>
     *   <li>Handle error cases appropriately</li>
     * </ul>
     * <p>
     * <strong>What you need to do:</strong>
     * <ul>
     *   <li>Must return a valid HTTP Response</li>
     *   <li>Should redirect user to external IdP</li>
     *   <li>Must handle request validation failures</li>
     * </ul>
     * <p>
     * <strong>Response types:</strong>
     * <ul>
     *   <li><strong>Redirect:</strong> HTTP 302 to external IdP</li>
     *   <li><strong>Error:</strong> Error page for invalid requests</li>
     *   <li><strong>Form:</strong> Auto-submit form (rare)</li>
     * </ul>
     *
     * @param request The authentication request from the client
     * @return HTTP Response redirecting to external IdP or showing error
     */
    @Override
    public Response performLogin(AuthenticationRequest request) {
        logger.infof("Performing login in CustomOIDCProvider: %s %s", request.getRedirectUri(), request.getState());
        return super.performLogin(request);
    }

    /**
     * Get the claim name to use for the username in the ID token.
     * <p>
     * Purpose: Provide a hook to customize which claim from the ID token
     * should be used as the username when creating or linking users.
     * When invoked: Called during identity extraction to determine the
     * username claim.
     */
    @Override
    protected String getusernameClaimNameForIdToken() {
        return super.getusernameClaimNameForIdToken();
    }

    /**
     * Check if the auth_time claim indicates that authentication time has expired.
     * <p>
     * Purpose: Validate the auth_time claim against the required max_age
     * to determine if re-authentication is needed.
     * When invoked: Called during token validation to enforce max_age
     * requirements.
     */
    @Override
    protected boolean isAuthTimeExpired(JsonWebToken idToken, AuthenticationSessionModel authSession) {
        return super.isAuthTimeExpired(idToken, authSession);
    }

    /**
     * Create the refresh token request to obtain a new access token.
     * <p>
     * Purpose: Provide a hook to customize the refresh token request
     * (headers, parameters) before sending it to the identity provider.
     * When invoked: Called when the provider needs to refresh an access token
     * using a refresh token.
     */
    @Override
    protected SimpleHttp getRefreshTokenRequest(KeycloakSession session, String refreshToken, String clientId, String clientSecret) {
        return super.getRefreshTokenRequest(session, refreshToken, clientId, clientSecret);
    }

    /**
     * Handle Keycloak-initiated browser logout.
     * <p>
     * Purpose: Provide a hook to customize the logout process when
     * Keycloak initiates a browser logout for the user.
     * When invoked: Called during the logout flow to perform any
     * necessary actions before completing the logout.
     */
    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        return super.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
    }

    /**
     * Handle backchannel logout for the user session.
     * <p>
     * Purpose: Provide a hook to customize the backchannel logout
     * process for the given user session.
     * When invoked: Called during backchannel logout to perform
     * necessary actions for the user session.
     */
    @Override
    protected void backchannelLogout(UserSessionModel userSession, String idToken) {
        super.backchannelLogout(userSession, idToken);
    }

    /**
     * Exchange a token for another token.
     * <p>
     * Purpose: Provide a hook to customize the token exchange
     * process when exchanging one token for another.
     * When invoked: Called during token exchange flows.
     */
    @Override
    public Response exchangeFromToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject, MultivaluedMap<String, String> params) {
        return super.exchangeFromToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject, params);
    }

    /**
     * Exchange a session token for another token.
     * <p>
     * Purpose: Provide a hook to customize the session token
     * exchange process.
     * When invoked: Called during session token exchange flows.
     */
    @Override
    protected Response exchangeSessionToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return super.exchangeSessionToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject);
    }

    /**
     * Export the identity provider configuration.
     * <p>
     * Purpose: Provide a hook to customize the export
     * of the identity provider configuration.
     * When invoked: Called during export operations.
     */
    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        return super.export(uriInfo, realm, format);
    }

    /**
     * Handle backchannel logout for the user session with realm context.
     * <p>
     * Purpose: Provide a hook to customize the backchannel logout
     * process with realm context.
     * When invoked: Called during backchannel logout with realm.
     */
    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        super.backchannelLogout(session, userSession, uriInfo, realm);
    }

    /**
     * Refresh the token during logout process.
     * <p>
     * Purpose: Provide a hook to customize the token refresh
     * process during logout.
     * When invoked: Called during logout to refresh tokens.
     */
    @Override
    public String refreshTokenForLogout(KeycloakSession session, UserSessionModel userSession) {
        return super.refreshTokenForLogout(session, userSession);
    }

    /**
     * Handle token exchange when the token is expired.
     * <p>
     * Purpose: Provide a hook to customize the token exchange
     * process when the token has expired.
     * When invoked: Called during token exchange for expired tokens.
     */
    @Override
    public Response exchangeTokenExpired(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return super.exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
    }

    /**
     * Exchange external tokens for Keycloak tokens.
     * <p>
     * Purpose: Provide a hook to customize the exchange
     * of external tokens for Keycloak tokens.
     * When invoked: Called during external token exchange flows.
     */
    @Override
    protected BrokeredIdentityContext exchangeExternalImpl(EventBuilder event, MultivaluedMap<String, String> params) {
        return super.exchangeExternalImpl(event, params);
    }

    /**
     * Handle unsupported token exchange requests.
     * <p>
     * Purpose: Provide a hook to customize the response
     * when a token exchange request is not supported.
     * When invoked: Called during token exchange flows.
     */
    @Override
    public Response exchangeNotSupported() {
        return super.exchangeNotSupported();
    }

    /**
     * Handle token exchange when the user is not linked.
     * <p>
     * Purpose: Provide a hook to customize the response
     * when a token exchange is attempted for a non-linked user.
     * When invoked: Called during token exchange flows.
     */
    @Override
    public Response exchangeNotLinked(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return super.exchangeNotLinked(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
    }

    /**
     * Handle token exchange when the user is not linked and no storage is available.
     * <p>
     * Purpose: Provide a hook to customize the response
     * when a token exchange is attempted for a non-linked user
     * and no user storage is available.
     * When invoked: Called during token exchange flows.
     */
    @Override
    public Response exchangeNotLinkedNoStore(UriInfo uriInfo, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        return super.exchangeNotLinkedNoStore(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
    }

    /**
     * Handle unsupported required token type during exchange.
     * <p>
     * Purpose: Provide a hook to customize the response
     * when an unsupported required token type is encountered.
     * When invoked: Called during token exchange flows.
     */
    @Override
    public Response exchangeUnsupportedRequiredType() {
        return super.exchangeUnsupportedRequiredType();
    }

    /**
     * Check if external exchange is supported.
     * <p>
     * Purpose: Provide a hook to customize the check
     * for whether external exchange is supported.
     * When invoked: Called during token exchange flows.
     */
    @Override
    protected boolean supportsExternalExchange() {
        return super.supportsExternalExchange();
    }

    /**
     * Check if there is an external exchange token present.
     * <p>
     * Purpose: Provide a hook to customize the check
     * for the presence of an external exchange token.
     * When invoked: Called during token exchange flows.
     */
    @Override
    protected Response hasExternalExchangeToken(EventBuilder event, UserSessionModel tokenUserSession, MultivaluedMap<String, String> params) {
        return super.hasExternalExchangeToken(event, tokenUserSession, params);
    }

    /**
     * Custom OIDC Endpoint to handle callback requests.
     * <p>
     * This class extends the OIDCEndpoint to provide custom handling of
     * the authorization response from the external identity provider.
     * <p>
     * <strong>Endpoint Flow:</strong>
     * <ol>
     *   <li>{@link #authResponse(String, String, String, String)} - Process authorization response</li>
     *   <li>{@link #generateTokenRequest(String)} - Create token exchange request</li>
     *   <li>{@link #logoutResponse(String)} - Handle logout responses (if applicable)</li>
     * </ol>
     */
    protected static class CustomOIDCEndpoint extends OIDCEndpoint {

        /**
         * Construct the custom endpoint to handle callback requests.
         * <p>
         * <strong>Purpose:</strong> Create endpoint instance bound to the current realm, event
         * builder and provider which will process the authorization callback.
         * <p>
         * <strong>When invoked:</strong> Instantiated by callback() when Keycloak requires an
         * endpoint for handling the provider callback URL.
         * <p>
         * <strong>What you can customize:</strong>
         * <ul>
         *   <li>Initialize custom endpoint state</li>
         *   <li>Set up request processing configuration</li>
         *   <li>Prepare logging and monitoring</li>
         * </ul>
         */
        public CustomOIDCEndpoint(AuthenticationCallback callback,
                                  RealmModel realm,
                                  EventBuilder event,
                                  OIDCIdentityProvider provider) {
            super(callback, realm, event, provider);
        }

        /**
         * Handle the authorization response from the external IdP.
         * <p>
         * <strong>Purpose:</strong> Called to process the authorization endpoint response
         * (contains state, authorization code or errors) and produce a
         * corresponding HTTP response for the client.
         * <p>
         * <strong>When invoked:</strong> Triggered by HTTP requests to the broker callback URL
         * after the user authenticates at the external identity provider.
         * <p>
         * <strong>Execution Order:</strong> First endpoint method called after user returns from IdP
         * <p>
         * <strong>What you can customize:</strong>
         * <ul>
         *   <li>Error response handling and formatting</li>
         *   <li>State validation logic</li>
         *   <li>Authorization code processing</li>
         *   <li>Security checks and validation</li>
         *   <li>Custom logging and auditing</li>
         *   <li>Response transformation</li>
         * </ul>
         * <p>
         * <strong>What you should do:</strong>
         * <ul>
         *   <li>Validate state parameter for CSRF protection</li>
         *   <li>Handle both success and error responses</li>
         *   <li>Log important events for debugging</li>
         *   <li>Call super.authResponse() for standard processing</li>
         * </ul>
         * <p>
         * <strong>What you need to do:</strong>
         * <ul>
         *   <li>Must return valid HTTP Response</li>
         *   <li>Should validate all parameters</li>
         *   <li>Must handle error cases properly</li>
         * </ul>
         * <p>
         * <strong>Parameter validation:</strong>
         * <ul>
         *   <li><strong>state:</strong> Must match original request</li>
         *   <li><strong>code:</strong> Required for success flow</li>
         *   <li><strong>error:</strong> Handle error conditions</li>
         *   <li><strong>error_description:</strong> Provide user feedback</li>
         * </ul>
         *
         * @param state             The state parameter for CSRF protection
         * @param authorizationCode The authorization code from IdP (success case)
         * @param error             The error code from IdP (error case)
         * @param errorDescription  Human-readable error description
         * @return HTTP Response continuing the authentication flow
         */
        @Override
        public Response authResponse(String state, String authorizationCode, String error, String errorDescription) {
            logger.infof("Auth response in CustomOIDCEndpoint: %s %s %s %s", state, authorizationCode, error, errorDescription);
            var response = super.authResponse(state, authorizationCode, error, errorDescription);
            logger.infof("Auth response generated in CustomOIDCEndpoint: %s %s %s", response.getStatus(), response.getLocation(), response.getEntity());
            return response;
        }

        /**
         * Generate the token request used to exchange an authorization code for tokens.
         * <p>
         * <strong>Purpose:</strong> Build the HTTP request that will be sent to the token endpoint
         * to exchange the authorization code for access/id/refresh tokens.
         * <p>
         * <strong>When invoked:</strong> Called during the authorization code exchange flow after
         * receiving the authorization code from authResponse.
         * <p>
         * <strong>Execution Order:</strong> Called after authResponse, before token exchange
         * <p>
         * <strong>What you can customize:</strong>
         * <ul>
         *   <li>Additional token request parameters</li>
         *   <li>Custom grant type handling</li>
         *   <li>Request header modifications</li>
         *   <li>PKCE parameter addition</li>
         *   <li>Custom scope requests</li>
         *   <li>Request timeout configuration</li>
         * </ul>
         * <p>
         * <strong>What you should do:</strong>
         * <ul>
         *   <li>Call super.generateTokenRequest() for base setup</li>
         *   <li>Add required OAuth 2.0 parameters</li>
         *   <li>Validate authorization code format</li>
         *   <li>Set appropriate HTTP headers</li>
         * </ul>
         * <p>
         * <strong>What you need to do:</strong>
         * <ul>
         *   <li>Must include authorization code</li>
         *   <li>Should include redirect_uri</li>
         *   <li>Must set grant_type=authorization_code</li>
         * </ul>
         * <p>
         * <strong>Required parameters:</strong>
         * <ul>
         *   <li><strong>grant_type:</strong> "authorization_code"</li>
         *   <li><strong>code:</strong> The authorization code</li>
         *   <li><strong>redirect_uri:</strong> Must match original request</li>
         *   <li><strong>client_id:</strong> OAuth client identifier</li>
         * </ul>
         *
         * @param authorizationCode The authorization code to exchange
         * @return HTTP request configured for token exchange
         */
        @Override
        public SimpleHttp generateTokenRequest(String authorizationCode) {
            try {
                logger.infof("Generating token request in CustomOIDCEndpoint: %s", authorizationCode);
                var response = super.generateTokenRequest(authorizationCode);
                logger.infof("Token request generated in CustomOIDCEndpoint: %s", response.getUrl(), response.asString());
                return response;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Handle the logout response from the external IdP.
         * <p>
         * <strong>Purpose:</strong> Process the logout response and produce a corresponding
         * HTTP response for the client after logout at external IdP.
         * <p>
         * <strong>When invoked:</strong> Triggered by HTTP requests to the broker logout URL
         * after the user logs out at the external identity provider.
         * <p>
         * <strong>Execution Order:</strong> Called during logout flows when IdP responds
         * <p>
         * <strong>What you can customize:</strong>
         * <ul>
         *   <li>Logout confirmation handling</li>
         *   <li>State validation for logout</li>
         *   <li>Custom logout success/error pages</li>
         *   <li>Session cleanup logic</li>
         *   <li>Logout event logging</li>
         *   <li>Post-logout redirects</li>
         * </ul>
         * <p>
         * <strong>What you should do:</strong>
         * <ul>
         *   <li>Validate logout state parameter</li>
         *   <li>Clean up any provider-specific session data</li>
         *   <li>Log logout events for auditing</li>
         *   <li>Call super.logoutResponse() for standard handling</li>
         * </ul>
         * <p>
         * <strong>Common scenarios:</strong>
         * <ul>
         *   <li>Successful logout from external IdP</li>
         *   <li>Logout errors or timeouts</li>
         *   <li>Partial logout scenarios</li>
         *   <li>Session timeout handling</li>
         * </ul>
         *
         * @param state The state parameter for logout request validation
         * @return HTTP Response completing the logout flow
         */
        @Override
        public Response logoutResponse(String state) {
            logger.infof("Logout response in CustomOIDCEndpoint: %s", state);
            var response = super.logoutResponse(state);
            logger.infof("Logout response generated in CustomOIDCEndpoint: %s %s %s", response.getStatus(), response.getLocation(), response.getEntity());
            return response;
        }
    }
}
