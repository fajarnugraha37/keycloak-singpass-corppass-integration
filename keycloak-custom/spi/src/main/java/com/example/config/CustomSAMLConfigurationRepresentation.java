package com.example.config;

public class CustomSAMLConfigurationRepresentation {

    private boolean hashPrincipalFlag;
    private boolean destinationUriValidationFlag;
    private boolean signatureValidationOnLogoutFlag;
    private boolean audiencesValidationFlag;
    private String validAudiences;
    private String suffixIdpName;

    public boolean isHashPrincipalFlag() {
        return hashPrincipalFlag;
    }

    public void setHashPrincipalFlag(boolean hashPrincipalFlag) {
        this.hashPrincipalFlag = hashPrincipalFlag;
    }

    public boolean isDestinationUriValidationFlag() {
        return destinationUriValidationFlag;
    }

    public void setDestinationUriValidationFlag(boolean destinationUriValidationFlag) {
        this.destinationUriValidationFlag = destinationUriValidationFlag;
    }

    public boolean isSignatureValidationOnLogoutFlag() {
        return signatureValidationOnLogoutFlag;
    }

    public void setSignatureValidationOnLogoutFlag(boolean signatureValidationOnLogoutFlag) {
        this.signatureValidationOnLogoutFlag = signatureValidationOnLogoutFlag;
    }

    public boolean isAudiencesValidationFlag() {
        return audiencesValidationFlag;
    }

    public void setAudiencesValidationFlag(boolean audiencesValidationFlag) {
        this.audiencesValidationFlag = audiencesValidationFlag;
    }

    public String getValidAudiences() {
        return validAudiences;
    }

    public void setValidAudiences(String validAudiences) {
        this.validAudiences = validAudiences;
    }

    public String getSuffixIdpName() {
        return suffixIdpName;
    }

    public void setSuffixIdpName(String suffixIdpName) {
        this.suffixIdpName = suffixIdpName;
    }
}
