package com.sami.app.inventory.event;

import java.time.Instant;
import java.util.Map;

/** Process-local Inventory event published after a tenant-scoped state change. */
public record InventoryDomainEvent(
        Long tenantId,
        String eventType,
        String entityType,
        Long entityId,
        Map<String, Object> detail,
        Long actorId,
        Instant occurredAt
) {
}
