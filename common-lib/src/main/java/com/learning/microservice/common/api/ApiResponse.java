package com.learning.microservice.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.microservice.common.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Standard envelope for every HTTP response. Use static factories ({@link #ok}, {@link #error}) so
 * {@code path} and {@code traceId} are populated automatically from the current request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    ErrorCode errorCode,
    List<FieldError> errors,
    Instant timestamp,
    String path,
    String traceId) {

  public static <T> ApiResponse<T> ok(T data) {
    return ok(data, null);
  }

  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(
        true, data, message, null, null, Instant.now(), currentPath(), currentTraceId());
  }

  public static <T> ApiResponse<T> error(ErrorCode code, String message, List<FieldError> errors) {
    return new ApiResponse<>(
        false, null, message, code, errors, Instant.now(), currentPath(), currentTraceId());
  }

  public static <T> ApiResponse<T> error(ErrorCode code, String message) {
    return error(code, message, null);
  }

  private static String currentPath() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes sra) {
      return sra.getRequest().getRequestURI();
    }
    return null;
  }

  private static String currentTraceId() {
    return MDC.get("traceId");
  }
}
