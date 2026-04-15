package com.learning.microservice.order.client;

import com.learning.microservice.common.api.ApiResponse;
import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductStockClient {

  private static final Logger log = LoggerFactory.getLogger(ProductStockClient.class);
  public static final String CB_NAME = "productService";

  private final RestClient client;

  public ProductStockClient(RestClient.Builder loadBalancedBuilder) {
    this.client = loadBalancedBuilder.baseUrl("http://product-service").build();
  }

  // Wrapped in a Resilience4j circuit breaker — on repeated failures the breaker opens and
  // fallback() is invoked immediately, giving the caller a fast degraded response instead of
  // cascading latency. Business errors (PRODUCT_OUT_OF_STOCK / _NOT_FOUND) are re-thrown as-is
  // so they don't trip the breaker or hit the fallback.
  @CircuitBreaker(name = CB_NAME, fallbackMethod = "fallback")
  public ProductStockReservation reserve(UUID productId, int quantity) {
    try {
      ApiResponse<ProductStockReservation> envelope =
          client
              .post()
              .uri("/api/v1/products/{id}/reservations", productId)
              .body(new ReserveStockRequest(quantity))
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (envelope == null || envelope.data() == null) {
        throw new IllegalStateException("Empty reservation response from product-service");
      }
      return envelope.data();
    } catch (RestClientResponseException ex) {
      throw translate(ex);
    }
  }

  // Breaker fallback — signature must match reserve(...) + trailing Throwable param.
  @SuppressWarnings("unused")
  private ProductStockReservation fallback(UUID productId, int quantity, Throwable cause) {
    // BusinessException from translate(...) propagates straight through Resilience4j without
    // being wrapped, so domain errors stay domain errors.
    if (cause instanceof BusinessException be) {
      throw be;
    }
    log.warn(
        "product-service unavailable for reservation of product={} qty={}: {}",
        productId,
        quantity,
        cause.toString());
    throw new BusinessException(
        ErrorCode.ORDER_STOCK_RESERVATION_FAILED,
        "Stock reservation temporarily unavailable, please retry");
  }

  private BusinessException translate(RestClientResponseException ex) {
    try {
      ApiResponse<?> body =
          ex.getResponseBodyAs(new ParameterizedTypeReference<ApiResponse<?>>() {});
      if (body != null && body.errorCode() != null) {
        return new BusinessException(
            body.errorCode(), body.message() != null ? body.message() : ex.getMessage());
      }
    } catch (Exception ignored) {
      // fall through to generic mapping
    }
    return new BusinessException(
        ErrorCode.ORDER_STOCK_RESERVATION_FAILED,
        "Unexpected response from product-service: " + ex.getStatusCode());
  }

  public record ReserveStockRequest(int quantity) {}
}
