package com.example.config;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class CustomSAMLIdentityProviderConfig extends SAMLIdentityProviderConfig {

    public static final String HASH_PRINCIPAL_FLAG = "hashPrincipalFlag";
    public static final String DESTINATION_URI_VALIDATION_FLAG = "destinationUriValidationFlag";
    public static final String SIGNATURE_VALIDATION_ON_LOGOUT_FLAG = "signatureValidationOnLogoutFlag";
    public static final String AUDIENCES_VALIDATION_FLAG = "audiencesValidationFlag";
    public static final String VALID_AUDIENCES = "validAudiences";
    public static final String SUFFIX_IDP_NAME = "suffixIdpName";

    public CustomSAMLIdentityProviderConfig() {
        super();
    }

    public CustomSAMLIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public boolean isHashPrincipalFlag() {
        return Boolean.parseBoolean(getConfig().get(HASH_PRINCIPAL_FLAG));
    }

    public void setHashPrincipalFlag(boolean hashPrincipalFlag) {
        getConfig().put(HASH_PRINCIPAL_FLAG, String.valueOf(hashPrincipalFlag));
    }

    public boolean isDestinationUriValidationFlag() {
        return Boolean.parseBoolean(getConfig().get(DESTINATION_URI_VALIDATION_FLAG));
    }

    public void setDestinationUriValidationFlag(boolean destinationUriValidationFlag) {
        getConfig().put(DESTINATION_URI_VALIDATION_FLAG, String.valueOf(destinationUriValidationFlag));
    }

    public boolean isSignatureValidationOnLogoutFlag() {
        return Boolean.parseBoolean(getConfig().get(SIGNATURE_VALIDATION_ON_LOGOUT_FLAG));
    }

    public void setSignatureValidationOnLogoutFlag(boolean signatureValidationOnLogoutFlag) {
        getConfig().put(SIGNATURE_VALIDATION_ON_LOGOUT_FLAG, String.valueOf(signatureValidationOnLogoutFlag));
    }

    public boolean isAudiencesValidationFlag() {
        return Boolean.parseBoolean(getConfig().get(AUDIENCES_VALIDATION_FLAG));
    }

    public void setAudiencesValidationFlag(boolean audiencesValidationFlag) {
        getConfig().put(AUDIENCES_VALIDATION_FLAG, String.valueOf(audiencesValidationFlag));
    }

    public List<String> getValidAudiences() {
        if (isNull(getConfig().get(VALID_AUDIENCES)) || getConfig().get(VALID_AUDIENCES).isEmpty()) {
            return null;
        }

        return Arrays.stream(getConfig().get(VALID_AUDIENCES).split(";"))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public void setValidAudiences(String validAudiences) {
        getConfig().put(VALID_AUDIENCES, validAudiences);
    }

    public String getSuffixIdpName() {
        return getConfig().get(SUFFIX_IDP_NAME);
    }

    public void setSuffixIdpName(String suffixIdpName) {
        getConfig().put(SUFFIX_IDP_NAME, suffixIdpName);
    }
}
