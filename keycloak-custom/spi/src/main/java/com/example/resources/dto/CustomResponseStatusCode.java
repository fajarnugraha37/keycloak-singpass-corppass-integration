package com.example.resources.dto;

public enum CustomResponseStatusCode {
    USER_EXIST_ASSIGNED(290),
    USER_EXIST_NOT_ASSIGNED(291),
    USER_DISABLED(292);

    private final int status;

    CustomResponseStatusCode(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}