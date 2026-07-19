package com.sami.app.scheduling.service;

import com.sami.app.calendar.api.DaySchedule;
import com.sami.app.calendar.api.WorkingTimeProvider;
import com.sami.app.calendar.service.WorkingTimeService;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.scheduling.api.BookingConflict;
import com.sami.app.scheduling.api.SlotRequest;
import com.sami.app.scheduling.api.TimeSlot;
import com.sami.app.scheduling.domain.AppointmentType;
import com.sami.app.scheduling.domain.Reservation;
import com.sami.app.scheduling.domain.SchedulableResource;
import com.sami.app.scheduling.provider.SchedulingRegistries;
import com.sami.app.scheduling.repository.AppointmentTypeRepository;
import com.sami.app.scheduling.repository.ReservationRepository;
import com.sami.app.scheduling.repository.SchedulableResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Answers "what is free" and "why is this not free".
 *
 * <p>Read-only by construction: it never writes a reservation. That separation
 * matters because its answers are inherently advisory — between this service
 * saying a slot is free and {@code SchedulingService} inserting the row, another
 * transaction may take it. The database exclusion constraint, not this class,
 * is what guarantees the schedule stays consistent. What this class provides is
 * good suggestions and precise diagnostics.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    /** How far ahead {@link #findSlots} will scan for free days. */
    private static final int MAX_SEARCH_DAYS = 60;

    private final SchedulableResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final WorkingTimeProvider workingTime;
    private final WorkingTimeService workingTimeService;
    private final SlotArithmetic slots;
    private final SchedulingRegistries.AllocationStrategies allocationStrategies;

    /**
     * Free slots matching the request, ranked by the allocation strategy.
     *
     * @param limit maximum slots to return; the search stops early once met
     */
    @Transactional(readOnly = true)
    public List<TimeSlot> findSlots(SlotRequest request, int limit) {
        AppointmentType type = requireType(request.appointmentTypeId());
        int duration = request.durationMinutes() != null
                ? request.durationMinutes()
                : type.getDefaultDurationMinutes();

        List<SchedulableResource> candidates = candidateResources(request);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Long calendarId = workingTime.resolveCalendarId(request.companyId(), request.branchId());
        ZoneId zone = workingTimeService.zoneOf(calendarId);
        int step = Math.max(5, workingTimeService.require(calendarId).getSlotMinutes());

        Instant searchStart = request.desiredStart() != null ? request.desiredStart() : Instant.now();
        Instant searchEnd = request.desiredEnd() != null
                ? request.desiredEnd()
                : searchStart.plus(Duration.ofDays(MAX_SEARCH_DAYS));

        LocalDate from = searchStart.atZone(zone).toLocalDate();
        LocalDate to = searchEnd.atZone(zone).toLocalDate();
        if (to.isAfter(from.plusDays(MAX_SEARCH_DAYS))) {
            to = from.plusDays(MAX_SEARCH_DAYS);
        }

        List<DaySchedule> days = workingTime.scheduleForRange(calendarId, from, to);
        List<Long> resourceIds = candidates.stream().map(SchedulableResource::getId).toList();

        // One query for the whole horizon rather than one per day per resource.
        List<Reservation> busy = reservationRepository.findOverlapping(
                resourceIds,
                from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant(),
                request.excludeScheduleId());

        List<TimeSlot> free = new ArrayList<>();
        for (DaySchedule day : days) {
            if (!day.bookable()) {
                continue;
            }
            List<SlotArithmetic.Span> candidateSpans =
                    slots.candidateSlots(day, zone, type, duration, step);
            if (candidateSpans.isEmpty()) {
                continue;
            }
            for (SchedulableResource resource : candidates) {
                List<SlotArithmetic.Span> resourceBusy = busy.stream()
                        .filter(r -> r.getResource().getId().equals(resource.getId()))
                        .map(r -> new SlotArithmetic.Span(r.getStartsAt(), r.getEndsAt()))
                        .toList();

                for (SlotArithmetic.Span span : slots.removeBusy(candidateSpans, type, resourceBusy)) {
                    if (span.start().isBefore(searchStart)) {
                        continue;
                    }
                    SlotArithmetic.Span occupied = slots.resourceSpan(type, span.start(), span.end());
                    free.add(TimeSlot.of(resource.getId(), resource.getName(),
                            span.start(), span.end(), occupied.start(), occupied.end()));
                }
            }
            if (free.size() >= limit) {
                break;
            }
        }

        return allocationStrategies.resolve(null).rank(request, free).stream()
                .limit(limit)
                .toList();
    }

    /**
     * Why a specific window cannot be booked on a specific resource, or empty
     * when it can.
     *
     * <p>Checks are ordered cheapest-and-most-informative first, so the caller
     * receives the most actionable reason rather than whichever failed last.
     */
    @Transactional(readOnly = true)
    public Optional<BookingConflict> check(AppointmentType type, SchedulableResource resource,
                                           Long companyId, Long branchId,
                                           Instant start, Instant end, Long excludeScheduleId) {

        int duration = (int) Duration.between(start, end).toMinutes();
        if (!type.permitsDuration(duration)) {
            return Optional.of(BookingConflict.of(
                    BookingConflict.Reason.DURATION_NOT_ALLOWED, resource.getId(),
                    "%d minutes is outside the %d–%d permitted for %s".formatted(
                            duration, type.getMinDurationMinutes(),
                            type.getMaxDurationMinutes(), type.getName())));
        }

        Optional<BookingConflict> notice = checkNotice(type, start);
        if (notice.isPresent()) {
            return notice;
        }

        if (!resource.acceptsBookings()) {
            return Optional.of(BookingConflict.of(
                    BookingConflict.Reason.RESOURCE_UNAVAILABLE, resource.getId(),
                    "%s is %s".formatted(resource.getName(),
                            resource.getStatus().getName().toLowerCase())));
        }

        // The resource span, not the customer window, is what must fit the day
        // and avoid collisions.
        SlotArithmetic.Span occupied = slots.resourceSpan(type, start, end);

        if (type.isEnforceWorkingHours()) {
            Long calendarId = resource.getCalendarId() != null
                    ? resource.getCalendarId()
                    : workingTime.resolveCalendarId(companyId, branchId);
            ZoneId zone = workingTimeService.zoneOf(calendarId);
            LocalDate date = occupied.start().atZone(zone).toLocalDate();

            DaySchedule day = workingTime.scheduleFor(calendarId, date);
            if (!day.workingDay() || day.appointmentsBlocked()) {
                return Optional.of(BookingConflict.of(
                        BookingConflict.Reason.OUTSIDE_BUSINESS_DAY, resource.getId(),
                        day.reason() != null
                                ? "%s is not a bookable day (%s)".formatted(date, day.reason())
                                : "%s is not a working day".formatted(date)));
            }
            if (!workingTime.isWithinWorkingHours(calendarId, occupied.start(), occupied.end())) {
                return Optional.of(BookingConflict.of(
                        BookingConflict.Reason.OUTSIDE_WORKING_HOURS, resource.getId(),
                        "The appointment, including %d minutes of preparation and %d of cleanup, "
                                .formatted(type.leadInMinutes(), type.leadOutMinutes())
                                + "does not fit inside a single working window"));
            }
        }

        List<Reservation> clashes = reservationRepository.findOverlapping(
                List.of(resource.getId()), occupied.start(), occupied.end(), excludeScheduleId);
        if (!clashes.isEmpty()) {
            Reservation first = clashes.get(0);
            return Optional.of(new BookingConflict(
                    BookingConflict.Reason.RESOURCE_DOUBLE_BOOKED, resource.getId(),
                    "%s is already reserved from %s to %s".formatted(
                            resource.getName(), first.getStartsAt(), first.getEndsAt()),
                    first.getStartsAt(), first.getEndsAt(),
                    clashes.stream().map(Reservation::getScheduleId).filter(java.util.Objects::nonNull).toList()));
        }
        return Optional.empty();
    }

    private Optional<BookingConflict> checkNotice(AppointmentType type, Instant start) {
        Instant now = Instant.now();
        long minutesAhead = Duration.between(now, start).toMinutes();

        if (minutesAhead < type.getMinNoticeMinutes()) {
            return Optional.of(BookingConflict.of(
                    BookingConflict.Reason.NOTICE_PERIOD_VIOLATED, null,
                    "%s requires at least %d minutes' notice".formatted(
                            type.getName(), type.getMinNoticeMinutes())));
        }
        if (Duration.between(now, start).toDays() > type.getMaxAdvanceDays()) {
            return Optional.of(BookingConflict.of(
                    BookingConflict.Reason.NOTICE_PERIOD_VIOLATED, null,
                    "%s cannot be booked more than %d days ahead".formatted(
                            type.getName(), type.getMaxAdvanceDays())));
        }
        return Optional.empty();
    }

    /** Active, bookable resources matching branch, category and required skills. */
    @Transactional(readOnly = true)
    public List<SchedulableResource> candidateResources(SlotRequest request) {
        List<SchedulableResource> pool;
        if (request.resourceIds() != null && !request.resourceIds().isEmpty()) {
            pool = resourceRepository.findAllById(request.resourceIds()).stream()
                    .filter(SchedulableResource::acceptsBookings)
                    .toList();
        } else {
            pool = resourceRepository.findBookable(request.branchId(), null);
        }
        return pool.stream()
                .filter(r -> !r.getCategory().isRequiresSkillMatch()
                        || r.hasSkills(request.requiredSkills()))
                .toList();
    }

    private AppointmentType requireType(Long id) {
        return appointmentTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment type %d not found or inactive".formatted(id)));
    }
}
