package com.sami.app.calendar.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A non-working (or restricted) day.
 *
 * <p>Either a fixed Gregorian {@code holidayDate}, or an annual recurrence
 * expressed in the chronology named by {@code recurrenceSystemId}. Recurrences
 * must be stored as month/day rather than a resolved date because 1 Farvardin
 * lands on a different Gregorian day every year — storing the date would be
 * correct in year one and wrong thereafter.
 *
 * <p>A NULL {@code calendarId} applies the holiday to every calendar in the
 * tenant, so national holidays are entered once.
 */
@Entity
@Table(name = "holidays")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Holiday extends BaseEntity {

    @Column(name = "calendar_id") private Long calendarId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holiday_type_id", nullable = false)
    private HolidayType holidayType;

    @Column(nullable = false, length = 255) private String name;
    @Column(length = 500) private String description;

    @Column(name = "holiday_date") private LocalDate holidayDate;

    @Column(name = "is_recurring", nullable = false) private boolean isRecurring;
    @Column(name = "recurrence_system_id") private Long recurrenceSystemId;
    @Column(name = "recurrence_month") private Short recurrenceMonth;
    @Column(name = "recurrence_day") private Short recurrenceDay;

    /** Multi-day observances (Nowruz spans 1–4 Farvardin) without one row each. */
    @Column(name = "duration_days", nullable = false)
    @Builder.Default
    private int durationDays = 1;

    @Column(name = "half_day_start") private LocalTime halfDayStart;
    @Column(name = "half_day_end") private LocalTime halfDayEnd;

    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isHalfDay() {
        return halfDayStart != null && halfDayEnd != null;
    }
}
