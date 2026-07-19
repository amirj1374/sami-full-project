package com.sami.app.calendar.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;

/**
 * A named working-time definition, scoped to a company and optionally a branch.
 *
 * <p>A NULL {@code branchId} means "every branch of this company that has no
 * calendar of its own" — resolution order (branch → company → tenant default)
 * lives in {@code CalendarResolver} so no caller repeats it.
 */
@Entity
@Table(name = "business_calendars")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessCalendar extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 255) private String name;
    @Column(length = 500) private String description;

    @Column(name = "company_id") private Long companyId;
    @Column(name = "branch_id") private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_system_id", nullable = false)
    private CalendarSystemDef calendarSystem;

    @Column(nullable = false, length = 64)
    @Builder.Default
    private String timezone = "Asia/Tehran";

    @Column(name = "slot_minutes", nullable = false)
    @Builder.Default
    private int slotMinutes = 30;

    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;

    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    /**
     * Every instant-to-local conversion in the scheduler goes through this, so
     * a branch in a different timezone cannot be booked against the wrong
     * working day.
     */
    public ZoneId zone() {
        return ZoneId.of(timezone);
    }
}
