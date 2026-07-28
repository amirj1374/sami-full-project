package com.sami.app.crm.dto;

import com.sami.app.crm.domain.CustomerEvent;

import java.time.Instant;
import java.util.Map;

/** One timeline entry. */
public record CustomerEventResponse(
        Long id,
        String eventType,
        String title,
        Map<String, Object> detail,
        String sourceModule,
        Long actorId,
        String actorEmail,
        Instant occurredAt
) {
    public static CustomerEventResponse from(CustomerEvent e) {
        return new CustomerEventResponse(e.getId(), e.getEventType(), e.getTitle(), e.getDetail(),
                e.getSourceModule(), e.getActorId(), e.getActorEmail(), e.getOccurredAt());
    }
}
