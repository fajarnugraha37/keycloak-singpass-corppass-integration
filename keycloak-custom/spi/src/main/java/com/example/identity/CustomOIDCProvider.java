package com.example.identity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import com.example.utils.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.*;
import liquibase.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.*;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.*;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.example.config.CustomOIDCIdentityProviderConfig;
import com.nimbusds.jose.JWSAlgorithm;

import org.keycloak.util.JsonSerialization;

import static org.keycloak.utils.MediaType.APPLICATION_JSON_TYPE;

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
public class CustomOIDCProvider extends CustomDuplicator {
    private static final Logger logger = Logger.getLogger(CustomOIDCProvider.class);

    public final CustomOIDCIdentityProviderConfig configuration;

    public CustomOIDCProvider(KeycloakSession session, CustomOIDCIdentityProviderConfig config) {
        super(session, config);
        this.configuration = config;
    }

    /**
     * GET /realms/<realm-name>/broker/custom-oidc/login flow:
     * 1. performLogin() - Initiates login by redirecting to the IdP's authorization endpoint.
     * 2. callback() - Creates an endpoint to handle the IdP's response.
     * 3. CustomOIDCEndpoint.authResponse() - Processes the authorization code returned by the IdP.
     * 4. CustomOIDCEndpoint.generateTokenRequest() - Generates the token exchange request.
     * 5. authenticateTokenRequest() - Adds client authentication to the token request.
     * 6. getFederatedIdentity() - Processes the token response and creates the federated identity.
     * 7. extractIdentity() - Maps tokens to user attributes.
     * 8. preprocessFederatedIdentity() - Pre-processes the federated identity before user import/update.
     * 9. importNewUser() OR updateBrokeredUser() - Imports or updates the user in Keycloak.
     * 10. authenticationFinished() - Finalizes the authentication process.
     * </p>
     * let say user login for the 2nd time, there is a changes in id token claim or userinfo, the flow will be:
     * 1. performLogin()
     * 2. callback()
     * 3. CustomOIDCEndpoint.authResponse()
     * 4. CustomOIDCEndpoint.generateTokenRequest()
     * 5. authenticateTokenRequest()
     * 6. getFederatedIdentity()
     * 7. extractIdentity()
     * 8. preprocessFederatedIdentity()
     * 9. updateBrokeredUser()
     * 10. authenticationFinished()
     * </p>
     * which Part I need to customize if I want to upadate user attribute when there is a change in id token claim or userinfo?
     * 1. extractIdentity() - This method is responsible for mapping tokens to user attributes.
     * You can customize this method to ensure that it correctly identifies changes in the ID token claims or userinfo and maps them to the appropriate user attributes in Keycloak.
     * 2. preprocessFederatedIdentity() - This method allows you to pre-process the federated identity before the user is imported or updated in Keycloak.
     * 3. updateBrokeredUser() - This method is called when an existing user is being updated.
     * You can customize this method to implement logic that checks for changes in user attributes and updates them accordingly in Keycloak.
     */

    @Override
    public boolean supportsLongStateParameter() {
        logger.info("[supportsLongStateParameter] Checking if long state parameter is supported in CustomOIDCProvider");
        return super.supportsLongStateParameter();
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        var redirectUri = request.getRedirectUri();
        var realmId = request.getRealm().getId();
        var clientId = request.getState().getClientId();
        var authSession = request.getAuthenticationSession();
        var authSessionId = authSession.getTabId();
        logger.infof("[performLogin] Login details - Realm ID: %s, Client ID: %s, Auth Session ID: %s, Redirect URI: %s, State: %s",
                realmId,
                clientId,
                authSessionId,
                redirectUri,
                request.getState());

        var httpMethod = request.getHttpRequest().getHttpMethod();
        var httpPath = request.getHttpRequest().getUri().getPath();
        var httpPathParameters = request.getHttpRequest().getUri().getPathParameters();
        var httpQueryParameters = request.getHttpRequest().getUri().getQueryParameters();
        var httpHeaders = request.getHttpRequest().getHttpHeaders().getRequestHeaders();
        logger.infof("[performLogin] HTTP Request ---> %s %s, \n\tPath Parameters: %s, \n\tQuery Parameters: %s, \n\tHeaders: %s",
                httpMethod,
                httpPath,
                httpPathParameters,
                httpQueryParameters,
                httpHeaders);
        var response = super.performLogin(request);
        logger.infof("[performLogin] HTTP Response <--- %s %s \n\t%s", response.getStatus(), response.getLocation(), response.getEntity());
        return response;
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        logger.infof("[createAuthorizationUrl] Creating authorization URL in CustomOIDCProvider: %s %s %s", request.getRealm().getName(), request.getRedirectUri(), request.getState());
        return super.createAuthorizationUrl(request);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new CustomOIDCEndpoint(callback, realm, event, this);
    }

