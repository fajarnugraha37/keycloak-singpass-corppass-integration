package com.example.events;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

public class LoginInitiatorUserAttrListener implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(LoginInitiatorUserAttrListener.class);
    public static final String ATTR_KEY = "last_client_login_initiation";
    public static final String ATTR_TIME = "last_client_login_initiation_at";
    public static final String NOTE_KEY = "initiating-client";

    @SuppressWarnings("unused")
    private final KeycloakSession session;

    public LoginInitiatorUserAttrListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        logger.info("Received event: " + event.getType() + " for user ID: " + event.getUserId() + " from client: "
                + event.getClientId());
        // var clientId = event.getClientId(); // inisiator login
        // if (event.getType() == EventType.LOGIN) {
        //     logger.info("Processing LOGIN event for user ID: " + event.getUserId() + " from client: "+ event.getClientId());
        //     if (event.getUserId() == null || event.getUserId().isEmpty()) {
        //         logger.warn("Event userId is null or empty, skipping processing.");
        //         return;
        //     }

        //     var realm = session.getContext().getRealm();
        //     var user = session.users().getUserById(realm, event.getUserId());
        //     if (user != null) {
        //         logger.info("Setting user attribute for user: " + user.getUsername() + " with clientId: " + clientId);
        //         user.setSingleAttribute(ATTR_KEY, clientId);
        //         user.setSingleAttribute(ATTR_TIME, Long.toString(System.currentTimeMillis()));
        //     } else {
        //         logger.warn("User not found for ID: " + event.getUserId());
        //     }
        // } else if (event.getType() == EventType.CODE_TO_TOKEN) {
        //     logger.info("Processing CODE_TO_TOKEN event for user ID: " + event.getUserId() + " from client: "
        //             + event.getClientId());
        //     // var sessionModel = session.sessions().getUserSession(session.getContext().getRealm(), event.getUserId());
        //     // if (sessionModel != null) {
        //     //     logger.warn("Session model not found for user ID: " + event.getUserId());
        //     //     logger.warn("Session ID from event: " + sessionModel.getId());
        //     //     logger.warn("Username from event: " + sessionModel.getLoginUsername());
        //     //     var already = sessionModel.getNote(NOTE_KEY);
        //     //     if (already == null || already.isEmpty()) {
        //     //         logger.info("Setting note for the first time");
        //     //         sessionModel.setNote(NOTE_KEY, clientId);
        //     //     } else {
        //     //         logger.info("Note already set: " + already);
        //     //     }
        //     //     return;
        //     // } else {
        //     //     logger.warn("Session model not found for user ID: " + event.getUserId());
        //     // }
        // } else {
        //     logger.info("Ignoring event type: " + event.getType());
        // }
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
