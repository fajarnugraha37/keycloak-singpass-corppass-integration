package com.example.config;

import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

public class CustomOIDCConfigurationRepresentation extends OIDCConfigurationRepresentation {

    private boolean encryptedIdTokenFlag;
    private String forwarderUrl;
    private String forwarderHeaderName;
    private String signingKeyId;
    private String idpDifferentTimes;
    private boolean singpassOIDCFlag;
    private boolean hashUsernameFlag;
    private boolean saveIdNumberToUserAttributeFlag;
    private String userAttributeKeyForIdNumber;
    private String keyFromClaimToIdentityNumber;
    private String suffixIdpName;
    private String redirectClients;
    private boolean validateNonce;
    private String claimExtractionTemplate;
    private String claimExtractionToAttributeTemplate;

    public String getIdpDifferentTimes() {
        return idpDifferentTimes;
    }

    public void setIdpDifferentTimes(String idpDifferentTimes) {
        this.idpDifferentTimes = idpDifferentTimes;
    }

    public boolean isEncryptedIdTokenFlag() {
        return encryptedIdTokenFlag;
    }

    public void setEncryptedIdTokenFlag(boolean encryptedIdTokenFlag) {
        this.encryptedIdTokenFlag = encryptedIdTokenFlag;
    }

    public String getForwarderUrl() {
        return forwarderUrl;
    }

    public void setForwarderUrl(String forwarderUrl) {
        this.forwarderUrl = forwarderUrl;
    }

    public String getSigningKeyId() {
        return signingKeyId;
    }

    public void setSigningKeyId(String signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    public String getForwarderHeaderName() {
        return forwarderHeaderName;
    }

    public void setForwarderHeaderName(String forwarderHeaderName) {
        this.forwarderHeaderName = forwarderHeaderName;
    }

    public boolean isSingpassOIDCFlag() {
        return singpassOIDCFlag;
    }

    public void setSingpassOIDCFlag(boolean singpassOIDCFlag) {
        this.singpassOIDCFlag = singpassOIDCFlag;
    }

    public boolean isHashUsernameFlag() {
        return hashUsernameFlag;
    }

    public void setHashUsernameFlag(boolean hashUsernameFlag) {
        this.hashUsernameFlag = hashUsernameFlag;
    }

    public boolean isSaveIdNumberToUserAttributeFlag() {
        return saveIdNumberToUserAttributeFlag;
    }

    public void setSaveIdNumberToUserAttributeFlag(boolean saveIdNumberToUserAttributeFlag) {
        this.saveIdNumberToUserAttributeFlag = saveIdNumberToUserAttributeFlag;
    }

    public String getKeyFromClaimToIdentityNumber() {
        return keyFromClaimToIdentityNumber;
    }

    public void setKeyFromClaimToIdentityNumber(String keyFromClaimToIdentityNumber) {
        this.keyFromClaimToIdentityNumber = keyFromClaimToIdentityNumber;
    }

    public String getSuffixIdpName() {
        return suffixIdpName;
    }

    public void setSuffixIdpName(String suffixIdpName) {
        this.suffixIdpName = suffixIdpName;
    }

    public String getUserAttributeKeyForIdNumber() {
        return userAttributeKeyForIdNumber;
    }

    public void setUserAttributeKeyForIdNumber(String userAttributeKeyForIdNumber) {
        this.userAttributeKeyForIdNumber = userAttributeKeyForIdNumber;
    }

    public String getRedirectClients() {
        return redirectClients;
    }

    public void setRedirectClients(String redirectClients) {
        this.redirectClients = redirectClients;
    }

    public boolean isValidateNonce() {
        return validateNonce;
    }

    public void setValidateNonce(boolean validateNonce) {
        this.validateNonce = validateNonce;
    }

    public String getClaimExtractionTemplate() {
        return claimExtractionTemplate;
    }

    public void setClaimExtractionTemplate(String claimExtractionTemplate) {
        this.claimExtractionTemplate = claimExtractionTemplate;
    }

    public String getClaimExtractionToAttributeTemplate() {
        return claimExtractionToAttributeTemplate;
    }

    public void setClaimExtractionToAttributeTemplate(String claimExtractionToAttributeTemplate) {
        this.claimExtractionToAttributeTemplate = claimExtractionToAttributeTemplate;
    }
}