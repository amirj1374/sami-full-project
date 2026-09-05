package com.sami.app.treasury.event;

import java.time.Instant;
import java.util.Map;

/** Process-local integration boundary; it does not create accounting postings. */
public record TreasuryDomainEvent(Long tenantId, String eventType, String entityType, Long entityId,
                                  Map<String, Object> detail, Long actorId, Instant occurredAt) { }
