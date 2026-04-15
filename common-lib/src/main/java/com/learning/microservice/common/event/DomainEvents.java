package com.learning.microservice.common.event;

/** Shared constants for the {@code domain.events} topic exchange. */
public final class DomainEvents {

  public static final String EXCHANGE = "domain.events";

  public static final class RoutingKeys {
    public static final String ORDER_CREATED = "order.created";

    private RoutingKeys() {}
  }

  private DomainEvents() {}
}
