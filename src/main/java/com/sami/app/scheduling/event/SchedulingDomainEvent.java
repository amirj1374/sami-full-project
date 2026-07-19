package com.sami.app.scheduling.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The module's published business event. Consumers subscribe with a plain
 * Spring {@code @EventListener}; automation reaches it via DomainEventBridge.
 *
 * <p>Publishing these is how other modules react to scheduling without the
 * scheduler knowing they exist — a repair module can open a work order on
 * CustomerCheckedIn without this module importing anything from it.
 */
public record SchedulingDomainEvent(
        String eventId,
        String eventType,
        Long scheduleId,
        String scheduleNumber,
        String moduleCode,
        Long resourceId,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public static final String APPOINTMENT_CREATED = "AppointmentCreated";
    public static final String APPOINTMENT_CONFIRMED = "AppointmentConfirmed";
    public static final String APPOINTMENT_RESCHEDULED = "AppointmentRescheduled";
    public static final String APPOINTMENT_CANCELLED = "AppointmentCancelled";
    public static final String APPOINTMENT_COMPLETED = "AppointmentCompleted";
    public static final String APPOINTMENT_NO_SHOW = "AppointmentNoShow";
    public static final String RESOURCE_RESERVED = "ResourceReserved";
    public static final String RESOURCE_RELEASED = "ResourceReleased";
    public static final String CUSTOMER_CHECKED_IN = "CustomerCheckedIn";
    public static final String CUSTOMER_CHECKED_OUT = "CustomerCheckedOut";
    public static final String WAITING_LIST_ADDED = "WaitingListAdded";
    public static final String WAITING_LIST_PROMOTED = "WaitingListPromoted";

    public static SchedulingDomainEvent of(String eventType, Long scheduleId, String scheduleNumber,
                                           String moduleCode, Long resourceId,
                                           Map<String, Object> payload) {
        return new SchedulingDomainEvent(UUID.randomUUID().toString(), eventType, scheduleId,
                scheduleNumber, moduleCode, resourceId,
                payload == null ? Map.of() : payload, Instant.now());
    }
}
