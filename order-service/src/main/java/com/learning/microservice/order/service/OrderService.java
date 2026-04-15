package com.learning.microservice.order.service;

import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import com.learning.microservice.common.event.OrderCreatedV1;
import com.learning.microservice.common.id.UuidV7;
import com.learning.microservice.order.client.ProductStockClient;
import com.learning.microservice.order.client.ProductStockReservation;
import com.learning.microservice.order.domain.Order;
import com.learning.microservice.order.domain.OrderRepository;
import com.learning.microservice.order.domain.OrderStatus;
import com.learning.microservice.order.web.dto.CreateOrderRequest;
import com.learning.microservice.order.web.dto.OrderResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

  private final OrderRepository orders;
  private final ProductStockClient stockClient;
  private final ApplicationEventPublisher applicationEvents;

  public OrderService(
      OrderRepository orders,
      ProductStockClient stockClient,
      ApplicationEventPublisher applicationEvents) {
    this.orders = orders;
    this.stockClient = stockClient;
    this.applicationEvents = applicationEvents;
  }

  public OrderResponse place(CreateOrderRequest req) {
    ProductStockReservation reservation = stockClient.reserve(req.productId(), req.quantity());

    Order order = new Order();
    order.setUserId(req.userId());
    order.setProductId(req.productId());
    order.setQuantity(req.quantity());
    order.setUnitPriceCents(reservation.unitPriceCents());
    order.setTotalPriceCents(reservation.unitPriceCents() * req.quantity());
    order.setStatus(OrderStatus.CREATED);
    Order saved = orders.save(order);

    // Publish AFTER commit so a rollback can't leak a ghost event to consumers.
    applicationEvents.publishEvent(
        new OrderCreatedV1(
            UuidV7.generate(),
            Instant.now(),
            saved.getId(),
            saved.getUserId(),
            saved.getProductId(),
            saved.getQuantity(),
            saved.getTotalPriceCents()));

    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public OrderResponse getById(UUID id) {
    return orders
        .findById(id)
        .map(OrderService::toResponse)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> list() {
    return orders.findAll().stream().map(OrderService::toResponse).toList();
  }

  private static OrderResponse toResponse(Order o) {
    return new OrderResponse(
        o.getId(),
        o.getUserId(),
        o.getProductId(),
        o.getQuantity(),
        o.getUnitPriceCents(),
        o.getTotalPriceCents(),
        o.getStatus(),
        o.getCreatedAt());
  }
}
