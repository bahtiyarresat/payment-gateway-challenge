package com.checkout.payment.gateway.model;

import java.util.List;

public class ErrorResponse {

  private final String code;
  private final String message;
  private final List<String> errors;

  public ErrorResponse(String code, String message) {
    this(code, message, null);
  }

  public ErrorResponse(String code, String message, List<String> errors) {
    this.code = code;
    this.message = message;
    this.errors = errors;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public List<String> getErrors() {
    return errors;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "code='" + code + '\'' +
        ", message='" + message + '\'' +
        ", errors=" + errors +
        '}';
  }
}
