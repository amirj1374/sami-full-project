package com.sami.app.calendar.service;

import com.sami.app.calendar.api.DaySchedule;
import com.sami.app.calendar.api.WorkingTimeProvider;
import com.sami.app.calendar.api.WorkingWindow;
import com.sami.app.calendar.domain.BusinessCalendar;
import com.sami.app.calendar.domain.CalendarException;
import com.sami.app.calendar.domain.CalendarShift;
import com.sami.app.calendar.domain.CalendarWorkingDay;
import com.sami.app.calendar.domain.Holiday;
import com.sami.app.calendar.repository.BusinessCalendarRepository;
import com.sami.app.calendar.repository.CalendarExceptionRepository;
import com.sami.app.calendar.repository.CalendarShiftRepository;
import com.sami.app.calendar.repository.CalendarSystemDefRepository;
import com.sami.app.calendar.repository.CalendarWorkingDayRepository;
import com.sami.app.calendar.repository.HolidayRepository;
import com.sami.app.calendar.spi.CalendarSystem;
import com.sami.app.calendar.spi.CalendarSystemRegistry;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Resolves working time. The single authority for "is the business open".
 *
 * <p><b>Precedence, highest first:</b> a dated exception, then a holiday, then
 * the weekly template. An exception outranks a holiday because it is the only
 * construct that can ADD working time to a day that would otherwise be closed.
 *
 * <p><b>Working vs bookable.</b> These are separate throughout. A stocktake day
 * is worked but accepts no appointments; an observance may be the reverse.
 * Collapsing them would let customers book into a closed shop.
 */
@Service
@RequiredArgsConstructor
public class WorkingTimeService implements WorkingTimeProvider {

    /** Guard against an all-holidays calendar turning a search into a hang. */
    private static final int MAX_DAY_SCAN = 800;

    private final BusinessCalendarRepository calendarRepository;
    private final CalendarWorkingDayRepository workingDayRepository;
    private final CalendarShiftRepository shiftRepository;
    private final CalendarExceptionRepository exceptionRepository;
    private final HolidayRepository holidayRepository;
    private final CalendarSystemDefRepository systemRepository;
    private final CalendarSystemRegistry systemRegistry;
    private final HolidayResolver holidayResolver;

    // -----------------------------------------------------------------
    // Resolution
    // -----------------------------------------------------------------

    /**
     * Branch calendar → company calendar → tenant default. Performed here once
     * so that no caller invents its own fallback order.
     */
    @Override
    @Transactional(readOnly = true)
    public Long resolveCalendarId(Long companyId, Long branchId) {
        if (branchId != null) {
            Optional<BusinessCalendar> byBranch =
                    calendarRepository.findFirstByBranchIdAndIsActiveTrue(branchId);
            if (byBranch.isPresent()) {
                return byBranch.get().getId();
            }
        }
        if (companyId != null) {
            Optional<BusinessCalendar> byCompany =
                    calendarRepository.findFirstByCompanyIdAndBranchIdIsNullAndIsActiveTrue(companyId);
            if (byCompany.isPresent()) {
                return byCompany.get().getId();
            }
        }
        return calendarRepository.findFirstByIsDefaultTrueAndIsActiveTrue()
                .map(BusinessCalendar::getId)
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No active business calendar is configured"));
    }

    @Transactional(readOnly = true)
    public BusinessCalendar require(Long calendarId) {
        return calendarRepository.findByIdAndIsActiveTrue(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business calendar %d not found or inactive".formatted(calendarId)));
    }

    public ZoneId zoneOf(Long calendarId) {
        return require(calendarId).zone();
    }

    // -----------------------------------------------------------------
    // Day resolution
    // -----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public DaySchedule scheduleFor(Long calendarId, LocalDate date) {
        return scheduleForRange(calendarId, date, date).get(0);
    }

