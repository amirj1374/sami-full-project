package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What kind of appointment this is, and every rule that follows from it.
 *
 * <p>This is where the module's configurability concentrates: durations,
 * buffers, notice periods, self-service eligibility and no-show thresholds are
 * all per-type data. Adding "Video Consultation" is an INSERT.
 *
 * <p>{@code subjectProviderKey} binds the type to a {@code
 * ScheduleSubjectProvider} bean. A type naming a provider that has no bean —
 * every repair and trade-in type today — still books normally and simply
 * carries no linked business record, which is what lets these be configured
 * before the modules that own them exist.
 */
@Entity
@Table(name = "appointment_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentType extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ScheduleCategory category;

    @Column(name = "default_duration_minutes", nullable = false) private int defaultDurationMinutes;
    @Column(name = "min_duration_minutes", nullable = false) private int minDurationMinutes;
    @Column(name = "max_duration_minutes", nullable = false) private int maxDurationMinutes;

    /** Resource-only time before the customer arrives. */
    @Column(name = "preparation_minutes", nullable = false) private int preparationMinutes;
    /** Resource-only time after the customer leaves. */
    @Column(name = "cleanup_minutes", nullable = false) private int cleanupMinutes;
    @Column(name = "buffer_before_minutes", nullable = false) private int bufferBeforeMinutes;
    @Column(name = "buffer_after_minutes", nullable = false) private int bufferAfterMinutes;

    @Column(name = "requires_customer", nullable = false) private boolean requiresCustomer;
    @Column(name = "requires_supplier", nullable = false) private boolean requiresSupplier;
    @Column(name = "requires_resource", nullable = false) private boolean requiresResource;
    @Column(name = "requires_approval", nullable = false) private boolean requiresApproval;
    @Column(name = "allows_waiting_list", nullable = false) private boolean allowsWaitingList;
    @Column(name = "allows_self_service", nullable = false) private boolean allowsSelfService;
    @Column(name = "allows_overbooking", nullable = false) private boolean allowsOverbooking;
    @Column(name = "enforce_working_hours", nullable = false) private boolean enforceWorkingHours;

    @Column(name = "max_advance_days", nullable = false) private int maxAdvanceDays;
    @Column(name = "min_notice_minutes", nullable = false) private int minNoticeMinutes;
    @Column(name = "cancellation_notice_minutes", nullable = false) private int cancellationNoticeMinutes;

    @Column(name = "late_threshold_minutes", nullable = false) private int lateThresholdMinutes;
    @Column(name = "no_show_threshold_minutes", nullable = false) private int noShowThresholdMinutes;

    @Column(name = "subject_provider_key", length = 64) private String subjectProviderKey;
    @Column(name = "module_code", length = 64) private String moduleCode;
    @Column(length = 16) private String color;

    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;

    /**
     * Minutes the RESOURCE is occupied for an appointment of the given
     * customer-facing length. Buffers and prep/cleanup are resource time, not
     * customer time, so the overlap check must use this — not the duration the
     * customer sees — or back-to-back repairs would leave no bench turnaround.
     */
    public int resourceMinutesFor(int customerMinutes) {
        return preparationMinutes + bufferBeforeMinutes
                + customerMinutes
                + cleanupMinutes + bufferAfterMinutes;
    }

    /** Minutes the resource is held BEFORE the customer-facing start. */
    public int leadInMinutes() {
        return preparationMinutes + bufferBeforeMinutes;
    }

    /** Minutes the resource is held AFTER the customer-facing end. */
    public int leadOutMinutes() {
        return cleanupMinutes + bufferAfterMinutes;
    }

    public boolean permitsDuration(int minutes) {
        return minutes >= minDurationMinutes && minutes <= maxDurationMinutes;
    }
}
