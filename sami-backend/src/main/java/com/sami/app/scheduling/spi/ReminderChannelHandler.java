package com.sami.app.scheduling.spi;

/**
 * Delivers an appointment reminder over one channel.
 *
 * <p>Every channel row in {@code reminder_channels} is seeded inactive because
 * no notification module exists yet. This interface is the contract that module
 * will implement; until then the reminder rows are still materialised and
 * queued, so switching a channel on replays nothing and loses nothing.
 *
 * <p>Implementations must never throw to the caller: a failed reminder must not
 * roll back or invalidate the appointment it belongs to.
 */
public interface ReminderChannelHandler {

    /** Matches {@code reminder_channels.handler_key}. */
    String key();

    /** @return true when delivery succeeded; false records a retryable failure */
    boolean send(ReminderMessage message);
}
