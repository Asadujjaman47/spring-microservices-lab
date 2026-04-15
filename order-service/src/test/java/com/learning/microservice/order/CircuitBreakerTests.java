package com.learning.microservice.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import com.learning.microservice.order.client.ProductStockClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// Exercises the @CircuitBreaker aspect end-to-end. Replaces the @LoadBalanced RestClient.Builder
// with one whose ClientHttpRequestFactory always throws, simulating product-service being down —
// after the configured window of failures (sliding-window-size=4, min-calls=2) the breaker opens
// and the fallback kicks in.
@Import(CircuitBreakerTests.FailingRestClientConfig.class)
class CircuitBreakerTests extends AbstractIntegrationTest {

  @Autowired ProductStockClient client;
  @Autowired CircuitBreakerRegistry registry;

  @Test
  void openCircuit_invokesFallback_withStockReservationFailed() {
    CircuitBreaker cb = registry.circuitBreaker(ProductStockClient.CB_NAME);
    cb.reset();

    // Drive enough failures to trip the breaker, then verify subsequent calls fast-fail via the
    // fallback path (which maps network failures to ORDER_STOCK_RESERVATION_FAILED).
    for (int i = 0; i < 5; i++) {
      try {
        client.reserve(UUID.randomUUID(), 1);
      } catch (BusinessException ignored) {
        // Expected — every call goes through the fallback.
      }
    }

    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    assertThatThrownBy(() -> client.reserve(UUID.randomUUID(), 1))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.ORDER_STOCK_RESERVATION_FAILED));
  }

  @TestConfiguration
  static class FailingRestClientConfig {

    @Bean
    @LoadBalanced
    @Primary
    RestClient.Builder failingBuilder() {
      return RestClient.builder()
          .requestFactory(
              (uri, httpMethod) -> {
                throw new ResourceAccessException("simulated product-service outage");
              });
    }
  }
}
