package com.example.key;

import org.keycloak.component.ComponentModel;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class SingleJwkKeyProviderFactory implements KeyProviderFactory<SingleJwkKeyProvider> {
    public static final String ID = "uploaded-single-jwk";
    public static final String CFG_JWK = "jwkJson";
    public static final String CFG_USE = "forceUse"; // auto|sig|enc
    public static final String CFG_PRIORITY = "priority";

    static final ProviderConfigProperty P_JWK = new ProviderConfigProperty(
            CFG_JWK, "JWK JSON",
            """
                    paste a single JWK object (ec). private or public. example: {"kty":"EC","crv":"P-256","x":"...","y":"...","d":"...","alg":"ES256","kid":"my-sig-key"}
                    """,
            ProviderConfigProperty.TEXT_TYPE, ""
    );
    static final ProviderConfigProperty P_USE = new ProviderConfigProperty(
            CFG_USE, "Use",
            "auto-infer from alg, or force: sig/enc",
            ProviderConfigProperty.LIST_TYPE, "auto"
    );
    static final ProviderConfigProperty P_PRIORITY = new ProviderConfigProperty(
            CFG_PRIORITY, "Priority",
            "provider priority (higher = preferred)",
            ProviderConfigProperty.STRING_TYPE, "100"
    );

    static {
        if (P_USE.getOptions() != null) {
            P_USE.getOptions().addAll(List.of("auto", "sig", "enc"));
        } else {
            P_USE.setOptions(List.of("auto", "sig", "enc"));
        }
    }

    public SingleJwkKeyProviderFactory() {
        // empty constructor
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public SingleJwkKeyProvider create(KeycloakSession session, ComponentModel model) {
        return new SingleJwkKeyProvider(session, model);
    }

    @Override
    public String getHelpText() {
        return "register exactly one uploaded JWK (EC)";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property(Attributes.PRIORITY_PROPERTY)
                .property(Attributes.ENABLED_PROPERTY)
                .property(Attributes.ACTIVE_PROPERTY)
                .property(P_JWK)
                .property(P_USE)
                .property(P_PRIORITY)
                .build();
    }
}
