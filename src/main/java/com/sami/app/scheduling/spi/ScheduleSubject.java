package com.sami.app.scheduling.spi;

/** A business record an appointment refers to, as the scheduler sees it. */
public record ScheduleSubject(
        String entityType,
        Long entityId,
        String reference,
        String title,
        String statusLabel
) { }
