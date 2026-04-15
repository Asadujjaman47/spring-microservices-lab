package com.learning.microservice.product.web.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

// Implements Serializable so the default Redis JdkSerializationRedisSerializer can cache it.
public record ProductResponse(
    UUID id,
    String name,
    String description,
    long priceCents,
    int stock,
    Instant createdAt,
    Instant updatedAt)
    implements Serializable {}
