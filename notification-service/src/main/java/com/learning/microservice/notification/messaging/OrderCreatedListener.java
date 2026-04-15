package com.learning.microservice.notification.messaging;

import com.learning.microservice.common.event.OrderCreatedV1;
import com.learning.microservice.notification.config.RabbitConfig;
import com.learning.microservice.notification.domain.ProcessedEvent;
import com.learning.microservice.notification.domain.ProcessedEventRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderCreatedListener {

  private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
  private static final String EVENT_TYPE = "OrderCreatedV1";

  private final ProcessedEventRepository processed;

  public OrderCreatedListener(ProcessedEventRepository processed) {
    this.processed = processed;
  }

  // Idempotent: inserting into processed_events first makes a duplicate redelivery crash on the
  // PK constraint — we catch that and ack silently. Real side-effects (email send) happen only
  // on the path that owns the fresh row, so they run at-most-once per unique eventId. Exceptions
  // other than the duplicate-key one propagate, which the AMQP retry interceptor then retries with
  // exponential backoff; once attempts exhaust, Spring rejects with requeue=false and the message
  // is routed to the DLQ via the x-dead-letter-exchange on the main queue.
  @Transactional
  @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
  public void handle(OrderCreatedV1 event) {
    try {
      processed.save(new ProcessedEvent(event.eventId(), EVENT_TYPE, Instant.now()));
    } catch (DataIntegrityViolationException dup) {
      log.info(
          "Duplicate {} eventId={} — skipping notification side-effects",
          EVENT_TYPE,
          event.eventId());
      return;
    }
    log.info(
        "📣 notify: order {} placed by user {} — {} x product {} (total {} cents)",
        event.orderId(),
        event.userId(),
        event.quantity(),
        event.productId(),
        event.totalPriceCents());
    // In a real system this is where an email / SMS provider call would go.
  }
}
