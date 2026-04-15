package com.learning.microservice.order.messaging;

import com.learning.microservice.common.event.OrderCreatedV1;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventRelay {

  private final OrderEventPublisher eventPublisher;

  public OrderEventRelay(OrderEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOrderCreated(OrderCreatedV1 event) {
    eventPublisher.publishOrderCreated(event);
  }
}
