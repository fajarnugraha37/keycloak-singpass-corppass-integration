package com.example.resources.dto;

class XSingleResponse<T> extends XBaseResponse {
    private T data;

    public XSingleResponse() {
    }

    public XSingleResponse(T data, Integer status) {
        super(status);
        this.data = data;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}