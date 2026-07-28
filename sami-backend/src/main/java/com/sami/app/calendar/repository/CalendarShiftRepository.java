package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.CalendarShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarShiftRepository extends JpaRepository<CalendarShift, Long> {
    List<CalendarShift> findByCalendarIdAndIsActiveTrueOrderByDisplayOrderAscStartTimeAsc(Long calendarId);
    List<CalendarShift> findByCalendarIdOrderByDisplayOrderAsc(Long calendarId);
}
