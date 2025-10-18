package com.example.resources;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resource.RealmResourceProvider;

public class SharedKeyApiProviderFactory extends AlternateApiProviderFactory {

    public static final String ID = "shared-key";

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return new SharedKeyApiProvider(new RealmManager(keycloakSession).getRealmByName("master"));
    }

    @Override
    public String getId() {
        return ID;
    }
}