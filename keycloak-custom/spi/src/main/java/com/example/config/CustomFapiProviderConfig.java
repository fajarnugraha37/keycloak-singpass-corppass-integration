package com.example.config;

import org.keycloak.models.IdentityProviderModel;

public class CustomFapiProviderConfig extends CustomOIDCIdentityProviderConfig {

    public CustomFapiProviderConfig() {
        super();
    }

    public CustomFapiProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    // ============ FAPI 2.0 Configuration Options ============

    /**
     * Enable DPoP (Demonstrating Proof-of-Possession) for FAPI 2.0 compliance
     */
    public boolean isDPoPEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault("dpopEnabled", "false"));
    }

    public void setDPoPEnabled(boolean dpopEnabled) {
        getConfig().put("dpopEnabled", String.valueOf(dpopEnabled));
    }

    /**
     * Get the Key ID for DPoP signing key
     */
    public String getDPoPSigningKeyId() {
        return getConfig().get("dpopSigningKeyId");
    }

    public void setDPoPSigningKeyId(String dpopSigningKeyId) {
        getConfig().put("dpopSigningKeyId", dpopSigningKeyId);
    }

    /**
     * Enable PAR (Pushed Authorization Request) for FAPI 2.0 compliance
     */
    public boolean isPAREnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault("parEnabled", "false"));
    }

    public void setPAREnabled(boolean parEnabled) {
        getConfig().put("parEnabled", String.valueOf(parEnabled));
    }

    /**
     * Get PAR endpoint URL
     */
    public String getPAREndpoint() {
        return getConfig().get("parEndpoint");
    }

    public void setPAREndpoint(String parEndpoint) {
        getConfig().put("parEndpoint", parEndpoint);
    }

    /**
     * Enable JARM (JWT-Secured Authorization Response Mode)
     */
    public boolean isJARMEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault("jarmEnabled", "false"));
    }

    public void setJARMEnabled(boolean jarmEnabled) {
        getConfig().put("jarmEnabled", String.valueOf(jarmEnabled));
    }

    /**
     * Get JARM response mode (query.jwt, fragment.jwt, form_post.jwt, jwt)
     */
    public String getJARMResponseMode() {
        return getConfig().getOrDefault("jarmResponseMode", "jwt");
    }

    public void setJARMResponseMode(String jarmResponseMode) {
        getConfig().put("jarmResponseMode", jarmResponseMode);
    }

    /**
     * Enable FAPI 2.0 mode - enforces all FAPI 2.0 security requirements
     */
    public boolean isFAPI2Mode() {
        return Boolean.parseBoolean(getConfig().getOrDefault("fapi2Mode", "false"));
    }

    public void setFAPI2Mode(boolean fapi2Mode) {
        getConfig().put("fapi2Mode", Boolean.toString(fapi2Mode));
    }

    /**
     * Nonce for DPoP to prevent replay attacks
     */
    public String getDPoPNonce() {
        return getConfig().get("dpopNonce");
    }

    public void setDPoPNonce(String dpopNonce) {
        getConfig().put("dpopNonce", dpopNonce);
    }

    /**
     * Enable sender-constrained access tokens (mTLS or DPoP)
     */
    public boolean isSenderConstrainedTokens() {
        return Boolean.parseBoolean(getConfig().getOrDefault("senderConstrainedTokens", "false"));
    }

    public void setSenderConstrainedTokens(boolean senderConstrainedTokens) {
        getConfig().put("senderConstrainedTokens", String.valueOf(senderConstrainedTokens));
    }
}