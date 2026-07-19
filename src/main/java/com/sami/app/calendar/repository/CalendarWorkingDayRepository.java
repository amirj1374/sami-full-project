package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.CalendarWorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarWorkingDayRepository extends JpaRepository<CalendarWorkingDay, Long> {
    List<CalendarWorkingDay> findByCalendarIdOrderByDayOfWeekAsc(Long calendarId);
    Optional<CalendarWorkingDay> findByCalendarIdAndDayOfWeek(Long calendarId, short dayOfWeek);
    void deleteByCalendarId(Long calendarId);
}
