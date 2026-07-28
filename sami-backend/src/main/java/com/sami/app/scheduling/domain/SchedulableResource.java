package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Anything that can be booked: a technician, a service counter, a repair
 * bench, a meeting room, a delivery vehicle.
 *
 * <p>Named {@code SchedulableResource} rather than {@code Resource} to avoid
 * colliding with {@code jakarta.annotation.Resource} and Spring's
 * {@code org.springframework.core.io.Resource}, both of which are routinely
 * imported in this codebase — an unqualified {@code Resource} here would be a
 * standing trap.
 *
 * <p>{@code userId} links a human resource to its system account; rooms and
 * benches leave it null. {@code skills} is a JSONB tag list rather than a
 * table because this module must not grow into an HR system — when a real
 * employee module arrives it can own skills and this becomes a cache.
 */
@Entity
@Table(name = "resources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchedulableResource extends BaseEntity {

    @Column(name = "resource_code", nullable = false, length = 64) private String resourceCode;
    @Column(nullable = false, length = 255) private String name;
    @Column(length = 500) private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ResourceCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private ResourceStatus status;

    @Column(name = "company_id") private Long companyId;
    @Column(name = "branch_id") private Long branchId;

    /** Own roster; NULL inherits the branch calendar via WorkingTimeProvider. */
    @Column(name = "calendar_id") private Long calendarId;

    @Column(name = "user_id") private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private int capacity = 1;

    @Column(nullable = false) private int priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> skills;

    @Column(name = "is_bookable_online", nullable = false) private boolean isBookableOnline;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;

    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    /** Bookable only when active AND its status permits new reservations. */
    public boolean acceptsBookings() {
        return isActive && status != null && status.isAllowsBooking();
    }

    /**
     * True when this resource covers every required skill. An empty
     * requirement matches anything; a resource with no skills matches only an
     * empty requirement, so an unconfigured technician is never silently
     * treated as qualified for specialist work.
     */
    public boolean hasSkills(List<String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        return skills != null && skills.containsAll(required);
    }
}
