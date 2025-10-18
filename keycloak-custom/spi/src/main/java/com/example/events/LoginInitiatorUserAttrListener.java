package com.example.events;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

public class LoginInitiatorUserAttrListener implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(LoginInitiatorUserAttrListener.class);
    public static final String ATTR_KEY = "last_client_login_initiation";
    public static final String ATTR_TIME = "last_client_login_initiation_at";

    private final KeycloakSession session;

    public LoginInitiatorUserAttrListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        logger.infof("LoginInitiatorUserAttrListener received event: %s for user ID: %s from client: %s with Event details: %s",
                event.getType(),
                event.getUserId(),
                event.getClientId(),
                event.getDetails());
        var clientId = event.getClientId(); // inisiator login
        if (event.getType() == EventType.LOGIN) {
            logger.infof("Processing LOGIN event for user ID: %s from client: %s",
                    event.getUserId(),
                    event.getClientId());
            if (event.getUserId() == null || event.getUserId().isEmpty()) {
                logger.warn("Event userId is null or empty, skipping processing.");
                return;
            }

            var realm = session.getContext().getRealm();
            var user = session.users().getUserById(realm, event.getUserId());
            if (user != null) {
                logger.infof("Setting user attribute for user ID: %s with clientId: %s",
                        event.getUserId(),
                        clientId);
                user.setSingleAttribute(ATTR_KEY, clientId);
                user.setSingleAttribute(ATTR_TIME, Long.toString(System.currentTimeMillis()));
            } else {
                logger.warnf("User not found for ID: %s", event.getUserId());
            }
        } else if (event.getType() == EventType.CODE_TO_TOKEN) {
            logger.infof("Processing CODE_TO_TOKEN event for user ID: %s from client: %s ",
                    event.getUserId(),
                    event.getClientId());
            var sessionModel = session.sessions()
                    .getUserSession(session.getContext().getRealm(), event.getUserId());
            if (sessionModel != null) {
                logger.infof("Setting session note for user ID: %s, session ID: %s, username: %s",
                        event.getUserId(),
                        sessionModel.getId(),
                        sessionModel.getLoginUsername());
                var already = sessionModel.getNote(ATTR_KEY);
                if (already == null || already.isEmpty()) {
                    logger.info("Setting note for the first time");
                    sessionModel.setNote(ATTR_KEY, clientId);
                    sessionModel.setNote(ATTR_TIME, Long.toString(System.currentTimeMillis()));
                } else {
                    logger.infof("Note already set: %s", already);
                }
            } else {
                logger.warnf("Session model not found for user ID: %s", event.getUserId());
            }
        } else {
            logger.infof("Ignoring event type: %s", event.getType());
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRep) {
        logger.info("Ignoring admin event: " + adminEvent.getOperationType());
    }

    @Override
    public void close() {
        logger.info("LoginInitiatorUserAttrListener closed");
    }
}
