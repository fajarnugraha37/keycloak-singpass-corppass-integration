package com.example.events;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;

public class LoginInitiatorUserAttrListenerFactory implements EventListenerProviderFactory {
    private static final Logger logger = Logger.getLogger(LoginInitiatorUserAttrListenerFactory.class);
    public static final String ID = "login-initiator-user-attr-listener";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        logger.info("LoginInitiatorUserAttrListenerFactory create provider");
        return new LoginInitiatorUserAttrListener(session);
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("LoginInitiatorUserAttrListenerFactory initialized");
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
        logger.info("LoginInitiatorUserAttrListenerFactory post-initialized");
    }

    @Override
    public void close() {
        logger.info("LoginInitiatorUserAttrListenerFactory closed");
    }

    @Override
    public String getId() {
        return ID;
    }
}
