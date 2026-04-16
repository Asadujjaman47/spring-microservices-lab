package com.learning.microservice.product.web.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PatchProductRequest(
    @Size(min = 1, max = 255) String name,
    @Size(max = 5000) String description,
    @PositiveOrZero Long priceCents,
    @PositiveOrZero Integer stock) {}
