package com.example.resources.dto;

public class XBaseResponse {
    private int status;

    public XBaseResponse() {
    }

    public XBaseResponse(Integer status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}