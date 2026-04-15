package com.learning.microservice.common.error;

import com.learning.microservice.common.api.ApiResponse;
import com.learning.microservice.common.api.FieldError;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Single advice for every service — maps thrown exceptions to {@link ApiResponse} envelopes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
    ErrorCode code = ex.errorCode();
    return ResponseEntity.status(code.httpStatus()).body(ApiResponse.error(code, ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    List<FieldError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
        .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, "Validation failed", errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException ex) {
    List<FieldError> errors =
        ex.getConstraintViolations().stream()
            .map(v -> new FieldError(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
        .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, "Validation failed", errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
        .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Internal error"));
  }
}
