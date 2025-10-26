package com.example.identity.enums;

import org.keycloak.provider.ProviderConfigProperty;

public enum CustomOIDCConfigEnum {
    // OpenID Connect Configuration
    ENCRYPTED_ID_TOKEN_FLAG("Encrypted ID Token", "encryptedIdTokenFlag", ProviderConfigProperty.BOOLEAN_TYPE),
    SIGNING_KEY_ID("Signing Key ID", "signingKeyId", ProviderConfigProperty.STRING_TYPE),
    FORWARDED_URL("Forwarded URL", "forwarderUrl", ProviderConfigProperty.STRING_TYPE),
    FORWARDED_HEADER("Forwarder Header Name", "forwarderHeaderName", ProviderConfigProperty.STRING_TYPE),
    IDP_DIFF_TIMES("IDP Different Times (Seconds)", "idpDifferentTimes", ProviderConfigProperty.STRING_TYPE),
    VALIDATE_NONCE_FLAG("Validate Nonce ?", "validateNonce", ProviderConfigProperty.BOOLEAN_TYPE),

    // User Details
    CLAIM_EXTRACT_TEMPLATE("Claim Extraction Template", "claimExtractionTemplate", ProviderConfigProperty.STRING_TYPE),
    HASH_USERNAME_FLAG("Hash The Username ?", "hashUsernameFlag", ProviderConfigProperty.BOOLEAN_TYPE),
    IDP_NAME_SUFFIX("Username Suffix", "suffixIdpName", ProviderConfigProperty.STRING_TYPE),
    CLAIM_EXTRACT_TO_ATTR_TEMPLATE("Claim To Attribute Template", "claimExtractionToAttributeTemplate", ProviderConfigProperty.STRING_TYPE),

    // Client IDs for redirection
    REDIRECT_CLIENTS("Client IDs", "redirectClients", ProviderConfigProperty.STRING_TYPE),

    // Configuration properties below not shown in the Custom OIDC Identity Provider setup form
    SINGPASS_OIDC_FLAG("Is OIDC for SingPass ?", "singpassOIDCFlag", ProviderConfigProperty.BOOLEAN_TYPE),
    SAVE_ID_TO_USER_ATTR_FLAG("Save ID to user attribute ?", "saveIdNumberToUserAttributeFlag", ProviderConfigProperty.BOOLEAN_TYPE),
    ID_NUMBER_USER_ATTR_KEY("ID Number Attribute Key", "userAttributeKeyForIdNumber", ProviderConfigProperty.STRING_TYPE),
    ID_NUMBER_CLAIM_KEY("ID Number Claim Key", "keyFromClaimToIdentityNumber", ProviderConfigProperty.STRING_TYPE),
    ;

    private String name;
    private String label;
    private String helpText;
    private String type;

    CustomOIDCConfigEnum(String helpText,
                         String label,
                         String name,
                         String type) {
        this.helpText = helpText;
        this.label = label;
        this.name = name;
        this.type = type;
    }

    CustomOIDCConfigEnum(String label, String name, String type) {
        this.label = label;
        this.name = name;
        this.type = type;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}