package com.learning.microservice.order.web.dto;

import com.learning.microservice.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID userId,
    UUID productId,
    int quantity,
    long unitPriceCents,
    long totalPriceCents,
    OrderStatus status,
    Instant createdAt) {}
