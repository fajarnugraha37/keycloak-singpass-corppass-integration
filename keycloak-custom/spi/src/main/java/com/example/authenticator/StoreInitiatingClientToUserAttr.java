package com.example.authenticator;

import org.keycloak.authentication.Authenticator;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class StoreInitiatingClientToUserAttr implements Authenticator {
    private static final Logger logger = Logger.getLogger(StoreInitiatingClientToUserAttr.class);
    public static final String ATTR_KEY = "last_client_login_initiation";
    public static final String ATTR_TIME = "last_client_login_initiation_at";

    @Override
    public void authenticate(AuthenticationFlowContext ctx) {
        var isError = false;
        try {
            var authSession = ctx.getAuthenticationSession();
            var user = ctx.getUser();
            var already = authSession.getUserSessionNotes().get(CaptureInitiatingClientAuthenticator.NOTE_KEY);

            logger.info("StoreInitiatingClientToUserAttr authenticate");
            if (user != null && (already == null || already.isEmpty())) {
                logger.info("StoreInitiatingClientToUserAttr: user=" + user.getUsername() + ", client="
                        + authSession.getClient().getClientId());
                var clientId = authSession.getClient().getClientId();
                var now = System.currentTimeMillis();

                // selalu update tiap inisiasi login
                user.setSingleAttribute(ATTR_KEY, clientId);
                user.setSingleAttribute(ATTR_TIME, Long.toString(now));
            } else {
                logger.warn("StoreInitiatingClientToUserAttr: user is null, cannot set attribute: "
                        + (user == null ? "null" : user.getUsername()) + ", already=" + already);
            }
        } catch (Exception ex) {
            logger.error("Error in StoreInitiatingClientToUserAttr: " + ex.getMessage(), ex);
            isError = true;
        } finally {
            if (isError) {
                ctx.failure(AuthenticationFlowError.INTERNAL_ERROR);
            } else {
                ctx.success();
            }
        }
    }

    @Override
    public void action(AuthenticationFlowContext ctx) {
        logger.info("StoreInitiatingClientToUserAttr action");
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(org.keycloak.models.KeycloakSession s, org.keycloak.models.RealmModel r, UserModel u) {
        return true;
    }

    @Override
    public void close() {
        logger.info("StoreInitiatingClientToUserAttr closed");
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.info("StoreInitiatingClientToUserAttr setRequiredActions");
    }
}
