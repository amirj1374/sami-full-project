package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A materialised reminder awaiting delivery.
 *
 * <p>Rows are created when the appointment is booked, even though every
 * channel is currently inactive. That way switching a channel on starts
 * delivering future reminders immediately, with no backfill step.
 */
@Entity @Table(name = "schedule_reminders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleReminder extends BaseEntity {
    @Column(name = "schedule_id", nullable = false) private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private ReminderChannel channel;

    @Column(name = "scheduled_for", nullable = false) private Instant scheduledFor;
    @Column(name = "sent_at") private Instant sentAt;

    /** PENDING | SENT | FAILED | SKIPPED | CANCELLED */
    @Column(name = "delivery_state", nullable = false, length = 32)
    @Builder.Default
    private String deliveryState = "PENDING";

    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(length = 255) private String recipient;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
