package com.example.identity.endpoints;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;


public class CustomSAMLEndpoint extends SAMLEndpoint {
    private static final Logger logger = Logger.getLogger(CustomSAMLEndpoint.class);

    private final ClientConnection clientConnection;
    private final KeycloakSession session;

    public CustomSAMLEndpoint(KeycloakSession session,
                              CustomSAMLProvider provider,
                              CustomSAMLIdentityProviderConfig config,
                              DestinationValidator destinationValidator,
                              IdentityProvider.AuthenticationCallback callback) {
        super(session, provider, config, callback, destinationValidator);
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
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
        var response = new SAMLRedirectBinding().execute(samlRequest, samlResponse, relayState, clientId);
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
        var response = new SAMLPostBinding().execute(samlRequest, samlResponse, relayState, clientId);
        logger.infof("Post Binding Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    public class SAMLPostBinding extends PostBinding {
        private SAMLPostBinding() {
            super();
        }

        @Override
        public Response execute(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
            logger.infof("Executing SAML Post Binding for clientId: %s with relyState", clientId, relayState);
            event = new EventBuilder(realm, session, clientConnection);
            var response = basicChecks(samlRequest, samlResponse);
            if (response != null) {
                logger.infof("Basic checks failed. Returning response with status %s", response.getStatus());
                return response;
            }
            if (samlRequest != null) {
                logger.infof("Handling SAML Request in Post Binding");
                return handleSamlRequest(samlRequest, relayState);
            }

            logger.info("Handling SAML Response in Post Binding");
            return handleSamlResponse(samlResponse, relayState, clientId);
        }
    }

    public class SAMLRedirectBinding extends RedirectBinding {
        private SAMLRedirectBinding() {
            super();
        }

        @Override
        public Response execute(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
            logger.infof("Executing SAML Redirect Binding for clientId: %s with relyState %s", clientId, relayState);
            event = new EventBuilder(realm, session, clientConnection);
            var response = basicChecks(samlRequest, samlResponse);
            if (response != null) {
                logger.infof("Basic checks failed. Returning response with status %s", response.getStatus());
                return response;
            }
            if (samlRequest != null) {
                logger.infof("Handling SAML Request in Redirect Binding");
                return handleSamlRequest(samlRequest, relayState);
            }

            logger.infof("Handling SAML Response in Redirect Binding");
            return handleSamlResponse(samlResponse, relayState, clientId);
        }
    }

}