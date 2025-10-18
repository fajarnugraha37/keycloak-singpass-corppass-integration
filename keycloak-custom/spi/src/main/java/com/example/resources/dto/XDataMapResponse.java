package com.example.resources.dto;

import java.util.Map;

public class XDataMapResponse<T> extends XBaseResponse {
  private Map<String, T> data;

  public XDataMapResponse() {
  }

  public XDataMapResponse(Map<String, T> data, Integer status) {
    super(status);
    this.data = data;
  }

  public Map<String, T> getData() {
    return this.data;
  }

  public void setData(Map<String, T> data) {
    this.data = data;
  }
}