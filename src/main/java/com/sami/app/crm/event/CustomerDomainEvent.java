package com.sami.app.crm.event;

import java.time.Instant;
import java.util.Map;

/**
 * The CRM's published business event, fired for every timeline entry
 * (CustomerCreated, CustomerUpdated, CustomerMerged, CustomerBlacklisted, …).
 *
 * <p>Other modules subscribe with a plain Spring listener and stay fully
 * decoupled from CRM internals:
 *
 * <pre>
 * &#64;EventListener
 * public void on(CustomerDomainEvent event) {
 *     if ("BLACKLISTED".equals(event.eventType())) { ... }
 * }
 * </pre>
 *
 * <p>Events are published inside the originating transaction; subscribers that
 * must only act on committed data use {@code @TransactionalEventListener}.
 */
public record CustomerDomainEvent(
        Long customerId,
        String eventType,
        String title,
        Map<String, Object> detail,
        String sourceModule,
        Instant occurredAt
) {
}
