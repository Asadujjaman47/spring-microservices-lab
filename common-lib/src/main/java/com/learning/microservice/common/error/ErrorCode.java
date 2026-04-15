package com.learning.microservice.common.error;

import org.springframework.http.HttpStatus;

/**
 * Central, domain-namespaced error catalog. Stable machine-readable identifier paired with a
 * default HTTP status. Messages stay in the thrown exception — codes here are forever.
 */
public enum ErrorCode {
  // Generic
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
  FORBIDDEN(HttpStatus.FORBIDDEN),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  CONFLICT(HttpStatus.CONFLICT),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

  // User domain
  USER_NOT_FOUND(HttpStatus.NOT_FOUND),
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
  USER_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),

  // Product domain
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
  PRODUCT_OUT_OF_STOCK(HttpStatus.CONFLICT),

  // Order domain
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
  ORDER_INVALID_STATE(HttpStatus.CONFLICT);

  private final HttpStatus httpStatus;

  ErrorCode(HttpStatus httpStatus) {
    this.httpStatus = httpStatus;
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }
}
