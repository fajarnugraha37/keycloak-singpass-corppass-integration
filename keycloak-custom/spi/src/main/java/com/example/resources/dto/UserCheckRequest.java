package com.example.resources.dto;

public class UserCheckRequest {
    private String sgId;
    private String identityNumber;
    private String idpRole;

    public String getSgId() {
        return sgId;
    }

    public void setSgId(String sgId) {
        this.sgId = sgId;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getIdpRole() {
        return idpRole;
    }

    public void setIdpRole(String idpRole) {
        this.idpRole = idpRole;
    }
}
