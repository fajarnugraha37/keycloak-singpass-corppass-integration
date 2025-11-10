package com.example.identity;

import com.example.config.CustomSAMLConfigurationRepresentation;
import com.example.config.CustomSAMLIdentityProviderConfig;
import org.keycloak.Config;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;

import static com.example.config.CustomSAMLIdentityProviderConfig.*;

public class CustomSAMLProviderFactory extends SAMLIdentityProviderFactory {
    private DestinationValidator destinationValidator;

    @Override
    public String getId() {
        return "custom-saml";
    }

    @Override
    public String getName() {
        return "Custom SAML v3";
    }

    @Override
    public SAMLIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new CustomSAMLProvider(
                session,
                new CustomSAMLIdentityProviderConfig(model),
                destinationValidator);
    }

    public void init(Config.Scope config) {
        super.init(config);
        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }

    @Override
    public SAMLIdentityProviderConfig createConfig() {
        return new CustomSAMLIdentityProviderConfig();
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String config) {
        try {
            var configMap = super.parseConfig(session, config);
            var configRepresentation = JsonSerialization.readValue(config, CustomSAMLConfigurationRepresentation.class);

            configMap.put(HASH_PRINCIPAL_FLAG, String.valueOf(configRepresentation.isHashPrincipalFlag()));
            configMap.put(DESTINATION_URI_VALIDATION_FLAG, String.valueOf(configRepresentation.isDestinationUriValidationFlag()));
            configMap.put(SIGNATURE_VALIDATION_ON_LOGOUT_FLAG, String.valueOf(configRepresentation.isSignatureValidationOnLogoutFlag()));
            configMap.put(AUDIENCES_VALIDATION_FLAG, String.valueOf(configRepresentation.isAudiencesValidationFlag()));
            configMap.put(VALID_AUDIENCES, configRepresentation.getValidAudiences());
            configMap.put(SUFFIX_IDP_NAME, configRepresentation.getSuffixIdpName());

            return configMap;
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
    }
}
