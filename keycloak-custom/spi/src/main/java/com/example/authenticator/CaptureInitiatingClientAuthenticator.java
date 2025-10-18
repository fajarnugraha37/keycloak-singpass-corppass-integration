package com.example.authenticator;

import org.keycloak.authentication.Authenticator;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class CaptureInitiatingClientAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(CaptureInitiatingClientAuthenticator.class);
    public static final String NOTE_KEY = "initiating_client";
    public static final String NOTE_TIME = "initiating_client_at";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var authSession = context.getAuthenticationSession();
        var already = authSession.getUserSessionNotes().get(NOTE_KEY);
        
        logger.info("CaptureInitiatingClientAuthenticator: " + authSession.getClient().getClientId());
        if (already == null || already.isEmpty()) {
            logger.info("Setting note for the first time");
            var clientId = authSession.getClient().getClientId();
            var now = System.currentTimeMillis();
            
            // selalu update tiap inisiasi login
            authSession.setUserSessionNote(NOTE_KEY, clientId);
            authSession.setUserSessionNote(NOTE_TIME, Long.toString(now));
        } else {
            logger.info("Note already set: " + already);
        }

        context.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("CaptureInitiatingClientAuthenticator action"); 
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(org.keycloak.models.KeycloakSession s, org.keycloak.models.RealmModel r, UserModel u) {
        return true;
    }

    @Override
    public void close() {
        logger.info("CaptureInitiatingClientAuthenticator closed");
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.info("CaptureInitiatingClientAuthenticator setRequiredActions");
    }
}
