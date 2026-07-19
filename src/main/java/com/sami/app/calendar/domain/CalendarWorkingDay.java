package com.sami.app.calendar.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One weekday of a calendar's weekly template.
 *
 * <p>{@code dayOfWeek} uses ISO-8601 numbering (1 = Monday … 7 = Sunday), which
 * is exactly {@code java.time.DayOfWeek.getValue()} — chosen so there is no
 * translation layer that could drift.
 */
@Entity
@Table(name = "calendar_working_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarWorkingDay extends BaseEntity {

    @Column(name = "calendar_id", nullable = false) private Long calendarId;
    @Column(name = "day_of_week", nullable = false) private short dayOfWeek;
    @Column(name = "is_working_day", nullable = false) private boolean isWorkingDay;
}
