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
 * A standing request for a slot that was not free.
 *
 * <p>Promotion records itself rather than deleting the row, so "how long did
 * customers wait" and "how often did we recover a cancellation" stay
 * answerable — the spec's Reservation History and no-show reporting both
 * depend on it.
 */
@Entity @Table(name = "waiting_list_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WaitingListEntry extends BaseEntity {
    @Column(name = "entry_number", nullable = false, length = 32) private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_type_id", nullable = false)
    private AppointmentType appointmentType;

    @Column(name = "customer_id") private Long customerId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "branch_id") private Long branchId;
    /** NULL means any suitable resource will do. */
    @Column(name = "resource_id") private Long resourceId;

    @Column(name = "desired_from", nullable = false) private Instant desiredFrom;
    @Column(name = "desired_to", nullable = false) private Instant desiredTo;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;

    @Column(nullable = false) private int priority;
    @Column(length = 500) private String notes;

    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "promoted_at") private Instant promotedAt;
    @Column(name = "promoted_schedule_id") private Long promotedScheduleId;
    @Column(name = "promoted_manually", nullable = false) private boolean promotedManually;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;

    @Column(name = "created_by") private Long createdBy;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /** True when the offered window sits inside what the requester accepts. */
    public boolean accepts(Instant start, Instant end) {
        return !start.isBefore(desiredFrom) && !end.isAfter(desiredTo);
    }
}
