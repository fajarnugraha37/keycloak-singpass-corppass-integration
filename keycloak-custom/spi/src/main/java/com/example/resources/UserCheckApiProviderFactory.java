package com.example.resources;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class UserCheckApiProviderFactory extends AlternateApiProviderFactory {

    public static final String ID = "user-check";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new UserCheckApiProvider();
    }

    @Override
    public String getId() {
        return ID;
    }
}