package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * An appointment: the business-facing booking.
 *
 * <p>A schedule is what a customer or supplier has agreed to. The resources it
 * consumes are separate {@link Reservation} rows — one per resource — because a
 * repair intake occupies a technician AND a bench, and each must be checked for
 * overlap independently.
 *
 * <p>The link to the originating business record ({@code moduleCode},
 * {@code relatedEntityType}, {@code relatedEntityId}) is deliberately NOT a
 * foreign key. The repair and trade-in modules do not exist yet, and when they
 * do this module must not acquire a compile-time dependency on them — the
 * {@code ScheduleSubjectProvider} SPI resolves the reference instead.
 */
@Entity
@Table(name = "schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Schedule extends BaseEntity {

    @Column(name = "schedule_number", nullable = false, length = 32) private String scheduleNumber;
    @Column(nullable = false, length = 255) private String title;
    @Column(length = 2000) private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ScheduleCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_type_id", nullable = false)
    private AppointmentType appointmentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private ScheduleStatus status;

    @Column(name = "company_id") private Long companyId;
    @Column(name = "branch_id") private Long branchId;
    @Column(name = "calendar_id") private Long calendarId;

    @Column(name = "module_code", length = 64) private String moduleCode;
    @Column(name = "related_entity_type", length = 64) private String relatedEntityType;
    @Column(name = "related_entity_id") private Long relatedEntityId;

    @Column(name = "customer_id") private Long customerId;
    @Column(name = "supplier_id") private Long supplierId;

    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;

    /**
     * Maintained by the database as a stored generated column, so it can never
     * disagree with the window. Read-only to JPA for exactly that reason.
     */
    @Column(name = "duration_minutes", insertable = false, updatable = false)
    private Integer durationMinutes;

    @Column(nullable = false) private int priority;

    @Column(name = "checked_in_at") private Instant checkedInAt;
    @Column(name = "checked_out_at") private Instant checkedOutAt;
    @Column(name = "arrived_late", nullable = false) private boolean arrivedLate;
    @Column(name = "late_by_minutes") private Integer lateByMinutes;
    @Column(name = "completion_notes", length = 2000) private String completionNotes;

    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;
    @Column(name = "cancelled_by") private Long cancelledBy;

    @Column(name = "confirmed_at") private Instant confirmedAt;

    /** STAFF | PORTAL | MOBILE | API — reporting treats self-service differently. */
    @Column(name = "source_channel", nullable = false, length = 32)
    @Builder.Default
    private String sourceChannel = "STAFF";

    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_by_email", length = 255) private String createdByEmail;

    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isCancelled() {
        return status != null && status.isCancelledState();
    }

    public boolean isEditable() {
        return status != null && status.isAllowsEdit();
    }

    /**
     * Whether an arrival at {@code arrivalTime} counts as late for this type.
     * The threshold is per-type configuration, never a constant.
     */
    public boolean isLateArrival(Instant arrivalTime) {
        if (appointmentType == null) {
            return false;
        }
        long minutesLate = Duration.between(startsAt, arrivalTime).toMinutes();
        return minutesLate > appointmentType.getLateThresholdMinutes();
    }

    /** Minutes past the start time, floored at zero for an early arrival. */
    public int minutesLate(Instant arrivalTime) {
        return (int) Math.max(0, Duration.between(startsAt, arrivalTime).toMinutes());
    }
}
