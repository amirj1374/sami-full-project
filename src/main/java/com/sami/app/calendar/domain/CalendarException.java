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
 * A date-specific override of the weekly template.
 *
 * <p>Distinct from a holiday because an exception can make a non-working day
 * WORKABLE — extra Friday trading before Nowruz — which a holiday can never do.
 * It therefore takes the highest precedence in {@code WorkingTimeService}:
 * exception &gt; holiday &gt; weekly template.
 */
@Entity
@Table(name = "calendar_exceptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarException extends BaseEntity {

    @Column(name = "calendar_id", nullable = false) private Long calendarId;
    @Column(name = "exception_date", nullable = false) private LocalDate exceptionDate;
    @Column(nullable = false, length = 255) private String name;
    @Column(length = 500) private String reason;

    @Column(name = "is_working_day", nullable = false) private boolean isWorkingDay;

    /** NULL on a working exception means "use the calendar's normal shifts". */
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;

    @Column(name = "blocks_appointments", nullable = false) private boolean blocksAppointments;
    @Column(name = "is_active", nullable = false) private boolean isActive;

    @Column(name = "created_by") private Long createdBy;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean hasCustomHours() {
        return startTime != null && endTime != null;
    }
}
