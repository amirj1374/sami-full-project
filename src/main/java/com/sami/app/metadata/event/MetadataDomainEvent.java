package com.sami.app.metadata.event;

import java.time.Instant;
import java.util.Map;

/** Published on metadata changes so caches, search indexes and automation react. */
public record MetadataDomainEvent(
        String eventId,
        String eventType,
        String moduleCode,
        String entityCode,
        Long entityId,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static final String FIELD_CREATED = "CustomFieldCreated";
    public static final String FIELD_UPDATED = "CustomFieldUpdated";
    public static final String FIELD_DELETED = "CustomFieldDeleted";
    public static final String FORM_PUBLISHED = "FormVersionPublished";
    public static final String LAYOUT_CHANGED = "FormLayoutChanged";
    public static final String VALUES_UPDATED = "CustomFieldValuesUpdated";
}
