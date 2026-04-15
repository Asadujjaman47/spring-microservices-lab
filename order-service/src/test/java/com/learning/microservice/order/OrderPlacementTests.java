package com.learning.microservice.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import com.learning.microservice.common.event.DomainEvents;
import com.learning.microservice.common.event.OrderCreatedV1;
import com.learning.microservice.order.client.ProductStockClient;
import com.learning.microservice.order.client.ProductStockReservation;
import com.learning.microservice.order.service.OrderService;
import com.learning.microservice.order.web.dto.CreateOrderRequest;
import com.learning.microservice.order.web.dto.OrderResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(OrderPlacementTests.TestRabbitConfig.class)
class OrderPlacementTests extends AbstractIntegrationTest {

  @Autowired OrderService orderService;
  @MockitoBean ProductStockClient productStockClient;
  @Autowired TestListener testListener;
  @Autowired RabbitAdmin rabbitAdmin;

  @BeforeEach
  void resetCapture() {
    testListener.lastEvent.set(null);
  }

  @Test
  void placeOrder_reservesStock_persists_andPublishesEvent() {
    UUID userId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    when(productStockClient.reserve(any(UUID.class), anyInt()))
        .thenReturn(new ProductStockReservation(productId, 2, 8, 1999L));

    OrderResponse response = orderService.place(new CreateOrderRequest(userId, productId, 2));

    assertThat(response.id()).isNotNull();
    assertThat(response.totalPriceCents()).isEqualTo(1999L * 2);

    // The event publish is transactional (AFTER_COMMIT) — poll until the test queue sees it.
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              OrderCreatedV1 event = testListener.lastEvent.get();
              assertThat(event).isNotNull();
              assertThat(event.orderId()).isEqualTo(response.id());
              assertThat(event.userId()).isEqualTo(userId);
              assertThat(event.productId()).isEqualTo(productId);
              assertThat(event.quantity()).isEqualTo(2);
            });
  }

  @Test
  void placeOrder_propagatesBusinessException_fromStockClient() {
    when(productStockClient.reserve(any(UUID.class), anyInt()))
        .thenThrow(new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK, "Insufficient stock"));

    assertThatThrownBy(
            () ->
                orderService.place(
                    new CreateOrderRequest(UUID.randomUUID(), UUID.randomUUID(), 99)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK));
  }

  // Binds an anonymous queue to domain.events under order.created so the test can observe
  // messages the real OrderEventPublisher produces — no mocking of the broker.
  @TestConfiguration
  static class TestRabbitConfig {

    @Bean
    Queue testCaptureQueue() {
      return new AnonymousQueue();
    }

    @Bean
    Binding testCaptureBinding(Queue testCaptureQueue, TopicExchange domainEventsExchange) {
      return BindingBuilder.bind(testCaptureQueue)
          .to(domainEventsExchange)
          .with(DomainEvents.RoutingKeys.ORDER_CREATED);
    }

    @Bean
    TestListener testListener() {
      return new TestListener();
    }
  }

  static class TestListener {
    final AtomicReference<OrderCreatedV1> lastEvent = new AtomicReference<>();

    @RabbitListener(queues = "#{testCaptureQueue.name}")
    public void onEvent(OrderCreatedV1 event) {
      lastEvent.set(event);
    }
  }
}
