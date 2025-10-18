package com.example.config;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class CustomOIDCIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public CustomOIDCIdentityProviderConfig() {
    }

    public CustomOIDCIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public boolean isEncryptedIdTokenFlag() {
        return Boolean.parseBoolean(getConfig().get("encryptedIdTokenFlag"));
    }

    public void setEncryptedIdTokenFlag(boolean encryptedIdTokenFlag) {
        getConfig().put("encryptedIdTokenFlag", String.valueOf(encryptedIdTokenFlag));
    }

    public String getSigningKeyId() {
        return getConfig().get("signingKeyId");
    }

    public void setSigningKeyId(String signingKeyId) {
        getConfig().put("signingKeyId", signingKeyId);
    }

    public String getForwarderUrl() {
        return getConfig().get("forwarderUrl");
    }

    public void setForwarderUrl(String forwarderUrl) {
        getConfig().put("forwarderUrl", forwarderUrl);
    }

    public String getForwarderHeaderName() {
        return getConfig().get("forwarderHeaderName");
    }

    public void setForwarderHeaderName(String forwarderHeaderName) {
        getConfig().put("forwarderHeaderName", forwarderHeaderName);
    }

    public String getIdpDifferentTimes() {
        return getConfig().get("idpDifferentTimes");
    }

    public void setIdpDifferentTimes(String idpDifferentTimes) {
        getConfig().put("idpDifferentTimes", idpDifferentTimes);
    }

    public boolean isSingpassOIDCFlag() {
        return Boolean.parseBoolean(getConfig().get("singpassOIDCFlag"));
    }

    public void setSingpassOIDCFlag(boolean singpassOIDCFlag) {
        getConfig().put("singpassOIDCFlag", String.valueOf(singpassOIDCFlag));
    }

    public boolean isHashUsernameFlag() {
        return Boolean.parseBoolean(getConfig().get("hashUsernameFlag"));
    }

    public void setHashUsernameFlag(boolean hashUsernameFlag) {
        getConfig().put("hashUsernameFlag", String.valueOf(hashUsernameFlag));
    }

    public boolean isSaveIdNumberToUserAttributeFlag() {
        return Boolean.parseBoolean(getConfig().get("saveIdNumberToUserAttributeFlag"));
    }

    public void setSaveIdNumberToUserAttributeFlag(boolean saveIdNumberToUserAttributeFlag) {
        getConfig().put("saveIdNumberToUserAttributeFlag", String.valueOf(saveIdNumberToUserAttributeFlag));
    }

    public String getKeyFromClaimToIdentityNumber() {
        return getConfig().get("keyFromClaimToIdentityNumber");
    }

    public void setKeyFromClaimToIdentityNumber(String keyFromClaimToIdentityNumber) {
        getConfig().put("keyFromClaimToIdentityNumber", keyFromClaimToIdentityNumber);
    }

    public String getSuffixIdpName() {
        return getConfig().get("suffixIdpName");
    }

    public void setSuffixIdpName(String suffixIdpName) {
        getConfig().put("suffixIdpName", suffixIdpName);
    }

    public String getUserAttributeKeyForIdNumber() {
        return getConfig().get("userAttributeKeyForIdNumber");
    }

    public void setUserAttributeKeyForIdNumber(String userAttributeKeyForIdNumber) {
        getConfig().put("userAttributeKeyForIdNumber", userAttributeKeyForIdNumber);
    }

    public String getRedirectClients() {
        return getConfig().get("redirectClients");
    }

    public void setRedirectClients(String redirectClients) {
        getConfig().put("redirectClients", redirectClients);
    }

    public boolean isValidateNonce() {
        return Boolean.parseBoolean(getConfig().get("validateNonce"));
    }

    public void setValidateNonce(boolean validateNonce) {
        getConfig().put("validateNonce", String.valueOf(validateNonce));
    }

    public String getClaimExtractionTemplate() {
        return getConfig().get("claimExtractionTemplate");
    }

    public void setClaimExtractionTemplate(String claimExtractionTemplate) {
        getConfig().put("claimExtractionTemplate", claimExtractionTemplate);
    }

    public String getClaimExtractionToAttributeTemplate() {
        return getConfig().get("claimExtractionToAttributeTemplate");

    }

    public void setClaimExtractionToAttributeTemplate(String claimExtractionToAttributeTemplate) {
        getConfig().put("claimExtractionToAttributeTemplate", claimExtractionToAttributeTemplate);
    }
}