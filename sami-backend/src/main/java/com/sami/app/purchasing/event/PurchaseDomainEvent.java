package com.sami.app.purchasing.event;

import java.time.Instant;
import java.util.Map;

/**
 * The purchasing module's published business event (SUBMITTED, APPROVED,
 * RECEIVED, COMPLETED, CANCELLED, RETURNED, …). Inventory and accounting
 * subscribe to these — purchasing never writes their data:
 *
 * <pre>
 * &#64;TransactionalEventListener
 * public void on(PurchaseDomainEvent event) {
 *     if ("RECEIVED".equals(event.action())) { /* update stock *&#47; }
 * }
 * </pre>
 */
public record PurchaseDomainEvent(
        Long tenantId,
        Long purchaseId,
        String purchaseNumber,
        String action,
        Map<String, Object> detail,
        Instant occurredAt
) {
}
