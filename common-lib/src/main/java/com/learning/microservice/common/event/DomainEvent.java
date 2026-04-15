package com.learning.microservice.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for events published to {@code domain.events}. Implementations are immutable
 * records named with a version suffix ({@code OrderCreatedV1}); bump the suffix on breaking schema
 * changes rather than mutating an existing type.
 */
public interface DomainEvent {

  /** Globally unique id of this event occurrence — used by consumers for idempotent dedupe. */
  UUID eventId();

  /** When the event was produced, UTC. */
  Instant occurredAt();
}
