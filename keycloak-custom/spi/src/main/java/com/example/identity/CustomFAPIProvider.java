package com.example.identity;

import com.example.config.CustomFapiProviderConfig;
import com.example.utils.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import liquibase.util.MD5Util;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;

import static org.keycloak.utils.MediaType.APPLICATION_JSON_TYPE;

public class CustomFAPIProvider extends CustomOIDCProvider {
    private static final Logger logger = Logger.getLogger(CustomFAPIProvider.class);
    private static final String DPOP_KEY_PAIR_NOTE = "DPOP_KEY_PAIR";
    private static final String DPOP_KEY_ID_NOTE = "DPOP_KEY_ID";
    private static final String PAR_REQUEST_URI_NOTE = "PAR_REQUEST_URI";

    public final CustomFapiProviderConfig configuration;

    public CustomFAPIProvider(KeycloakSession session, CustomFapiProviderConfig config) {
        super(session, config);
        this.configuration = config;
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        logger.infof("[createAuthorizationUrl] Creating FAPI 2.0 authorization URL: %s %s %s",
                    request.getRealm().getName(), request.getRedirectUri(), request.getState());

        var authenticationSession = request.getAuthenticationSession();

        // FAPI 2.0: Enforce PKCE with S256 (mandatory)
        if (configuration.isFAPI2Mode() || configuration.isPkceEnabled()) {
            logger.info("[createAuthorizationUrl] FAPI 2.0 mode: Enforcing PKCE with S256");
            if (!getConfig().isPkceEnabled()) {
                getConfig().setPkceEnabled(true);
            }
            // FAPI 2.0 requires S256 only
            if (!"S256".equals(getConfig().getPkceMethod())) {
                getConfig().setPkceMethod("S256");
            }
        }

        var uriBuilder = FuncUtil.invoke(() -> {
            final var _uriBuilder = UriBuilder.fromUri(getConfig().getAuthorizationUrl())
                    .queryParam(OAUTH2_PARAMETER_SCOPE, getConfig().getDefaultScope())
                    .queryParam(OAUTH2_PARAMETER_STATE, request.getState().getEncoded())
                    .queryParam(OAUTH2_PARAMETER_RESPONSE_TYPE, "code")
                    .queryParam(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                    .queryParam(OAUTH2_PARAMETER_REDIRECT_URI, request.getRedirectUri());

            var loginHint = request.getAuthenticationSession()
                    .getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
            if (getConfig().isLoginHint() && loginHint != null) {
                _uriBuilder.queryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
            }

            if (getConfig().isUiLocales()) {
                var uiLocalesParam = session.getContext()
                        .resolveLocale(null)
                        .toLanguageTag();
                _uriBuilder.queryParam(OIDCLoginProtocol.UI_LOCALES_PARAM, uiLocalesParam);
            }

            var prompt = getConfig().getPrompt();
            if (prompt == null || prompt.isEmpty()) {
                prompt = request.getAuthenticationSession()
                        .getClientNote(OAuth2Constants.PROMPT);
            }
            if (prompt != null) {
                _uriBuilder.queryParam(OAuth2Constants.PROMPT, prompt);
            }

            var acr = request.getAuthenticationSession()
                    .getClientNote(OAuth2Constants.ACR_VALUES);
            if (acr != null) {
                _uriBuilder.queryParam(OAuth2Constants.ACR_VALUES, acr);
            }

            var forwardParameterConfig = getConfig().getForwardParameters() != null
                    ? getConfig().getForwardParameters()
                    : "";
            var forwardParameters = forwardParameterConfig.split("\\s*,\\s*");
            for(var forwardParameter: forwardParameters) {
                var name = AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + forwardParameter.trim();
                var parameter = request.getAuthenticationSession()
                        .getClientNote(name);
                if(parameter != null && !parameter.isEmpty()) {
                    _uriBuilder.queryParam(forwardParameter, parameter);
                }
            }

            // FAPI 2.0: PKCE is mandatory with S256
            if (getConfig().isPkceEnabled()) {
                var codeVerifier = PkceUtils.generateCodeVerifier();
                var codeChallengeMethod = getConfig().getPkceMethod();
                request.getAuthenticationSession().setClientNote("BROKER_CODE_CHALLENGE", codeVerifier);
                request.getAuthenticationSession().setClientNote("BROKER_CODE_CHALLENGE_METHOD", codeChallengeMethod);

                var codeChallenge = PkceUtils.encodeCodeChallenge(codeVerifier, codeChallengeMethod);
                _uriBuilder.queryParam(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
                _uriBuilder.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
                logger.infof("[createAuthorizationUrl] PKCE enabled with method: %s", codeChallengeMethod);
            }

            // FAPI 2.0: JARM support - add response_mode if enabled
            if (configuration.isJARMEnabled()) {
                var responseMode = configuration.getJARMResponseMode();
                _uriBuilder.queryParam("response_mode", responseMode);
                logger.infof("[createAuthorizationUrl] JARM enabled with response_mode: %s", responseMode);
            }

            // FAPI 2.0: Add dpop_jkt parameter if DPoP is enabled (sender-constrained tokens)
            if (configuration.isDPoPEnabled()) {
                try {
                    var keyIdConfig = configuration.getDPoPSigningKeyId();
                    if (keyIdConfig == null || keyIdConfig.isEmpty()) {
                        keyIdConfig = configuration.getSigningKeyId();
                    }

                    if (keyIdConfig != null && !keyIdConfig.isEmpty()) {
                        final String keyId = keyIdConfig;
                        
                        // Store the DPoP key ID in the authentication session for later use
                        authenticationSession.setClientNote(DPOP_KEY_ID_NOTE, keyId);
                        logger.infof("[createAuthorizationUrl] Stored DPoP key ID in session: %s", keyId);
                        
                        var keyWrapper = session.keys()
                                .getKeysStream(session.getContext().getRealm())
                                .filter(key -> key.getKid().equalsIgnoreCase(keyId))
                                .findFirst();

                        if (keyWrapper.isPresent()) {
                            var publicKey = keyWrapper.get().getPublicKey();
                            var jwkThumbprint = DPoPUtil.generateJWKThumbprint(publicKey);
                            
                            // Store the JWK thumbprint for validation
                            authenticationSession.setClientNote(DPOP_KEY_PAIR_NOTE, jwkThumbprint);
                            logger.infof("[createAuthorizationUrl] Stored DPoP JWK thumbprint in session: %s", jwkThumbprint);
                            
                            _uriBuilder.queryParam("dpop_jkt", jwkThumbprint);
                            logger.infof("[createAuthorizationUrl] Added dpop_jkt parameter for sender-constrained tokens");
                        }
                    }
                } catch (Exception e) {
                    logger.errorf(e, "[createAuthorizationUrl] Failed to add dpop_jkt parameter: %s", e.getMessage());
                }
            }

            return _uriBuilder;
        });

        // FAPI 2.0: Nonce is mandatory (32 bytes)
        if (!getConfig().isDisableNonce()) {
            var nonce = Base64Url.encode(SecretGenerator.getInstance().randomBytes(32));
            authenticationSession.setClientNote("BROKER_NONCE", nonce);
            uriBuilder.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce);
            logger.infof("[createAuthorizationUrl] Nonce generated and added (32 bytes)");
        }

        var maxAge = request.getAuthenticationSession()
                .getClientNote(OIDCLoginProtocol.MAX_AGE_PARAM);
        if (getConfig().isPassMaxAge() && maxAge != null) {
            uriBuilder.queryParam(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
        }

        // FAPI 2.0: PAR (Pushed Authorization Request)
        if (configuration.isPAREnabled() && configuration.getPAREndpoint() != null) {
            logger.info("[createAuthorizationUrl] PAR enabled, will use Pushed Authorization Request");
            try {
                var requestUri = performPushedAuthorizationRequest(request, uriBuilder);
                authenticationSession.setClientNote(PAR_REQUEST_URI_NOTE, requestUri);

                // Replace query parameters with just request_uri and client_id
                var parUriBuilder = UriBuilder.fromUri(getConfig().getAuthorizationUrl())
                        .queryParam(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                        .queryParam("request_uri", requestUri);

                logger.infof("[createAuthorizationUrl] PAR request_uri obtained: %s", requestUri);
                return parUriBuilder;
            } catch (Exception e) {
                logger.errorf(e, "[createAuthorizationUrl] PAR request failed: %s", e.getMessage());
                throw new RuntimeException("PAR request failed", e);
            }
        }

        return uriBuilder;
    }

    /**
     * Performs Pushed Authorization Request (PAR) for FAPI 2.0
     */
    private String performPushedAuthorizationRequest(AuthenticationRequest request, UriBuilder uriBuilder) throws IOException {
        logger.info("[performPushedAuthorizationRequest] Initiating PAR request");

        var parEndpoint = configuration.getPAREndpoint();
        if (!PARUtil.isValidPAREndpoint(parEndpoint)) {
            throw new IllegalStateException("Invalid PAR endpoint: " + parEndpoint);
        }

        // Build PAR request with all authorization parameters
        var parRequest = SimpleHttp.doPost(parEndpoint, session);

        // Add all query parameters from the authorization URL as form parameters
        var queryParams = uriBuilder.build().getQuery();
        if (queryParams != null) {
            for (var param : queryParams.split("&")) {
                var keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    parRequest.param(keyValue[0], keyValue[1]);
                }
            }
        }

        // Authenticate the PAR request (same as token request)
        parRequest = authenticateTokenRequest(parRequest);

        // Send PAR request and get request_uri
        return PARUtil.pushAuthorizationRequest(session, parEndpoint, parRequest);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new CustomFAPI2Endpoint(callback, realm, event, this);
    }

    @Override
    public SimpleHttp authenticateTokenRequest(SimpleHttp tokenRequest) {
        logger.infof("[authenticateTokenRequest] Authenticating token request with FAPI 2.0 compliance");

        // FAPI 2.0: DPoP support
        if (configuration.isDPoPEnabled()) {
            logger.info("[authenticateTokenRequest] DPoP enabled, adding DPoP proof");
            try {
                var dpopProof = generateDPoPProof(
                    "POST",
                    getConfig().getTokenUrl(),
                    null // No access token hash for token request
                );
                tokenRequest.header("DPoP", dpopProof);
                logger.infof("[authenticateTokenRequest] DPoP header added to token request");
            } catch (Exception e) {
                logger.errorf(e, "[authenticateTokenRequest] Failed to generate DPoP proof: %s", e.getMessage());
            }
        }

        if (getConfig().isJWTAuthentication()) {
            String jws = null;
            if (configuration.getSigningKeyId() == null
                    || configuration.getSigningKeyId().isEmpty()) {
                jws = new JWSBuilder()
                        .type(OAuth2Constants.JWT)
                        .jsonContent(generateToken())
                        .sign(getSignatureContext());
            } else {
                var keyWrapper = session.keys()
                        .getKeysStream(session.getContext().getRealm())
                        .filter(key -> key.getKid().equalsIgnoreCase(this.configuration.getSigningKeyId()))
                        .findFirst();
                if (keyWrapper.isPresent()) {
                    var key = keyWrapper.get();
                    var algorithm = JWSAlgorithm.parse(key.getAlgorithm());
                    logger.infof("[authenticateTokenRequest] Signing key id: %s", this.configuration.getSigningKeyId());
                    logger.infof("[authenticateTokenRequest] Found key for signing the request: %s", key.getAlgorithm());
                    logger.infof("[authenticateTokenRequest] Using key with KID: %s and algorithm: %s", key.getKid(), algorithm);
                    jws = JwtUtil.createBearer(configuration, key, algorithm);
                } else {
                    logger.errorf("[authenticateTokenRequest] Unable to find matching key for signing the request %s", this.configuration.getSigningKeyId());
                }
            }
            logger.infof("[authenticateTokenRequest] Client ID: %s", getConfig().getClientId());
            logger.infof("[authenticateTokenRequest] Client Assertion Type: %s", OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
            logger.infof("[authenticateTokenRequest] JWS: %s", jws);
            return tokenRequest.param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                    .param(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)
                    .param(OAuth2Constants.CLIENT_ASSERTION, jws);
        }

        try (var vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            if (getConfig().isBasicAuthentication()) {
                logger.infof("[authenticateTokenRequest] Using basic authentication for client ID: %s", getConfig().getClientId());
                logger.infof("[authenticateTokenRequest] Client Secret from Vault: %s", vaultStringSecret.get().orElse("Not found in vault"));
                var clientSecret = vaultStringSecret.get()
                        .orElse(getConfig().getClientSecret());
                return tokenRequest.authBasic(
                        getConfig().getClientId(),
                        clientSecret);
            }

            logger.infof("[authenticateTokenRequest] Using form authentication for client ID: %s", getConfig().getClientId());
            logger.infof("[authenticateTokenRequest] Client Secret from Vault: %s", vaultStringSecret.get().orElse("Not found in vault"));
            var clientSecret = vaultStringSecret.get()
                    .orElse(getConfig().getClientSecret());
            return tokenRequest.param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                    .param(OAUTH2_PARAMETER_CLIENT_SECRET, clientSecret);
        }
    }

    /**
     * Generates DPoP proof for FAPI 2.0 compliance
     * Uses the stored DPoP key from the authorization session to ensure consistency
     */
    private String generateDPoPProof(String httpMethod, String httpUri, String accessTokenHash) {
        // First, try to get the DPoP key ID from the authentication session (stored during authorization)
        var authSession = session.getContext().getAuthenticationSession();
        var storedKeyId = authSession != null ? authSession.getClientNote(DPOP_KEY_ID_NOTE) : null;
        
        var keyIdConfig = storedKeyId != null ? storedKeyId : configuration.getDPoPSigningKeyId();
        if (keyIdConfig == null || keyIdConfig.isEmpty()) {
            keyIdConfig = configuration.getSigningKeyId();
        }

        if (keyIdConfig == null || keyIdConfig.isEmpty()) {
            throw new IllegalStateException("No signing key configured for DPoP");
        }

        final String keyId = keyIdConfig; // Make effectively final for lambda
        
        logger.infof("[generateDPoPProof] Using DPoP key ID: %s (from session: %s)", 
                    keyId, storedKeyId != null ? "yes" : "no");

        var keyWrapper = session.keys()
                .getKeysStream(session.getContext().getRealm())
                .filter(key -> key.getKid().equalsIgnoreCase(keyId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DPoP signing key not found: " + keyId));
        
        // Verify the key matches the stored thumbprint if available
        if (authSession != null) {
            var storedThumbprint = authSession.getClientNote(DPOP_KEY_PAIR_NOTE);
            if (storedThumbprint != null) {
                try {
                    var currentThumbprint = DPoPUtil.generateJWKThumbprint(keyWrapper.getPublicKey());
                    if (!storedThumbprint.equals(currentThumbprint)) {
                        logger.warnf("[generateDPoPProof] DPoP key mismatch! Expected: %s, Got: %s", 
                                    storedThumbprint, currentThumbprint);
                        throw new IllegalStateException("DPoP key mismatch - key has changed during the flow");
                    }
                    logger.infof("[generateDPoPProof] DPoP key verification successful");
                } catch (Exception e) {
                    logger.errorf(e, "[generateDPoPProof] Failed to verify DPoP key consistency: %s", e.getMessage());
                }
            }
        }

        return DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, accessTokenHash);
    }

    @Override
    protected void processAccessTokenResponse(BrokeredIdentityContext context, AccessTokenResponse response) {
        var isJwt = JwtUtil.isJwt(response.getToken());
        logger.infof("[processAccessTokenResponse] Process Access Token is jwt = %s %s %s %s", isJwt, response.getToken(), response.getTokenType(), response.toString());

        // FAPI 2.0: Validate token_type for DPoP
        if (configuration.isDPoPEnabled() && !"DPoP".equalsIgnoreCase(response.getTokenType())) {
            logger.warnf("[processAccessTokenResponse] Expected DPoP token type but got: %s", response.getTokenType());
        }

        if (isJwt && getConfig().isAccessTokenJwt()) {
            logger.infof("[processAccessTokenResponse] Access token is JWT, validating token");
            var access = validateToken(response.getToken(), true);
            context.getContextData().put(VALIDATED_ACCESS_TOKEN, access);
        } else {
            logger.infof("[processAccessTokenResponse] Access token is not JWT, skipping validation");
        }
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        logger.infof("[getFederatedIdentity] Getting federated identity with FAPI 2.0 compliance: %s", response);

        // FAPI 2.0: ID Token is always encrypted - decrypt it
        var tokenResponse = JwtUtil.decryptAccessTokenResponse(response, session, this.configuration.isEncryptedIdTokenFlag());
        var accessToken = JwtUtil.verifyAccessToken(tokenResponse);
        var encodedIdToken = tokenResponse.getIdToken();
        
        logger.infof("[getFederatedIdentity] FAPI 2.0 - Decrypted ID Token: %s", encodedIdToken);
        logger.infof("[getFederatedIdentity] Access Token: %s", accessToken);

        try {
            var idToken = this.validateToken(encodedIdToken);
            var identity = this.extractIdentity(tokenResponse, accessToken, idToken);
            var nonce = idToken.getOtherClaims().get("nonce");
            logger.infof("[getFederatedIdentity] Extracted identity: %s with nonce: %s", identity.getUsername(), nonce);
            identity.getContextData().put("BROKER_NONCE", nonce);
            
            if (this.getConfig().isStoreToken()) {
                if (tokenResponse.getExpiresIn() > 0L) {
                    var accessTokenExpiration = (long) org.keycloak.common.util.Time.currentTime() + tokenResponse.getExpiresIn();
                    tokenResponse.getOtherClaims().put("accessTokenExpiration", accessTokenExpiration);
                    response = JsonSerialization.writeValueAsString(tokenResponse);
                    logger.infof("[getFederatedIdentity] Access token expiration set to: %s", accessTokenExpiration);
                } else {
                    logger.infof("[getFederatedIdentity] Token expiration not set in response.");
                }
                identity.setToken(response);
            } else {
                logger.infof("[getFederatedIdentity] Not storing token as per configuration.");
            }
            
            // FAPI 2.0: Log token type validation
            if (configuration.isDPoPEnabled()) {
                logger.infof("[getFederatedIdentity] FAPI 2.0 DPoP mode - Token type: %s", tokenResponse.getTokenType());
            }
            
            return identity;
        } catch (IOException e) {
            throw new IdentityBrokerException("[getFederatedIdentity] Could not fetch attributes from userinfo endpoint.", e);
        }
    }

    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse,
                                                      String accessToken,
                                                      JsonWebToken idToken) throws IOException {
        logger.infof("[extractIdentity] Extracting identity in CustomOIDCProvider");
        var subKeyValue = JwtUtil.subToKeyValue(idToken.getSubject());
        var id = idToken.getSubject();
        var brokerUserId = getConfig().getAlias() + "." + MD5Util.computeMD5(id);
        var identity = new BrokeredIdentityContext(id, getConfig());
        var name = (String) idToken.getOtherClaims().get(IDToken.NAME);
        var givenName = (String) idToken.getOtherClaims().get(IDToken.GIVEN_NAME);
        var familyName = (String) idToken.getOtherClaims().get(IDToken.FAMILY_NAME);
        var preferredUsername = "";
        if (subKeyValue.get("s") != null) {
            logger.infof("[toIdentityContext] Found 's' in sub claim: %s", subKeyValue.get("s"));
            preferredUsername = subKeyValue.get("s");
        } else {
            logger.infof("[toIdentityContext] 's' not found in sub claim, using subject: %s", idToken.getSubject());
            preferredUsername = idToken.getSubject();
        }
        var email = (String) idToken.getOtherClaims().get(IDToken.EMAIL);
        var userInfoUrl = getUserInfoUrl();
        logger.infof("[toIdentityContext] Initial values: id=%s, brokerUserId=%s, name=%s, givenName=%s, familyName=%s, preferredUsername=%s, email=%s, userInfoUrl=%s",
                id,
                brokerUserId,
                name,
                givenName,
                familyName,
                preferredUsername,
                email,
                userInfoUrl);

        if (!getConfig().isDisableUserInfoService()
                && userInfoUrl != null
                && !userInfoUrl.isEmpty()
                && accessToken != null) {
            logger.infof("[toIdentityContext] HTTP Request ---> %s", userInfoUrl);
            var userInfoRequest = SimpleHttp.doGet(userInfoUrl, session)
                    .header("Authorization", "DPoP " + accessToken);

            // FAPI 2.0: Add DPoP header for userinfo request if enabled
            if (configuration.isDPoPEnabled()) {
                try {
                    var accessTokenHash = DPoPUtil.generateAccessTokenHash(accessToken);
                    var dpopProof = generateDPoPProof("GET", userInfoUrl, accessTokenHash);
                    userInfoRequest.header("DPoP", dpopProof);
                    logger.infof("[toIdentityContext] DPoP header added to userinfo request");
                } catch (Exception e) {
                    logger.errorf(e, "[toIdentityContext] Failed to add DPoP header to userinfo request: %s", e.getMessage());
                }
            }

            try (var response = HttpUtil.executeRequest(
                    userInfoUrl,
                    userInfoRequest)) {
                logger.infof("[toIdentityContext] HTTP Response <--- %s", userInfoUrl);
                for (var header : response.getAllHeaders()) {
                    logger.infof("[toIdentityContext] HTTP Response Header: %s -> %s", header.getName(), header.getValue());
                }
                var contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
                var contentMediaType = FuncUtil.invoke(() -> MediaType.valueOf(contentType));
                if (contentMediaType == null || contentMediaType.isWildcardSubtype() || contentMediaType.isWildcardType()) {
                    throw new RuntimeException("[toIdentityContext] Unsupported content-type [" + contentType + "] in response from [" + userInfoUrl + "].");
                }

                var userInfo = FuncUtil.invoke(() -> {
                    if (APPLICATION_JSON_TYPE.isCompatible(contentMediaType)) {
                        return response.asJson();
                    } else if (MediaType.valueOf("application/jwt").isCompatible(contentMediaType)) {
                        var parsed = this.parseTokenInput(response.asString(), false);
                        return JsonSerialization.readValue(parsed, JsonNode.class);
                    } else {
                        throw new RuntimeException("[toIdentityContext] Unsupported content-type [" + contentType + "] in response from [" + userInfoUrl + "].");
                    }
                });
                identity.getContextData().put(USER_INFO, userInfo);
                id = getJsonProperty(userInfo, "sub");
                givenName = getJsonProperty(userInfo, IDToken.GIVEN_NAME);
                familyName = getJsonProperty(userInfo, IDToken.FAMILY_NAME);
                email = getJsonProperty(userInfo, "email");
                AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, userInfo, getConfig().getAlias());

                logger.infof("[toIdentityContext] UserInfo response: %s", userInfo.toString());
                JsonUtil.printAll(userInfo, "");

                for (var entry : JsonUtil.flattenJsonNode(userInfo, null, new HashMap<>()).entrySet()) {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    identity.setUserAttribute(key, value);

                    if (key.contains("value")) {
                        if (key.contains("email")) {
                            email = value;
                        }
                        if (key.contains("name")) {
                            name = value;
                            givenName = value;
                        }
                        if (key.contains("aliasname")) {
                            familyName = value;
                        }
                        if (key.contains("uinfin") && key.contains("value")) {
                            preferredUsername = value;
                            logger.infof("[toIdentityContext] Setting preferredUsername from uinfin value: %s", preferredUsername);
                        }
                    }
                }
            }
        } else {
            logger.infof("[toIdentityContext] Skipping user info request as per configuration.");
        }

        identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);
        identity.getContextData().put(VALIDATED_ACCESS_TOKEN, accessToken);
        identity.setId(id);
        if (givenName != null) {
            identity.setFirstName(givenName);
        }
        if (familyName != null) {
            identity.setLastName(familyName);
        }
        if (givenName == null && familyName == null) {
            identity.setName(name);
        }

        identity.setEmail(email);
        identity.setBrokerUserId(brokerUserId);

        if (tokenResponse != null && tokenResponse.getSessionState() != null) {
            var brokerSessionId = getConfig().getAlias() + "." + tokenResponse.getSessionState();
            logger.infof("[toIdentityContext] Setting broker session ID: %s", brokerSessionId);
            identity.setBrokerSessionId(brokerSessionId);
        }
        if (tokenResponse != null) {
            processAccessTokenResponse(identity, tokenResponse);
            var accessTokenKeyValue = MapperUtil.toMap(tokenResponse);
            for (var entry : accessTokenKeyValue.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                logger.infof("[toIdentityContext] Access Token Claim ---> attribute: %s = %s", key, value);
                identity.setUserAttribute(key, value);
            }
        }

        var idTokenKeyValue = MapperUtil.toMap(idToken);
        for (var entry : idTokenKeyValue.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            logger.infof("[toIdentityContext] ID Token Claim ---> attribute: %s = %s", key, value);
            identity.setUserAttribute(key, value);
            if ("id_token.entityInfo.CPEntID".equalsIgnoreCase(key)) {
                preferredUsername = value;
                if (subKeyValue.get("s") != null) {
                    preferredUsername = value + "-" + subKeyValue.get("s");
                }
                logger.infof("[toIdentityContext] Setting preferredUsername from CPEntID: %s", preferredUsername);
            }
        }

        if (configuration.isStoreToken()) {
            identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
            identity.getContextData().put(FEDERATED_ID_TOKEN, idToken);
            identity.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
            if (tokenResponse != null && tokenResponse.getRefreshToken() != null) {
                identity.getContextData().put(FEDERATED_REFRESH_TOKEN, tokenResponse.getRefreshToken());
                identity.getContextData().put(FEDERATED_TOKEN_EXPIRATION, tokenResponse.getExpiresIn());
            }
        }

        if (preferredUsername == null) {
            preferredUsername = email;
            logger.infof("[toIdentityContext] Setting preferredUsername from email: %s", preferredUsername);
        }
        if (preferredUsername == null) {
            preferredUsername = id;
            logger.infof("[toIdentityContext] Setting preferredUsername from subject id: %s", preferredUsername);
        }
        identity.setUsername(preferredUsername);

        logger.infof("[extractIdentity] Initial extracted identity: %s with id: %s", identity.getUsername(), identity.getId());
        for (var entry : identity.getContextData().entrySet()) {
            var attrKey = entry.getKey();
            var attrValues = entry.getValue();
            logger.infof("[toIdentityContext] Context Data: %s = %s", attrKey, attrValues);
        }
        for (var entry : identity.getAttributes().entrySet()) {
            var attrKey = entry.getKey();
            var attrValues = entry.getValue();
            logger.infof("[toIdentityContext] User Attribute: %s = %s", attrKey, attrValues);
        }

        return identity;
    }

    protected static class CustomFAPI2Endpoint extends OIDCEndpoint {
        private final OIDCIdentityProvider provider;
        private final CustomFAPIProvider fapiProvider;

        public CustomFAPI2Endpoint(AuthenticationCallback callback,
                                  RealmModel realm,
                                  EventBuilder event,
                                  CustomFAPIProvider provider) {
            super(callback, realm, event, provider);
            this.provider = provider;
            this.fapiProvider = provider;
        }

        @Override
        public Response authResponse(String state, String authorizationCode, String error, String errorDescription) {
            logger.infof("[authResponse] FAPI 2.0 Auth response: state=%s, code=%s, error=%s, errorDescription=%s",
                        state, authorizationCode, error, errorDescription);

            // FAPI 2.0: Handle JARM response if enabled
            var providerConfig = provider.getConfig();
            if (providerConfig instanceof CustomFapiProviderConfig config) {
                if (config.isJARMEnabled()) {
                    logger.info("[authResponse] JARM enabled, checking for JWT response");
                    // Check if response parameter exists (JARM)
                    var responseJwt = session.getContext().getUri().getQueryParameters().getFirst("response");
                    if (responseJwt != null && !responseJwt.isEmpty()) {
                        logger.info("[authResponse] Processing JARM JWT response");
                        return handleJARMResponse(state, responseJwt);
                    }
                }
            }

            var _authResponse = FuncUtil.invoke(() -> {
                if (state == null) {
                    var providerId = providerConfig.getProviderId();
                    var redirectionUrl = session.getContext().getUri().getRequestUri().toString();
                    logger.errorf("%s. providerId=%s, redirectionUrl=%s", "Redirection URL does not contain a state parameter", providerId, redirectionUrl);

                    event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                    event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_MISSING_STATE_ERROR);
                }

                try {
                    var authSession = this.callback.getAndVerifyAuthenticationSession(state);
                    session.getContext().setAuthenticationSession(authSession);
                    if (error != null) {
                        var providerId = providerConfig.getProviderId();
                        var redirectionUrl = session.getContext().getUri().getRequestUri().toString();
                        logger.errorf("%s. providerId=%s, redirectionUrl=%s", "Redirection URL contains an error", providerId, redirectionUrl);

                        if (error.equals(ACCESS_DENIED)) {
                            return callback.cancelled(providerConfig);
                        } else if (error.equals(OAuthErrorException.LOGIN_REQUIRED)
                                || error.equals(OAuthErrorException.INTERACTION_REQUIRED)) {
                            return callback.error(error);
                        } else if (error.equals(OAuthErrorException.TEMPORARILY_UNAVAILABLE)
                                && Constants.AUTHENTICATION_EXPIRED_MESSAGE.equals(errorDescription)) {
                            return callback.retryLogin(this.provider, authSession);
                        } else {
                            return callback.error(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                        }
                    }

                    if (authorizationCode == null) {
                        var providerId = providerConfig.getProviderId();
                        var redirectionUrl = session.getContext().getUri().getRequestUri().toString();
                        logger.errorf("%s. providerId=%s, redirectionUrl=%s", "Redirection URL neither contains a code nor error parameter", providerId, redirectionUrl);

                        event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                        event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                        return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_MISSING_CODE_OR_ERROR_ERROR);
                    }

                    var simpleHttp = generateTokenRequest(authorizationCode);
                    String response;
                    try (SimpleHttp.Response simpleResponse = simpleHttp.asResponse()) {
                        var status = simpleResponse.getStatus();
                        var success = status >= 200 && status < 400;
                        response = simpleResponse.asString();

                        if (!success) {
                            logger.errorf("Unexpected response from token endpoint %s. status=%s, response=%s",
                                    simpleHttp.getUrl(),
                                    status, response);

                            event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                            event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                            return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                        }
                    }

                    var federatedIdentity = provider.getFederatedIdentity(response);
                    if (providerConfig.isStoreToken()) {
                        // make sure that token wasn't already set by getFederatedIdentity();
                        // want to be able to allow provider to set the token itself.
                        if (federatedIdentity.getToken() == null)
                            federatedIdentity.setToken(response);
                    }
                    federatedIdentity.setIdp(provider);
                    federatedIdentity.setAuthenticationSession(authSession);

                    return callback.authenticated(federatedIdentity);
                } catch (WebApplicationException e) {
                    return e.getResponse();
                } catch (IdentityBrokerException e) {
                    if (e.getMessageCode() != null) {
                        event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                        event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                        return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, e.getMessageCode());
                    }

                    logger.error("Failed to make identity provider oauth callback", e);
                    event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                    event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                } catch (Exception e) {

                    logger.error("Failed to make identity provider oauth callback", e);
                    event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                    event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
            });
            logger.infof("[authResponse] Auth response generated: %s %s %s",
                    _authResponse.getStatus(),
                    _authResponse.getLocation(),
                    _authResponse.getEntity());

            return _authResponse;
        }

        /**
         * Handles JARM (JWT-Secured Authorization Response Mode) responses
         */
        private Response handleJARMResponse(String state, String responseJwt) {
            logger.infof("[handleJARMResponse] Processing JARM response JWT");

            try {
                // Parse and validate the JWT response
                var parsedJwt = fapiProvider.parseTokenInput(responseJwt, true);
                var jarmResponse = JsonSerialization.readValue(parsedJwt, JsonWebToken.class);

                // Extract code and state from JWT
                var code = (String) jarmResponse.getOtherClaims().get("code");
                var jarmState = (String) jarmResponse.getOtherClaims().get("state");
                var error = (String) jarmResponse.getOtherClaims().get("error");
                var errorDescription = (String) jarmResponse.getOtherClaims().get("error_description");

                logger.infof("[handleJARMResponse] JARM JWT validated: state=%s, code=%s", jarmState, code);

                // Validate state matches
                if (!state.equals(jarmState)) {
                    logger.errorf("[handleJARMResponse] State mismatch: expected=%s, got=%s", state, jarmState);
                    throw new IdentityBrokerException("State mismatch in JARM response");
                }

                // Continue with normal flow using extracted code and error
                return authResponse(state, code, error, errorDescription);

            } catch (Exception e) {
                logger.errorf(e, "[handleJARMResponse] Failed to process JARM response: %s", e.getMessage());
                event.event(EventType.IDENTITY_PROVIDER_LOGIN);
                event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
                return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
        }

        @Override
        public SimpleHttp generateTokenRequest(String authorizationCode) {
            logger.infof("[generateTokenRequest] Generating FAPI 2.0 token request: %s", authorizationCode);
            var simpleHttp = FuncUtil.invoke(() -> {
                var context = session.getContext();
                var providerConfig = provider.getConfig();
                var redirectUri = Urls.identityProviderAuthnResponse(
                        context.getUri().getBaseUri(),
                        providerConfig.getAlias(),
                        context.getRealm().getName()).toString();
                var tokenRequest = SimpleHttp.doPost(providerConfig.getTokenUrl(), session)
                        .param(OAUTH2_PARAMETER_CODE, authorizationCode)
                        .param(OAUTH2_PARAMETER_REDIRECT_URI, redirectUri)
                        .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE);

                if (providerConfig.isPkceEnabled()) {
                    // reconstruct the original code verifier that was used to generate the code challenge from the HttpRequest.
                    var stateParam = session.getContext()
                            .getUri()
                            .getQueryParameters()
                            .getFirst(OAuth2Constants.STATE);
                    if (stateParam == null) {
                        logger.warn("Cannot lookup PKCE code_verifier: state param is missing.");
                        return tokenRequest;
                    }

                    var realm = context.getRealm();
                    var idpBrokerState = IdentityBrokerState.encoded(stateParam, realm);
                    var client = realm.getClientByClientId(idpBrokerState.getClientId());
                    var authSession = ClientSessionCode.getClientSession(
                            idpBrokerState.getEncoded(),
                            idpBrokerState.getTabId(),
                            session,
                            realm,
                            client,
                            event,
                            AuthenticationSessionModel.class);
                    if (authSession == null) {
                        logger.warnf("Cannot lookup PKCE code_verifier: authSession not found. state=%s", stateParam);
                        return tokenRequest;
                    }

                    var brokerCodeChallenge = authSession.getClientNote("BROKER_CODE_CHALLENGE");
                    if (brokerCodeChallenge == null) {
                        logger.warnf("Cannot lookup PKCE code_verifier: brokerCodeChallenge not found. state=%s", stateParam);
                        return tokenRequest;
                    }

                    tokenRequest.param(OAuth2Constants.CODE_VERIFIER, brokerCodeChallenge);
                    logger.infof("[generateTokenRequest] PKCE code_verifier added to token request");
                }

                return provider.authenticateTokenRequest(tokenRequest);
            });
            logger.infof("[generateTokenRequest] Token request generated for endpoint: %s", simpleHttp.getUrl());
            return simpleHttp;
        }
    }
}
