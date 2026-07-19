package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.CalendarException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarExceptionRepository extends JpaRepository<CalendarException, Long> {
    Optional<CalendarException> findByCalendarIdAndExceptionDateAndIsActiveTrue(Long calendarId, LocalDate date);
    List<CalendarException> findByCalendarIdAndExceptionDateBetweenAndIsActiveTrue(
            Long calendarId, LocalDate from, LocalDate to);
}