    @Override
    public SimpleHttp authenticateTokenRequest(SimpleHttp tokenRequest) {
        logger.infof("[authenticateTokenRequest] Authenticating token request in CustomOIDCProvider");
        if (getConfig().isJWTAuthentication()) {
            String jws = null;
            if (configuration.getSigningKeyId() == null || configuration.getSigningKeyId().isEmpty()) {
                jws = new JWSBuilder().type(OAuth2Constants.JWT).jsonContent(generateToken()).sign(getSignatureContext());
            } else {
                var keyWrapper = session.keys().getKeysStream(session.getContext().getRealm()).filter(key -> key.getKid().equalsIgnoreCase(this.configuration.getSigningKeyId())).findFirst();
                if (keyWrapper.isPresent()) {
                    var key = keyWrapper.get();
                    var algorithm = JWSAlgorithm.parse(key.getAlgorithm());
                    logger.infof("[authenticateTokenRequest] Signing key id:", this.configuration.getSigningKeyId());
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
            return tokenRequest.param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId()).param(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT).param(OAuth2Constants.CLIENT_ASSERTION, jws);
        }

        try (var vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            if (getConfig().isBasicAuthentication()) {
                logger.infof("[authenticateTokenRequest] Using basic authentication for client ID: %s", getConfig().getClientId());
                logger.infof("[authenticateTokenRequest] Client Secret from Vault: %s", vaultStringSecret.get().orElse("Not found in vault"));
                return tokenRequest.authBasic(getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()));
            }

            logger.infof("[authenticateTokenRequest] Using form authentication for client ID: %s", getConfig().getClientId());
            logger.infof("[authenticateTokenRequest] Client Secret from Vault: %s", vaultStringSecret.get().orElse("Not found in vault"));
            return tokenRequest.param(OAUTH2_PARAMETER_CLIENT_ID,
                    getConfig().getClientId()).param(OAUTH2_PARAMETER_CLIENT_SECRET,
                    vaultStringSecret.get().orElse(getConfig().getClientSecret()));
        }
    }

    @Override
    protected void processAccessTokenResponse(BrokeredIdentityContext context, AccessTokenResponse response) {
        var isJwt = JwtUtil.isJwt(response.getToken());
        logger.infof("[processAccessTokenResponse] Process Access Token is jwt = %s %s %s %s", isJwt, response.getToken(), response.getTokenType(), response.toString());
        if (isJwt && getConfig().isAccessTokenJwt()) {
            logger.infof("[processAccessTokenResponse] Access token is JWT, validating token");
            var access = validateToken(response.getToken(), true);
            context.getContextData().put(VALIDATED_ACCESS_TOKEN, access);
        } else {
            logger.infof("[processAccessTokenResponse] Access token is not JWT, skipping validation");
        }
    }

    @Override
    protected String parseTokenInput(String encodedToken, boolean shouldBeSigned) {
        return JweUtil.parse(session, getConfig(), encodedToken, shouldBeSigned);
    }

    @Override
    protected JsonWebToken validateToken(String encodedToken, boolean ignoreAudience) {
        logger.infof("[validateToken] Validating token in CustomOIDCProvider with ignoreAudience=%s: %s", ignoreAudience, encodedToken);
        return JwtUtil.validateToken(getConfig(), encodedToken, ignoreAudience, this::parseTokenInput);
    }

    @Override
    public JsonWebToken validateToken(String encodedToken) {
        logger.infof("[validateToken] Validating token in CustomOIDCProvider: %s", encodedToken);
        return super.validateToken(encodedToken);
    }

