package com.learning.microservice.product.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 5000) String description,
    @PositiveOrZero long priceCents,
    @PositiveOrZero int stock) {}
