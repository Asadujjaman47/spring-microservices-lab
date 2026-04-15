package com.learning.microservice.order.client;

import java.util.UUID;

/**
 * Local view of product-service's {@code StockReservationResponse} — duplicated here so
 * order-service does not depend on product-service's module. Reordered/renamed fields on either
 * side are caught at the first integration run.
 */
public record ProductStockReservation(
    UUID productId, int reservedQuantity, int remainingStock, long unitPriceCents) {}
