package com.sami.app.supplier.event;

import java.time.Instant;
import java.util.Map;

/**
 * The supplier module's published business event (CREATED, UPDATED, RATED,
 * STATUS_CHANGED, ARCHIVED, DELETED, …). Other modules subscribe with Spring
 * listeners; nothing depends on supplier internals.
 */
public record SupplierDomainEvent(
        Long tenantId,
        Long supplierId,
        String supplierCode,
        String action,
        Map<String, Object> detail,
        Instant occurredAt
) {
}
