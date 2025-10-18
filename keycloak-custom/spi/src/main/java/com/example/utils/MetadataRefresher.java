package com.example.utils;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.models.KeycloakSession;

public class MetadataRefresher {
    private static final Logger LOG = Logger.getLogger(MetadataRefresher.class);

    private volatile long lastFetch = 0;

    public void refreshIfNeeded(KeycloakSession session, OIDCIdentityProvider idp, long ttlMillis) {
//        var now = System.currentTimeMillis();
//        if (now - lastFetch < ttlMillis)
//            return;
//        synchronized (this) {
//            if (now - lastFetch < ttlMillis)
//                return;
//            try {
//                // touch the provider metadata; OIDCIdentityProvider will (re)load as needed
//                idp.updateMetadata(session);
//                lastFetch = System.currentTimeMillis();
//                LOG.debug("singpass idp metadata refreshed");
//            } catch (Exception e) {
//                LOG.warn("failed to refresh singpass idp metadata", e);
//            }
//        }
    }
}
