package com.example.config;


public class CustomFAPIConfigurationRepresentation extends CustomOIDCConfigurationRepresentation {

    // ============ FAPI 2.0 Configuration Properties ============

    private boolean dpopEnabled;
    private String dpopSigningKeyId;
    private boolean parEnabled;
    private String parEndpoint;
    private boolean jarmEnabled;
    private String jarmResponseMode;
    private boolean fapi2Mode;
    private String dpopNonce;
    private boolean senderConstrainedTokens;

    // ============ Getters and Setters ============

    public boolean isDpopEnabled() {
        return dpopEnabled;
    }

    public void setDpopEnabled(boolean dpopEnabled) {
        this.dpopEnabled = dpopEnabled;
    }

    public String getDpopSigningKeyId() {
        return dpopSigningKeyId;
    }

    public void setDpopSigningKeyId(String dpopSigningKeyId) {
        this.dpopSigningKeyId = dpopSigningKeyId;
    }

    public boolean isParEnabled() {
        return parEnabled;
    }

    public void setParEnabled(boolean parEnabled) {
        this.parEnabled = parEnabled;
    }

    public String getParEndpoint() {
        return parEndpoint;
    }

    public void setParEndpoint(String parEndpoint) {
        this.parEndpoint = parEndpoint;
    }

    public boolean isJarmEnabled() {
        return jarmEnabled;
    }

    public void setJarmEnabled(boolean jarmEnabled) {
        this.jarmEnabled = jarmEnabled;
    }

    public String getJarmResponseMode() {
        return jarmResponseMode;
    }

    public void setJarmResponseMode(String jarmResponseMode) {
        this.jarmResponseMode = jarmResponseMode;
    }

    public boolean isFapi2Mode() {
        return fapi2Mode;
    }

    public void setFapi2Mode(boolean fapi2Mode) {
        this.fapi2Mode = fapi2Mode;
    }

    public String getDpopNonce() {
        return dpopNonce;
    }

    public void setDpopNonce(String dpopNonce) {
        this.dpopNonce = dpopNonce;
    }

    public boolean isSenderConstrainedTokens() {
        return senderConstrainedTokens;
    }

    public void setSenderConstrainedTokens(boolean senderConstrainedTokens) {
        this.senderConstrainedTokens = senderConstrainedTokens;
    }
}