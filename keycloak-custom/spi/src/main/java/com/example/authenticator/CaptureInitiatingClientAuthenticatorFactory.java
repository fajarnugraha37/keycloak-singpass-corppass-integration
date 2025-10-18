package com.example.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.models.AuthenticationExecutionModel;

import java.util.List;

public class CaptureInitiatingClientAuthenticatorFactory
        implements AuthenticatorFactory, EnvironmentDependentProviderFactory {
    private static final Logger logger = Logger.getLogger(CaptureInitiatingClientAuthenticatorFactory.class);

    public static final String ID = "capture-initiating-client";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "Capture Initiating Client";
    }

    @Override
    public String getHelpText() {
        return "Stores initial clientId into user session note 'initiating-client'.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new CaptureInitiatingClientAuthenticator();
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
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    // no config properties
    @Override
    public List<org.keycloak.provider.ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public String getReferenceCategory() {
        return "Capture Initiating Client";
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
