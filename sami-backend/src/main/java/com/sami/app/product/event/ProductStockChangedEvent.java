package com.sami.app.product.event;

import java.time.Instant;

/**
 * Synchronous compatibility event for the legacy Product stock field. Inventory
 * consumes it in the same transaction and records the corresponding ledger
 * adjustment; client-provided stock can therefore no longer bypass Inventory.
 */
public record ProductStockChangedEvent(
        Long tenantId,
        Long productId,
        int previousQuantity,
        int newQuantity,
        Long actorId,
        Instant occurredAt
) {
}
