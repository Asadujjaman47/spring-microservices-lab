package com.learning.microservice.product.web.dto;

import jakarta.validation.constraints.Positive;

public record ReserveStockRequest(@Positive int quantity) {}
