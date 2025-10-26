package com.example.identity;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.endpoints.CustomSAMLEndpoint;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.sessions.AuthenticationSessionModel;

public class CustomSAMLProvider extends SAMLIdentityProvider {
    private static final Logger logger = Logger.getLogger(CustomSAMLProvider.class);

    private final KeycloakSession session;
    private final CustomSAMLIdentityProviderConfig config;
    private final DestinationValidator destinationValidator;

    public CustomSAMLProvider(KeycloakSession session,
                              CustomSAMLIdentityProviderConfig config,
                              DestinationValidator destinationValidator) {
        super(session, config, destinationValidator);
        this.session = session;
        this.config = config;
        this.destinationValidator = destinationValidator;
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
        logger.infof("[performLogin] HTTP Response <--- %s %s \n\t%s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    @Override
    protected String getLinkingUrl(UriInfo uriInfo,
                                   ClientModel authorizedClient,
                                   UserSessionModel tokenUserSession) {
        logger.infof("Generating linking URL for client: %s, user session: %s",
                authorizedClient.getClientId(),
                tokenUserSession.getId());
        return super.getLinkingUrl(uriInfo, authorizedClient, tokenUserSession);
    }

    @Override
    public Object callback(RealmModel realm,
                           AuthenticationCallback callback,
                           EventBuilder event) {
        logger.infof("CustomSAMLProvider callback invoked for realm: %s", realm.getName());
        return new CustomSAMLEndpoint(session,this, config, destinationValidator, callback);
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        logger.infof("[retrieveToken] Retrieving token for federated identity: %s", identity.getUserId());
        return super.retrieveToken(session, identity);
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session,
                                            RealmModel realm,
                                            BrokeredIdentityContext context) {
        logger.infof("[preprocessFederatedIdentity] Preprocessing federated identity for user: %s", context.getUsername());
        super.preprocessFederatedIdentity(session, realm, context);
    }

    @Override
    public void importNewUser(KeycloakSession session,
                              RealmModel realm,
                              UserModel user,
                              BrokeredIdentityContext context) {
        logger.infof("[importNewUser] Importing new user: %s", user.getUsername());
        super.importNewUser(session, realm, user, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session,
                                   RealmModel realm,
                                   UserModel user,
                                   BrokeredIdentityContext context) {
        logger.infof("[updateBrokeredUser] Updating brokered user: %s", user.getUsername());
        super.updateBrokeredUser(session, realm, user, context);
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        logger.infof("[export] Exporting identity provider data for realm: %s in format: %s", realm.getName(), format);
        return super.export(uriInfo, realm, format);
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        var signatureAlgorithm = super.getSignatureAlgorithm();
        logger.infof("[getSignatureAlgorithm] Using signature algorithm: %s", signatureAlgorithm);
        return signatureAlgorithm;
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession,
                                       BrokeredIdentityContext context) {
        logger.infof("[authenticationFinished] Authentication finished: %s %s", authSession.getTabId(), context.getUsername());
        super.authenticationFinished(authSession, context);
    }
}
