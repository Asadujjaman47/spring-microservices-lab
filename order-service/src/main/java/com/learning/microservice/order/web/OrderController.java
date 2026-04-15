package com.learning.microservice.order.web;

import com.learning.microservice.common.api.ApiResponse;
import com.learning.microservice.order.service.OrderService;
import com.learning.microservice.order.web.dto.CreateOrderRequest;
import com.learning.microservice.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orders;

  public OrderController(OrderService orders) {
    this.orders = orders;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
    return ApiResponse.ok(orders.place(req), "Order created");
  }

  @GetMapping
  public ApiResponse<List<OrderResponse>> list() {
    return ApiResponse.ok(orders.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<OrderResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(orders.getById(id));
  }
}
