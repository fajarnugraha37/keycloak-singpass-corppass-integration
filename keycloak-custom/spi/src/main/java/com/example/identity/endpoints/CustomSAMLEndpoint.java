package com.example.identity.endpoints;

import com.example.identity.CustomSAMLProvider;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.events.EventBuilder;
import org.slf4j.LoggerFactory;


public class CustomSAMLEndpoint extends SAMLEndpoint {
    private static final Logger logger = Logger.getLogger(CustomSAMLEndpoint.class);
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CustomSAMLEndpoint.class);

    private final SAMLRedirectBinding redirectBinding;
    private final SAMLPostBinding postBinding;

    public CustomSAMLEndpoint(CustomSAMLProvider provider,
                              IdentityProvider.AuthenticationCallback callback) {
        super(provider.session, provider, provider.config, callback, provider.destinationValidator);
        this.redirectBinding = new SAMLRedirectBinding(provider, callback);
        this.postBinding = new SAMLPostBinding(provider, callback);
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
        var response = redirectBinding.execute(samlRequest, samlResponse, relayState, clientId);
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
        var response = postBinding.execute(samlRequest, samlResponse, relayState, clientId);
        logger.infof("Post Binding Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    public class SAMLPostBinding extends PostBinding {
        private final CustomSAMLProvider provider;
        private final IdentityProvider.AuthenticationCallback callback;

        public SAMLPostBinding(CustomSAMLProvider provider,
                               IdentityProvider.AuthenticationCallback callback) {
            this.provider = provider;
            this.callback = callback;
        }

        @Override
        public Response execute(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
            logger.infof("Executing SAML Post Binding for clientId: %s with relyState", clientId, relayState);
            event = new EventBuilder(realm, provider.session, provider.clientConnection);
            var response = basicChecks(samlRequest, samlResponse);
            if (response != null) {
                logger.infof("Basic checks failed. Returning response with status %s", response.getStatus());
                return response;
            }
            if (samlRequest != null) {
                logger.infof("Handling SAML Request in Post Binding");
                return handleSamlRequest(samlRequest, relayState);
            }

            log.info("Handling SAML Response in Post Binding");
            return handleSamlResponse(samlResponse, relayState, clientId);
        }
    }

    public class SAMLRedirectBinding extends RedirectBinding {
        private final CustomSAMLProvider provider;
        private final IdentityProvider.AuthenticationCallback callback;

        public SAMLRedirectBinding(CustomSAMLProvider provider,
                                   IdentityProvider.AuthenticationCallback callback) {
            this.provider = provider;
            this.callback = callback;
        }

        @Override
        public Response execute(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
            logger.infof("Executing SAML Redirect Binding for clientId: %s with relyState %s", clientId, relayState);
            event = new EventBuilder(realm, provider.session, provider.clientConnection);
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