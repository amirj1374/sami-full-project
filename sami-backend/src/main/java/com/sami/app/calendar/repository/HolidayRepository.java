package com.sami.app.calendar.repository;

import com.sami.app.calendar.domain.Holiday;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * Every holiday that could affect the given calendar: its own plus the
     * tenant-wide ones (NULL calendar_id).
     *
     * <p>Both fixed-date and recurring rows are returned unfiltered by date —
     * a recurrence cannot be matched in SQL because resolving 1 Farvardin to a
     * Gregorian day requires the chronology SPI. {@code HolidayResolver} does
     * the date matching in Java. The row count per tenant is small (tens), so
     * this is cheap and, unlike a SQL date filter, it cannot be silently wrong.
     */
    @EntityGraph(attributePaths = {"holidayType"})
    @Query("""
            SELECT h FROM Holiday h
            WHERE h.isActive = TRUE
              AND (h.calendarId = :calendarId OR h.calendarId IS NULL)
            """)
    List<Holiday> findApplicable(@Param("calendarId") Long calendarId);

    @EntityGraph(attributePaths = {"holidayType"})
    List<Holiday> findByCalendarIdOrderByHolidayDateAsc(Long calendarId);
}
