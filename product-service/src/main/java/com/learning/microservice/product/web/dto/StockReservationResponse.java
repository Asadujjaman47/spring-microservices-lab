package com.learning.microservice.product.web.dto;

import java.util.UUID;

public record StockReservationResponse(
    UUID productId, int reservedQuantity, int remainingStock, long unitPriceCents) {}
