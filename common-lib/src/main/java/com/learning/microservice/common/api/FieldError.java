package com.learning.microservice.common.api;

/** One entry per invalid field in a validation failure. */
public record FieldError(String field, String message) {}
