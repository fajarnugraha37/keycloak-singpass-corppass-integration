package com.example.identity.endpoints;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;


public class CustomSAMLEndpoint extends SAMLEndpoint {
    private static final Logger logger = Logger.getLogger(CustomSAMLEndpoint.class);

    protected final KeycloakSession session;
    protected final CustomSAMLProvider provider;
    protected final CustomSAMLIdentityProviderConfig config;
    protected final DestinationValidator destinationValidator;
    protected final ClientConnection clientConnection;
    protected final HttpHeaders headers;


    public CustomSAMLEndpoint(KeycloakSession session,
                              CustomSAMLProvider provider,
                              CustomSAMLIdentityProviderConfig config,
                              DestinationValidator destinationValidator,
                              IdentityProvider.AuthenticationCallback callback) {
        super(session, provider, config, callback, destinationValidator);
        this.session = session;
        this.provider = provider;
        this.config = config;
        this.clientConnection = session.getContext().getConnection();
        this.destinationValidator = destinationValidator;
        this.headers = session.getContext().getRequestHeaders();
    }

    @Override
    public Response getSPDescriptor() {
        logger.infof("Generating SP Descriptor for CustomSAMLProvider");
        var response = super.getSPDescriptor();
        logger.infof("SP Descriptor Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    @Override
    public Response redirectBinding(String samlRequest,
                                    String samlResponse,
                                    String relayState) {
        return this.redirectBinding(samlRequest, samlResponse, relayState, null);
    }

    @Override
    public Response redirectBinding(String samlRequest,
                                    String samlResponse,
                                    String relayState,
                                    String clientId) {
        logger.infof("Handling SAML Redirect Binding for clientId: %s", clientId);
        logger.infof("SAML Request: %s", samlRequest);
        logger.infof("SAML Response: %s", samlResponse);
        logger.infof("Relay State: %s", relayState);
        var binding = new CustomSAMLRedirectBinding(realm, config, session, clientConnection, destinationValidator, headers, callback, provider, event);
        var response = binding.execute(samlRequest, samlResponse, relayState, clientId);
        logger.infof("Redirect Binding Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    @Override
    public Response postBinding(String samlRequest,
                                String samlResponse,
                                String relayState) {
        return this.postBinding(samlRequest, samlResponse, relayState, null);
    }

    @Override
    public Response postBinding(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
        logger.infof("Handling SAML Post Binding for clientId: %s", clientId);
        logger.infof("SAML Request: %s", samlRequest);
        logger.infof("SAML Response: %s", samlResponse);
        logger.infof("Relay State: %s", relayState);
        var binding = new CustomSAMLPostBinding(realm, config, session, clientConnection, destinationValidator, headers, callback, provider, event);
        var response = binding.execute(samlRequest, samlResponse, relayState, clientId);
        logger.infof("Post Binding Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }
}