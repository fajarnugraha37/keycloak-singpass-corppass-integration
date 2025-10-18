package com.example.identity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.util.JsonSerialization;

import com.example.config.CustomOIDCConfigurationRepresentation;
import com.example.config.CustomOIDCIdentityProviderConfig;

public class CustomOIDCProviderFactory extends OIDCIdentityProviderFactory {
    @Override
    public String getId() {
        return "custom-oidc";
    }

    @Override
    public String getName() {
        return "Custom OIDC v3";
    }

    @Override
    public OIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        var config = new CustomOIDCIdentityProviderConfig(model);
        return new CustomOIDCProvider(session, config);
    }

    @Override
    public OIDCIdentityProviderConfig createConfig() {
        return new CustomOIDCIdentityProviderConfig();
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String configString) {
        CustomOIDCConfigurationRepresentation rep;
        try {
            rep = JsonSerialization.readValue(configString, CustomOIDCConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        var config = new CustomOIDCIdentityProviderConfig();
        config.setIssuer(rep.getIssuer());
        config.setLogoutUrl(rep.getLogoutEndpoint());
        config.setAuthorizationUrl(rep.getAuthorizationEndpoint());
        config.setTokenUrl(rep.getTokenEndpoint());
        config.setUserInfoUrl(rep.getUserinfoEndpoint());
        if (rep.getJwksUri() != null) {
            config.setValidateSignature(true);
            config.setUseJwksUrl(true);
            config.setJwksUrl(rep.getJwksUri());
        }

        // custom config
        config.setEncryptedIdTokenFlag(rep.isEncryptedIdTokenFlag());
        config.setSigningKeyId(rep.getSigningKeyId());
        config.setForwarderUrl(rep.getForwarderUrl());
        config.setForwarderHeaderName(rep.getForwarderHeaderName());
        config.setIdpDifferentTimes(rep.getIdpDifferentTimes());
        config.setValidateNonce(rep.isValidateNonce());

        config.setClaimExtractionTemplate(rep.getClaimExtractionTemplate());
        config.setHashUsernameFlag(rep.isHashUsernameFlag());
        config.setSuffixIdpName(rep.getSuffixIdpName());
        config.setClaimExtractionToAttributeTemplate(rep.getClaimExtractionToAttributeTemplate());

        config.setRedirectClients(rep.getRedirectClients());

        config.setSingpassOIDCFlag(rep.isSingpassOIDCFlag());
        config.setSaveIdNumberToUserAttributeFlag(rep.isSaveIdNumberToUserAttributeFlag());
        config.setUserAttributeKeyForIdNumber(rep.getUserAttributeKeyForIdNumber());
        config.setKeyFromClaimToIdentityNumber(rep.getKeyFromClaimToIdentityNumber());

        return config.getConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        var builder = ProviderConfigurationBuilder.create();
        Stream.of(CustomOIDCConfigEnum.values())
                .forEach(config -> builder.property()
                        .name(config.getName())
                        .label(config.getLabel())
                        .helpText(config.getHelpText())
                        .type(config.getType())
                        .add());
        return builder.build();
    }
}
