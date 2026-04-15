package com.learning.microservice.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.learning.microservice.common.event.DomainEvents;
import com.learning.microservice.common.event.OrderCreatedV1;
import com.learning.microservice.notification.config.RabbitConfig;
import com.learning.microservice.notification.domain.ProcessedEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

class OrderCreatedListenerTests extends AbstractIntegrationTest {

  @Autowired RabbitTemplate rabbit;
  @Autowired ProcessedEventRepository processed;

  @Test
  void duplicateDeliveries_areDedupedByEventId() {
    UUID eventId = UUID.randomUUID();
    OrderCreatedV1 event =
        new OrderCreatedV1(
            eventId,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            1999L);

    rabbit.convertAndSend(DomainEvents.EXCHANGE, DomainEvents.RoutingKeys.ORDER_CREATED, event);
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(processed.existsById(eventId)).isTrue());

    long before = processed.count();
    // Re-publish the exact same event — duplicate PK will make the handler return early.
    rabbit.convertAndSend(DomainEvents.EXCHANGE, DomainEvents.RoutingKeys.ORDER_CREATED, event);
    await()
        .during(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(4))
        .untilAsserted(() -> assertThat(processed.count()).isEqualTo(before));
  }

  @Test
  void poisonMessage_isRoutedToDlq_afterRetriesExhausted() {
    // Send a payload that cannot be deserialized into OrderCreatedV1 — Jackson fails in the
    // message converter, which for Spring AMQP is wrapped as a non-retryable conversion
    // exception that routes immediately to the DLQ (requeue=false).
    rabbit.convertAndSend(
        DomainEvents.EXCHANGE, DomainEvents.RoutingKeys.ORDER_CREATED, "this-is-not-json");

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              Object dlqMsg = rabbit.receiveAndConvert(RabbitConfig.ORDER_CREATED_DLQ, 100);
              assertThat(dlqMsg).as("expected a message on the DLQ").isNotNull();
            });
  }
}
