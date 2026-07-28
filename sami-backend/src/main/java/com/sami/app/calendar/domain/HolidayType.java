package com.sami.app.calendar.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Holiday classification.
 *
 * <p>{@code isWorkingDay} and {@code blocksAppointments} are deliberately
 * separable: a stocktake day is worked but accepts no customer bookings, and a
 * commemorative observance is the reverse. Services read the flags, never the
 * code.
 */
@Entity
@Table(name = "holiday_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HolidayType extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "is_working_day", nullable = false) private boolean isWorkingDay;
    @Column(name = "blocks_appointments", nullable = false) private boolean blocksAppointments;
    @Column(name = "is_paid", nullable = false) private boolean isPaid;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