    @Override
    protected boolean verify(JWSInput jws) {
        return JweUtil.verify(session, getConfig(), jws);
    }

    @Override
    protected String getusernameClaimNameForIdToken() {
        logger.info("[getusernameClaimNameForIdToken] Getting username claim name for ID token in CustomOIDCProvider");
        return super.getusernameClaimNameForIdToken();
    }

    @Override
    protected String getUsernameFromUserInfo(JsonNode userInfo) {
        logger.infof("[getUsernameFromUserInfo] Getting username from user info in CustomOIDCProvider: %s", userInfo.asText());
        var usernameNode = userInfo.path("sub");
        if (usernameNode.isMissingNode() || usernameNode.isNull()) {
            usernameNode = userInfo.path("uinfin").path("value");
        }

        return usernameNode.asText();
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        logger.infof("[getFederatedIdentity] Getting federated identity in CustomOIDCProvider: %s", response);

        var tokenResponse = JwtUtil.decryptAccessTokenResponse(response, session, this.configuration.isEncryptedIdTokenFlag());
        var accessToken = JwtUtil.verifyAccessToken(tokenResponse);
        var encodedIdToken = tokenResponse.getIdToken();
        logger.infof("[getFederatedIdentity] Access Token: %s; EncodedIdToken: %s;", accessToken, encodedIdToken);

        try {
            var idToken = this.validateToken(encodedIdToken);
            var identity = this.extractIdentity(tokenResponse, accessToken, idToken);
            var nonce = idToken.getOtherClaims().get("nonce");
            logger.infof("[getFederatedIdentity] Extracted identity: %s with nonce: %s", identity.getUsername(), nonce);
            identity.getContextData().put("BROKER_NONCE", nonce);
            if (this.getConfig().isStoreToken()) {
                if (tokenResponse.getExpiresIn() > 0L) {
                    var accessTokenExpiration = (long) Time.currentTime() + tokenResponse.getExpiresIn();
                    tokenResponse.getOtherClaims().put("accessTokenExpiration", accessTokenExpiration);
                    response = JsonSerialization.writeValueAsString(tokenResponse);
                    logger.infof("[getFederatedIdentity] Access token expiration set to: %s and response: %s", accessTokenExpiration, response);
                } else {
                    logger.infof("[getFederatedIdentity] Token expiration not set in response.");
                }
                identity.setToken(response);
            } else {
                logger.infof("[getFederatedIdentity] Not storing token as per configuration.");
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
                    .header("Authorization", "Bearer " + accessToken);
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

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        logger.infof("[preprocessFederatedIdentity] Preprocessing federated identity in CustomOIDCProvider: %s", context.getUsername());
        super.preprocessFederatedIdentity(session, realm, context);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        logger.infof("[importNewUser] Importing new user in CustomOIDCProvider: %s", user.getUsername());
        super.importNewUser(session, realm, user, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session,
                                   RealmModel realm,
                                   UserModel user,
                                   BrokeredIdentityContext context) {
        logger.infof("[updateBrokeredUser] Updating brokered user in CustomOIDCProvider: %s", user.getUsername());
        for (var entry : context.getAttributes().entrySet()) {
            var attrKey = entry.getKey();
            var attrValues = entry.getValue();
            var existingValues = user.getFirstAttribute(attrKey);
            var isNeedToUpdate = existingValues == null
                    || existingValues.isEmpty()
                    || !StringUtils.equalsAny(existingValues, attrValues.toArray(CharSequence[]::new));
            if (isNeedToUpdate) {
                logger.infof("[updateBrokeredUser] Updating attribute for user %s: %s = %s", user.getUsername(), attrKey, attrValues);
                user.setAttribute(attrKey, attrValues);
            } else {
                logger.infof("[updateBrokeredUser] No change for attribute %s for user %s", attrKey, user.getUsername());
            }
        }
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession,
                                       BrokeredIdentityContext context) {
        logger.infof("[authenticationFinished] Authentication finished in CustomOIDCProvider: %s %s", authSession.getTabId(), context.getUsername());
        super.authenticationFinished(authSession, context);
    }


    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        var profileEndpoint = super.getProfileEndpointForValidation(event);
        logger.infof("[getProfileEndpointForValidation] Getting profile endpoint for validation in CustomOIDCProvider: %s", profileEndpoint);
        return profileEndpoint;
    }

    @Override
    protected BrokeredIdentityContext validateExternalTokenThroughUserInfo(EventBuilder event,
                                                                           String subjectToken,
                                                                           String subjectTokenType) {
        logger.infof("[validateExternalTokenThroughUserInfo] Validating external token through user info in CustomOIDCProvider: %s, %s", subjectTokenType, subjectToken);
        return super.validateExternalTokenThroughUserInfo(event, subjectToken, subjectTokenType);
    }

    @Override
    protected BrokeredIdentityContext exchangeExternalUserInfoValidationOnly(EventBuilder event,
                                                                             MultivaluedMap<String, String> params) {
        logger.infof("[exchangeExternalUserInfoValidationOnly] Exchanging external user info validation only in CustomOIDCProvider with params: %s", params);
        return super.exchangeExternalUserInfoValidationOnly(event, params);
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event,
                                                                 JsonNode userInfo) {
        logger.infof("[extractIdentityFromProfile] Extracting identity from profile in CustomOIDCProvider: %s", userInfo);
        return super.extractIdentityFromProfile(event, userInfo);
    }

    @Override
    protected Response exchangeErrorResponse(UriInfo uriInfo,
                                             ClientModel authorizedClient,
                                             UserSessionModel tokenUserSession,
                                             String errorCode,
                                             String reason) {
        logger.infof("[exchangeErrorResponse] Exchanging error response in CustomOIDCProvider: %s, %s", errorCode, reason);
        return super.exchangeErrorResponse(uriInfo, authorizedClient, tokenUserSession, errorCode, reason);
    }

    @Override
    protected Response exchangeStoredToken(UriInfo uriInfo,
                                           EventBuilder event,
                                           ClientModel authorizedClient,
                                           UserSessionModel tokenUserSession,
                                           UserModel tokenSubject) {
        logger.infof("[exchangeStoredToken] Exchanging stored token in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        return super.exchangeStoredToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject);
    }

    @Override
    public void exchangeExternalComplete(UserSessionModel userSession,
                                         BrokeredIdentityContext context,
                                         MultivaluedMap<String, String> params) {
        logger.infof("[exchangeExternalComplete] Exchange external complete in CustomOIDCProvider for user session: %s", userSession.getId());
        params.forEach((key, value) -> logger.infof("[exchangeExternalComplete] Param: %s = %s", key, value));
        super.exchangeExternalComplete(userSession, context, params);
    }

    @Override
    public Response retrieveToken(KeycloakSession session,
                                  FederatedIdentityModel identity) {
        logger.infof("[retrieveToken] Retrieving token in CustomOIDCProvider for identity: %s", identity.getUserId());
        return super.retrieveToken(session, identity);
    }

    @Override
    protected SimpleHttp buildUserInfoRequest(String subjectToken,
                                              String userInfoUrl) {
        logger.infof("[buildUserInfoRequest] Building user info request in CustomOIDCProvider: %s %s", userInfoUrl, subjectToken);
        return super.buildUserInfoRequest(subjectToken, userInfoUrl);
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        logger.infof("[doGetFederatedIdentity] Getting federated identity in CustomOIDCProvider: %s", accessToken);
        return super.doGetFederatedIdentity(accessToken);
    }

    @Override
    protected String extractTokenFromResponse(String response, String tokenName) {
        logger.infof("[extractTokenFromResponse] Extracting token from response in CustomOIDCProvider: %s %s", tokenName, response);
        return super.extractTokenFromResponse(response, tokenName);
    }

    @Override
    protected boolean isAuthTimeExpired(JsonWebToken idToken,
                                        AuthenticationSessionModel authSession) {
        logger.infof("[isAuthTimeExpired] Checking if auth time is expired in CustomOIDCProvider for auth session: %s", authSession.getTabId());
        authSession.getClientNotes().forEach((key, value) -> logger.infof("[isAuthTimeExpired] Client note: %s = %s", key, value));
        authSession.getUserSessionNotes().forEach((key, value) -> logger.infof("[isAuthTimeExpired] User session note: %s = %s", key, value));
        return super.isAuthTimeExpired(idToken, authSession);
    }

    @Override
    protected SimpleHttp getRefreshTokenRequest(KeycloakSession session,
                                                String refreshToken,
                                                String clientId,
                                                String clientSecret) {
        logger.infof("[getRefreshTokenRequest] Getting refresh token request in CustomOIDCProvider for client ID: %s", clientId);
        return super.getRefreshTokenRequest(session, refreshToken, clientId, clientSecret);
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session,
                                                   UserSessionModel userSession,
                                                   UriInfo uriInfo,
                                                   RealmModel realm) {
        logger.infof("[keycloakInitiatedBrowserLogout] Keycloak initiated browser logout in CustomOIDCProvider for user session: %s", userSession.getId());
        return super.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
    }

    @Override
    protected void backchannelLogout(UserSessionModel userSession,
                                     String idToken) {
        logger.infof("[backchannelLogout] Backchannel logout in CustomOIDCProvider for user session: %s", userSession.getId());
        super.backchannelLogout(userSession, idToken);
    }

    @Override
    public void backchannelLogout(KeycloakSession session,
                                  UserSessionModel userSession,
                                  UriInfo uriInfo,
                                  RealmModel realm) {
        logger.infof("[backchannelLogout] Backchannel logout in CustomOIDCProvider for user session: %s", userSession.getId());
        super.backchannelLogout(session, userSession, uriInfo, realm);
    }

    @Override
    public String refreshTokenForLogout(KeycloakSession session,
                                        UserSessionModel userSession) {
        logger.infof("[refreshTokenForLogout] Refreshing token for logout in CustomOIDCProvider for user session: %s", userSession.getId());
        return super.refreshTokenForLogout(session, userSession);
    }

    @Override
    public Response exchangeTokenExpired(UriInfo uriInfo,
                                         ClientModel authorizedClient,
                                         UserSessionModel tokenUserSession,
                                         UserModel tokenSubject) {
        logger.infof("[exchangeTokenExpired] Exchanging token expired in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        return super.exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
    }

    @Override
    protected BrokeredIdentityContext exchangeExternalImpl(EventBuilder event,
                                                           MultivaluedMap<String, String> params) {
        logger.infof("[exchangeExternalImpl] Exchanging external impl in CustomOIDCProvider with params: %s", params);
        return super.exchangeExternalImpl(event, params);
    }

    @Override
    public Response exchangeFromToken(UriInfo uriInfo,
                                      EventBuilder event,
                                      ClientModel authorizedClient,
                                      UserSessionModel tokenUserSession,
                                      UserModel tokenSubject,
                                      MultivaluedMap<String, String> params) {
        logger.infof("[exchangeFromToken] Exchanging from token in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        params.forEach((key, value) -> logger.infof("[exchangeFromToken] Param: %s = %s", key, value));
        return super.exchangeFromToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject, params);
    }

    @Override
    protected Response exchangeSessionToken(UriInfo uriInfo,
                                            EventBuilder event,
                                            ClientModel authorizedClient,
                                            UserSessionModel tokenUserSession,
                                            UserModel tokenSubject) {
        logger.infof("[exchangeSessionToken] Exchanging session token in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        return super.exchangeSessionToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject);
    }

    @Override
    public Response exchangeNotSupported() {
        logger.info("[exchangeNotSupported] Exchanging not supported in CustomOIDCProvider");
        return super.exchangeNotSupported();
    }

    @Override
    public Response exchangeNotLinked(UriInfo uriInfo,
                                      ClientModel authorizedClient,
                                      UserSessionModel tokenUserSession,
                                      UserModel tokenSubject) {
        logger.infof("[exchangeNotLinked] Exchanging not linked in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        return super.exchangeNotLinked(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
    }

    @Override
    public Response exchangeNotLinkedNoStore(UriInfo uriInfo,
                                             ClientModel authorizedClient,
                                             UserSessionModel tokenUserSession,
                                             UserModel tokenSubject) {
        logger.infof("[exchangeNotLinkedNoStore] Exchanging not linked no store in CustomOIDCProvider for user: %s", tokenSubject.getUsername());
        return super.exchangeNotLinkedNoStore(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
    }

    @Override
    public Response exchangeUnsupportedRequiredType() {
        logger.info("[exchangeUnsupportedRequiredType] Exchanging unsupported required type in CustomOIDCProvider");
        return super.exchangeUnsupportedRequiredType();
    }

    @Override
    protected boolean supportsExternalExchange() {
        logger.info("[supportsExternalExchange] Checking if external exchange is supported in CustomOIDCProvider");
        return super.supportsExternalExchange();
    }

    @Override
    protected JsonWebToken generateToken() {
        logger.info("[generateToken] Generating token in CustomOIDCProvider");
        return super.generateToken();
    }

    @Override
    public boolean isIssuer(String issuer, MultivaluedMap<String, String> params) {
        logger.infof("[isIssuer] Checking if issuer is valid in CustomOIDCProvider: %s", issuer);
        params.forEach((key, value) -> logger.infof("[isIssuer] Param: %s = %s", key, value));
        return super.isIssuer(issuer, params);
    }

    @Override
    public boolean isMapperSupported(IdentityProviderMapper mapper) {
        logger.infof("[isMapperSupported] Checking if mapper is supported in CustomOIDCProvider: %s", mapper.getId());
        return super.isMapperSupported(mapper);
    }

    @Override
    public boolean reloadKeys() {
        logger.info("[reloadKeys] Reloading keys in CustomOIDCProvider");
        return super.reloadKeys();
    }

    @Override
    protected String getAccessTokenResponseParameter() {
        logger.info("[getAccessTokenResponseParameter] Getting access token response parameter in CustomOIDCProvider");
        return super.getAccessTokenResponseParameter();
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        logger.infof("[export] Exporting in CustomOIDCProvider: %s %s", realm.getName(), format);
        return super.export(uriInfo, realm, format);
    }

    @Override
    protected Response hasExternalExchangeToken(EventBuilder event,
                                                UserSessionModel tokenUserSession,
                                                MultivaluedMap<String, String> params) {
        logger.infof("[hasExternalExchangeToken] Checking for external exchange token in CustomOIDCProvider for user session: %s", tokenUserSession.getId());
        params.forEach((key, value) -> logger.infof("[hasExternalExchangeToken] Param: %s = %s", key, value));
        return super.hasExternalExchangeToken(event, tokenUserSession, params);
    }

    @Override
    public void close() {
        logger.info("[close] Closing CustomOIDCProvider");
        super.close();
    }

    protected static class CustomOIDCEndpoint extends OIDCEndpoint {

        public CustomOIDCEndpoint(AuthenticationCallback callback,
                                  RealmModel realm,
                                  EventBuilder event,
                                  OIDCIdentityProvider provider) {
            super(callback, realm, event, provider);
        }

        @Override
        public Response authResponse(String state, String authorizationCode, String error, String errorDescription) {
            logger.infof("[authResponse] Auth response in CustomOIDCEndpoint: %s %s %s %s", state, authorizationCode, error, errorDescription);
            var response = super.authResponse(state, authorizationCode, error, errorDescription);
            logger.infof("[authResponse] Auth response generated in CustomOIDCEndpoint: %s %s %s", response.getStatus(), response.getLocation(), response.getEntity());
            return response;
        }

        @Override
        public SimpleHttp generateTokenRequest(String authorizationCode) {
            try {
                logger.infof("[generateTokenRequest] Generating token request in CustomOIDCEndpoint: %s", authorizationCode);
                var response = super.generateTokenRequest(authorizationCode);
                logger.infof("[generateTokenRequest] Token request generated in CustomOIDCEndpoint: %s", response.getUrl(), response.asString());
                return response;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Response logoutResponse(String state) {
            logger.infof("[logoutResponse] Logout response in CustomOIDCEndpoint: %s", state);
            var response = super.logoutResponse(state);
            logger.infof("[logoutResponse] Logout response generated in CustomOIDCEndpoint: %s %s %s", response.getStatus(), response.getLocation(), response.getEntity());
            return response;
        }
    }
}
