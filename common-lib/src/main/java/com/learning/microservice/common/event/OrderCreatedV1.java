package com.learning.microservice.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an order is accepted and stock has been reserved. Versioned by class-name
 * suffix — bump the suffix on breaking schema changes rather than mutating this record.
 */
public record OrderCreatedV1(
    UUID eventId,
    Instant occurredAt,
    UUID orderId,
    UUID userId,
    UUID productId,
    int quantity,
    long totalPriceCents)
    implements DomainEvent {}
