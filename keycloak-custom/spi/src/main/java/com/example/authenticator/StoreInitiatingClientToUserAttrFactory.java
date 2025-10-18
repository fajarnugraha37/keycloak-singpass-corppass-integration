package com.example.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.models.AuthenticationExecutionModel;

public class StoreInitiatingClientToUserAttrFactory
        implements AuthenticatorFactory, EnvironmentDependentProviderFactory {
    private static final Logger logger = Logger.getLogger(StoreInitiatingClientToUserAttrFactory.class);
    public static final String ID = "store-initiating-client-to-user-attr";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "Store Initiating Client → User Attribute";
    }

    @Override
    public String getHelpText() {
        return "Saves clientId that initiated login to user attribute.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new StoreInitiatingClientToUserAttr();
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] { 
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE
         };
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public java.util.List<org.keycloak.provider.ProviderConfigProperty> getConfigProperties() {
        return java.util.List.of();
    }

    @Override
    public String getReferenceCategory() {
        return "Store Initiating Client";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override

    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public boolean isSupported(Scope config) {
        return true;
    }
}