    /**
     * Range form exists because the per-date form would issue three queries per
     * day; a month view would be ninety. Everything is loaded once here and
     * matched in memory.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DaySchedule> scheduleForRange(Long calendarId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Range end precedes its start");
        }
        require(calendarId);

        Map<Short, Boolean> template = new HashMap<>();
        for (CalendarWorkingDay d : workingDayRepository.findByCalendarIdOrderByDayOfWeekAsc(calendarId)) {
            template.put(d.getDayOfWeek(), d.isWorkingDay());
        }
        List<CalendarShift> shifts =
                shiftRepository.findByCalendarIdAndIsActiveTrueOrderByDisplayOrderAscStartTimeAsc(calendarId);
        List<Holiday> holidays = holidayRepository.findApplicable(calendarId);
        Map<LocalDate, CalendarException> exceptions = new HashMap<>();
        for (CalendarException e : exceptionRepository
                .findByCalendarIdAndExceptionDateBetweenAndIsActiveTrue(calendarId, from, to)) {
            exceptions.put(e.getExceptionDate(), e);
        }

        Function<Long, CalendarSystem> systemLookup = cachedSystemLookup();

        List<DaySchedule> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            result.add(resolveDay(date, template, shifts, holidays,
                    exceptions.get(date), systemLookup));
        }
        return result;
    }

    private DaySchedule resolveDay(LocalDate date, Map<Short, Boolean> template,
                                   List<CalendarShift> shifts, List<Holiday> holidays,
                                   CalendarException exception,
                                   Function<Long, CalendarSystem> systemLookup) {

        // 1. A dated exception wins outright — it is the only thing that can
        //    open a day the template closes.
        if (exception != null) {
            if (!exception.isWorkingDay()) {
                return DaySchedule.closed(date, exception.getName());
            }
            List<WorkingWindow> windows = exception.hasCustomHours()
                    ? List.of(new WorkingWindow("EXCEPTION", exception.getName(),
                            exception.getStartTime(), exception.getEndTime(), 0, false))
                    : windowsFor(date, shifts);
            return new DaySchedule(date, true, exception.isBlocksAppointments(),
                    windows, exception.getName());
        }

        // 2. Holidays.
        Optional<Holiday> holiday = holidayResolver.match(date, holidays, systemLookup);
        if (holiday.isPresent()) {
            Holiday h = holiday.get();
            boolean worked = h.getHolidayType().isWorkingDay();
            if (h.isHalfDay()) {
                // A half day is worked regardless of the type's default: the
                // explicit hours are the more specific statement.
                return new DaySchedule(date, true, h.getHolidayType().isBlocksAppointments(),
                        List.of(new WorkingWindow("HALF_DAY", h.getName(),
                                h.getHalfDayStart(), h.getHalfDayEnd(), 0, false)),
                        h.getName());
            }
            if (!worked) {
                return DaySchedule.closed(date, h.getName());
            }
            return new DaySchedule(date, true, h.getHolidayType().isBlocksAppointments(),
                    windowsFor(date, shifts), h.getName());
        }

        // 3. The weekly template. A weekday with no row is treated as
        //    non-working: an incomplete calendar must not silently open the
        //    shop on a day nobody configured.
        boolean working = template.getOrDefault((short) date.getDayOfWeek().getValue(), false);
        if (!working) {
            return DaySchedule.closed(date, null);
        }
        return DaySchedule.open(date, windowsFor(date, shifts));
    }

    private List<WorkingWindow> windowsFor(LocalDate date, List<CalendarShift> shifts) {
        int dow = date.getDayOfWeek().getValue();

        // A shift bound to this specific weekday overrides the general ones,
        // so a short Thursday replaces the standard day rather than adding to
        // it — otherwise "Thursday 09:00-13:00" would merely duplicate hours.
        boolean hasDaySpecific = shifts.stream()
                .anyMatch(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == dow);

        return shifts.stream()
                .filter(CalendarShift::isBookable)
                .filter(s -> hasDaySpecific
                        ? (s.getDayOfWeek() != null && s.getDayOfWeek() == dow)
                        : s.appliesTo(dow))
                .map(s -> new WorkingWindow(s.getCode(), s.getName(), s.getStartTime(),
                        s.getEndTime(), s.getMaxConcurrent(), s.isOvertime()))
                .toList();
    }

    /** Chronology lookups are repeated per date; resolve each id at most once. */
    private Function<Long, CalendarSystem> cachedSystemLookup() {
        Map<Long, CalendarSystem> cache = new HashMap<>();
        return id -> {
            if (id == null) {
                return null;
            }
            return cache.computeIfAbsent(id, key -> systemRepository.findById(key)
                    .flatMap(def -> systemRegistry.find(def.getHandlerKey()))
                    .orElse(null));
        };
    }

    // -----------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkingDay(Long calendarId, LocalDate date) {
        return scheduleFor(calendarId, date).workingDay();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookable(Long calendarId, LocalDate date) {
        return scheduleFor(calendarId, date).bookable();
    }

    /**
     * The appointment must sit inside ONE window. Spanning a lunch closure is
     * rejected rather than silently accepted, which is the "appointment exceeds
     * working hours" edge case in its most common disguise.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isWithinWorkingHours(Long calendarId, Instant start, Instant end) {
        if (!end.isAfter(start)) {
            return false;
        }
        ZoneId zone = require(calendarId).zone();
        LocalDateTime localStart = LocalDateTime.ofInstant(start, zone);
        LocalDateTime localEnd = LocalDateTime.ofInstant(end, zone);

        // An appointment crossing local midnight cannot lie in one window.
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            return false;
        }

        DaySchedule day = scheduleFor(calendarId, localStart.toLocalDate());
        if (!day.bookable()) {
            return false;
        }
        LocalTime from = localStart.toLocalTime();
        LocalTime to = localEnd.toLocalTime();
        return day.windows().stream().anyMatch(w -> w.encloses(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate nextWorkingDay(Long calendarId, LocalDate from) {
        LocalDate cursor = from.plusDays(1);
        for (int i = 0; i < MAX_DAY_SCAN; i++) {
            if (isWorkingDay(calendarId, cursor)) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }
        throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                "No working day found within %d days of %s — check the calendar configuration"
                        .formatted(MAX_DAY_SCAN, from));
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate addBusinessDays(Long calendarId, LocalDate from, int days) {
        if (days == 0) {
            return from;
        }
        int step = days > 0 ? 1 : -1;
        int remaining = Math.abs(days);
        LocalDate cursor = from;
        int scanned = 0;
        while (remaining > 0) {
            cursor = cursor.plusDays(step);
            if (++scanned > MAX_DAY_SCAN) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Could not add %d business days from %s within %d calendar days"
                                .formatted(days, from, MAX_DAY_SCAN));
            }
            if (isWorkingDay(calendarId, cursor)) {
                remaining--;
            }
        }
        return cursor;
    }

    @Override
    @Transactional(readOnly = true)
    public int countBusinessDays(Long calendarId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 0;
        }
        return (int) scheduleForRange(calendarId, from, to).stream()
                .filter(DaySchedule::workingDay)
                .count();
    }
}
