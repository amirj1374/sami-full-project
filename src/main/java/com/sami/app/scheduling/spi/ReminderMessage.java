package com.sami.app.scheduling.spi;

import java.time.Instant;

/** The payload handed to a {@link ReminderChannelHandler}. */
public record ReminderMessage(
        Long scheduleId,
        String scheduleNumber,
        String recipient,
        String templateCode,
        String title,
        Instant appointmentStart,
        String branchName
) { }
