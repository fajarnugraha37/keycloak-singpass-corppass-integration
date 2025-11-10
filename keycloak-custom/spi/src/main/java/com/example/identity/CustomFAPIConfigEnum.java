package com.example.identity;

import org.keycloak.provider.ProviderConfigProperty;

public enum CustomFAPIConfigEnum {
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

    // ============ FAPI 2.0 Configuration Properties ============

    // DPoP (Demonstrating Proof-of-Possession)
    DPOP_ENABLED("Enable DPoP for sender-constrained access tokens (FAPI 2.0)", "dpopEnabled", ProviderConfigProperty.BOOLEAN_TYPE),
    DPOP_SIGNING_KEY_ID("DPoP Signing Key ID - Key used to sign DPoP proofs", "dpopSigningKeyId", ProviderConfigProperty.STRING_TYPE),
    DPOP_NONCE("DPoP Nonce - Server-provided nonce for DPoP proof (optional)", "dpopNonce", ProviderConfigProperty.STRING_TYPE),

    // PAR (Pushed Authorization Request)
    PAR_ENABLED("Enable PAR - Push authorization request parameters securely (FAPI 2.0)", "parEnabled", ProviderConfigProperty.BOOLEAN_TYPE),
    PAR_ENDPOINT("PAR Endpoint URL - Pushed Authorization Request endpoint", "parEndpoint", ProviderConfigProperty.STRING_TYPE),

    // JARM (JWT-Secured Authorization Response Mode)
    JARM_ENABLED("Enable JARM - Receive authorization response as signed JWT (FAPI 2.0)", "jarmEnabled", ProviderConfigProperty.BOOLEAN_TYPE),
    JARM_RESPONSE_MODE("JARM Response Mode - jwt, query.jwt, fragment.jwt, or form_post.jwt", "jarmResponseMode", ProviderConfigProperty.STRING_TYPE),

    // FAPI 2.0 Mode
    FAPI2_MODE("Enable FAPI 2.0 Mode - Enforces all FAPI 2.0 security requirements", "fapi2Mode", ProviderConfigProperty.BOOLEAN_TYPE),

    // Sender-Constrained Tokens
    SENDER_CONSTRAINED_TOKENS("Enable Sender-Constrained Tokens - Binds tokens to client (DPoP or mTLS)", "senderConstrainedTokens", ProviderConfigProperty.BOOLEAN_TYPE),

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

    CustomFAPIConfigEnum(String helpText,
                         String label,
                         String name,
                         String type) {
        this.helpText = helpText;
        this.label = label;
        this.name = name;
        this.type = type;
    }

    CustomFAPIConfigEnum(String label, String name, String type) {
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

