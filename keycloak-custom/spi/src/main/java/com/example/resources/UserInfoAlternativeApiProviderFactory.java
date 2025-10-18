package com.example.resources;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class UserInfoAlternativeApiProviderFactory extends AlternateApiProviderFactory {

    public static final String ID = "user-info";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new UserInfoAlternativeApiProvider();
    }

    @Override
    public String getId() {
        return ID;
    }
}