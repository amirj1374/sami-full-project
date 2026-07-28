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
 * A resource occupied for a span of time.
 *
 * <p>This is the row the database's exclusion constraint guards. Its window
 * INCLUDES the appointment type's preparation, cleanup and buffer minutes, so
 * it is genuinely wider than the schedule's customer-facing window — the
 * technician is busy before the customer arrives and after they leave, and the
 * overlap check must use the wider span or back-to-back repairs would leave no
 * turnaround.
 *
 * <p>{@code scheduleId} is nullable: a resource can be blocked with no
 * appointment behind it, which is how maintenance, leave and out-of-service
 * periods are represented.
 *
 * <p><b>{@code holdsResource} must mirror {@code status.blocksResource}.</b>
 * The exclusion constraint predicates on this column because a constraint
 * predicate must be immutable and cannot join to the status table. Every write
 * path goes through {@code ReservationService.applyStatus} to keep the two in
 * step; drift would either double-book or leak released slots.
 */
@Entity
@Table(name = "reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation extends BaseEntity {

    @Column(name = "reservation_number", nullable = false, length = 32) private String reservationNumber;

    @Column(name = "schedule_id") private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private SchedulableResource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private ScheduleStatus status;

    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;

    @Column(name = "duration_minutes", insertable = false, updatable = false)
    private Integer durationMinutes;

    /** Mirrors {@code status.blocksResource}; see the class comment. */
    @Column(name = "holds_resource", nullable = false)
    @Builder.Default
    private boolean holdsResource = true;

    @Column(length = 500) private String purpose;
    @Column(nullable = false) private int priority;
    @Column(name = "is_maintenance", nullable = false) private boolean isMaintenance;

    @Column(name = "customer_id") private Long customerId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "employee_user_id") private Long employeeUserId;

    @Column(name = "released_at") private Instant releasedAt;
    @Column(name = "release_reason", length = 500) private String releaseReason;

    @Column(name = "created_by") private Long createdBy;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        // Half-open [start, end), matching the database range so Java and SQL
        // agree that back-to-back bookings do not collide.
        return startsAt.isBefore(otherEnd) && otherStart.isBefore(endsAt);
    }
}
