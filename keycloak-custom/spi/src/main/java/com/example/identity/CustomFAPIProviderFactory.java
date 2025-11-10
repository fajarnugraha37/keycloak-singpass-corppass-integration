package com.example.identity;

import com.example.config.CustomFAPIConfigurationRepresentation;
import com.example.config.CustomFapiProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CustomFAPIProviderFactory extends CustomOIDCProviderFactory {
    @Override
    public String getId() {
        return "custom-fapi-2.0";
    }

    @Override
    public String getName() {
        return "Custom FAPI 2.0 OIDC";
    }

    @Override
    public CustomFAPIProvider create(KeycloakSession session, IdentityProviderModel model) {
        var config = new CustomFapiProviderConfig(model);
        return new CustomFAPIProvider(session, config);
    }

    @Override
    public CustomFapiProviderConfig createConfig() {
        return new CustomFapiProviderConfig();
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String configString) {
        CustomFAPIConfigurationRepresentation rep;
        try {
            rep = JsonSerialization.readValue(configString, CustomFAPIConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        var config = new CustomFapiProviderConfig();
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

        // ============ FAPI 2.0 Configuration ============
        config.setDPoPEnabled(rep.isDpopEnabled());
        config.setDPoPSigningKeyId(rep.getDpopSigningKeyId());
        config.setPAREnabled(rep.isParEnabled());
        config.setPAREndpoint(rep.getParEndpoint());
        config.setJARMEnabled(rep.isJarmEnabled());
        config.setJARMResponseMode(rep.getJarmResponseMode());
        config.setFAPI2Mode(rep.isFapi2Mode());
        config.setDPoPNonce(rep.getDpopNonce());
        config.setSenderConstrainedTokens(rep.isSenderConstrainedTokens());

        return config.getConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        var builder = ProviderConfigurationBuilder.create();
        Stream.of(CustomFAPIConfigEnum.values())
                .forEach(config -> builder.property()
                        .name(config.getName())
                        .label(config.getLabel())
                        .helpText(config.getHelpText())
                        .type(config.getType())
                        .add());
        return builder.build();
    }
}
