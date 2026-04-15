package com.learning.microservice.order.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID userId, @NotNull UUID productId, @Positive int quantity) {}
