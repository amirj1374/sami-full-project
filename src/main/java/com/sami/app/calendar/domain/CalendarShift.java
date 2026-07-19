package com.sami.app.calendar.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * A working window within a day.
 *
 * <p>A lunch closure is two shifts rather than a nullable break pair, which
 * generalises to any number of gaps without schema change. A NULL
 * {@code dayOfWeek} applies the shift to every working day — the common case,
 * entered once.
 */
@Entity
@Table(name = "calendar_shifts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarShift extends BaseEntity {

    @Column(name = "calendar_id", nullable = false) private Long calendarId;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;

    /** NULL = applies to every working day of the calendar. */
    @Column(name = "day_of_week") private Short dayOfWeek;

    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;

    /** 0 = unlimited; otherwise a soft capacity hint for the scheduler. */
    @Column(name = "max_concurrent", nullable = false) private int maxConcurrent;

    @Column(name = "is_overtime", nullable = false) private boolean isOvertime;
    @Column(name = "is_bookable", nullable = false) private boolean isBookable;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;

    public boolean appliesTo(int isoDayOfWeek) {
        return dayOfWeek == null || dayOfWeek == isoDayOfWeek;
    }
}
