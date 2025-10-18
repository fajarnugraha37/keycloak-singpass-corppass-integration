package com.example.resources;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class CustomCertsApiProviderFactory extends AlternateApiProviderFactory {

    public static final String ID = "oidc";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new CustomCertsApiProvider();
    }

    @Override
    public String getId() {
        return ID;
    }
}