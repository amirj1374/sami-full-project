package com.sami.app.scheduling.service;

import com.sami.app.calendar.api.WorkingTimeProvider;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.scheduling.api.BookingCommand;
import com.sami.app.scheduling.api.BookingConflict;
import com.sami.app.scheduling.api.BookingResult;
import com.sami.app.scheduling.api.SlotRequest;
import com.sami.app.scheduling.api.TimeSlot;
import com.sami.app.scheduling.domain.AppointmentType;
import com.sami.app.scheduling.domain.Reservation;
import com.sami.app.scheduling.domain.SchedulableResource;
import com.sami.app.scheduling.domain.Schedule;
import com.sami.app.scheduling.domain.ScheduleStatus;
import com.sami.app.scheduling.event.SchedulingDomainEvent;
import com.sami.app.scheduling.repository.AppointmentTypeRepository;
import com.sami.app.scheduling.repository.ReservationRepository;
import com.sami.app.scheduling.repository.ScheduleRepository;
import com.sami.app.scheduling.repository.ScheduleStatusRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates, changes and closes appointments. The module's write path.
 *
 * <p><b>How double booking is actually prevented.</b> {@link AvailabilityService}
 * is consulted first, but only to produce a good error and useful alternatives.
 * The guarantee comes from the {@code ex_reservations_no_double_booking}
 * exclusion constraint: two concurrent bookings for the same technician can
 * both pass the advisory check, and Postgres will reject the second insert
 * regardless of interleaving. {@link #book} translates that rejection into a
 * domain conflict, and because the exception propagates the transaction rolls
 * back — there is no window in which a half-created appointment exists.
 *
 * <p>{@link #requestBooking} is deliberately NOT transactional: it wraps
 * {@link #book} so that after a rollback it can open a fresh read to compute
 * alternatives. Querying inside the poisoned transaction would fail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final String EXCLUSION_CONSTRAINT = "ex_reservations_no_double_booking";
    private static final int MAX_ALTERNATIVES = 5;

    private final ScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ScheduleStatusRepository statusRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final AvailabilityService availability;
    private final ReminderPlanner reminderPlanner;
    private final WaitingListService waitingListService;
    private final WorkingTimeProvider workingTime;
    private final SlotArithmetic slots;
    private final SchedulingAuditService audit;
    private final TenantDefaults tenantDefaults;
    private final ApplicationEventPublisher events;

    // -----------------------------------------------------------------
    // Booking
    // -----------------------------------------------------------------

    /**
     * Books an appointment, converting any conflict into a {@link BookingResult}
     * rather than an exception — "that slot is taken, here are three others" is
     * an ordinary business answer, not a fault.
     *
     * <p>Not transactional by design; see the class comment.
     */
    public BookingResult requestBooking(BookingCommand command) {
        try {
            return book(command);
        } catch (ScheduleConflictException e) {
            BookingConflict conflict = e.getConflict();
            List<TimeSlot> alternatives = safeAlternatives(command);

            Long waitingListId = null;
            if (command.joinWaitingList()) {
                waitingListId = waitingListService.enrol(command).orElse(null);
            }
            return new BookingResult(null, null, false, false, conflict, alternatives,
                    waitingListId, conflict.detail());
        }
    }

    /**
     * The transactional booking itself.
     *
     * @throws ScheduleConflictException when the slot is unavailable; the
     *                                   transaction rolls back
     */
    @Transactional
    public BookingResult book(BookingCommand command) {
        AppointmentType type = requireType(command.appointmentTypeCode());
        validateParties(type, command);

        int duration = command.durationMinutes() != null
                ? command.durationMinutes()
                : type.getDefaultDurationMinutes();
        Instant start = command.startsAt();
        Instant end = start.plus(Duration.ofMinutes(duration));

        List<SchedulableResource> resources = resolveResources(command, type, start, end);

        ScheduleStatus initial = statusRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No default schedule status is configured"));

        Long tenantId = tenantDefaults.current();
        Long calendarId = workingTime.resolveCalendarId(command.companyId(), command.branchId());

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .scheduleNumber(nextScheduleNumber())
                .title(command.title() != null ? command.title() : type.getName())
                .description(command.description())
                .category(type.getCategory())
                .appointmentType(type)
                .status(initial)
                .companyId(command.companyId())
                .branchId(command.branchId())
                .calendarId(calendarId)
                .moduleCode(command.moduleCode() != null ? command.moduleCode() : type.getModuleCode())
                .relatedEntityType(command.relatedEntityType())
                .relatedEntityId(command.relatedEntityId())
                .customerId(command.customerId())
                .supplierId(command.supplierId())
                .startsAt(start)
                .endsAt(end)
                .priority(command.priority() != null ? command.priority() : 0)
                .sourceChannel(command.sourceChannel() != null ? command.sourceChannel() : "STAFF")
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .tenantId(tenantId)
                .build());

        SlotArithmetic.Span occupied = slots.resourceSpan(type, start, end);
        for (SchedulableResource resource : resources) {
            createReservation(schedule, resource, initial, occupied, tenantId);
        }

        reminderPlanner.planFor(schedule);
        audit.record(SchedulingAuditService.SCHEDULE, schedule.getId(),
                SchedulingAuditService.CREATED, null,
                Map.of("scheduleNumber", schedule.getScheduleNumber(),
                        "startsAt", start.toString(),
                        "endsAt", end.toString(),
                        "resourceCount", resources.size()));

        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.APPOINTMENT_CREATED, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(),
                resources.isEmpty() ? null : resources.get(0).getId(),
                Map.of("startsAt", start.toString(), "durationMinutes", duration)));

        return new BookingResult(schedule.getId(), schedule.getScheduleNumber(),
                true, false, null, List.of(), null, "Appointment created");
    }

    /**
     * Inserts one reservation, translating the database's rejection.
     *
     * <p>{@code saveAndFlush} forces the INSERT now rather than at commit, so
     * the violation surfaces here where the offending resource is known and can
     * be named in the message. Deferring to commit would produce an opaque
     * failure attributable to no particular resource.
     */
    private void createReservation(Schedule schedule, SchedulableResource resource,
                                   ScheduleStatus status, SlotArithmetic.Span occupied,
                                   Long tenantId) {
        Reservation reservation = Reservation.builder()
                .reservationNumber(nextReservationNumber(schedule))
                .scheduleId(schedule.getId())
                .resource(resource)
                .status(status)
                .startsAt(occupied.start())
                .endsAt(occupied.end())
                // The invariant the exclusion constraint depends on.
                .holdsResource(status.isBlocksResource())
                .purpose(schedule.getTitle())
                .priority(schedule.getPriority())
                .customerId(schedule.getCustomerId())
                .supplierId(schedule.getSupplierId())
                .employeeUserId(resource.getUserId())
                .createdBy(CurrentActor.id())
                .tenantId(tenantId)
                .build();
        try {
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            if (isDoubleBooking(e)) {
                throw new ScheduleConflictException(new BookingConflict(
                        BookingConflict.Reason.RESOURCE_DOUBLE_BOOKED, resource.getId(),
                        "%s was reserved by another booking moments ago".formatted(resource.getName()),
                        occupied.start(), occupied.end(), List.of()));
            }
            throw e;
        }
        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.RESOURCE_RESERVED, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(), resource.getId(),
                Map.of("from", occupied.start().toString(), "to", occupied.end().toString())));
    }

    /**
     * Recognises the exclusion-constraint violation.
     *
     * <p>Matched on the constraint NAME rather than on the message text, which
     * varies by PostgreSQL version and server locale. The name is asserted to
     * exist by V24's post-migration check, so this cannot silently stop working.
     */
    private boolean isDoubleBooking(DataIntegrityViolationException e) {
        Throwable cursor = e;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && message.contains(EXCLUSION_CONSTRAINT)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private List<SchedulableResource> resolveResources(BookingCommand command, AppointmentType type,
                                                       Instant start, Instant end) {
        SlotRequest request = new SlotRequest(type.getId(), command.branchId(), command.companyId(),
                command.resourceIds(), command.requiredSkills(), start, end, null, null);
        List<SchedulableResource> candidates = availability.candidateResources(request);

        if (candidates.isEmpty()) {
            if (!type.isRequiresResource()) {
                return List.of();
            }
            throw new ScheduleConflictException(BookingConflict.of(
                    BookingConflict.Reason.NO_SUITABLE_RESOURCE, null,
                    "No active resource matches the requested branch and skills"));
        }

        // Explicitly named resources must ALL be free: asking for a specific
        // technician and silently getting someone else would be worse than a
        // clear failure.
        if (command.resourceIds() != null && !command.resourceIds().isEmpty()) {
            for (SchedulableResource resource : candidates) {
                availability.check(type, resource, command.companyId(), command.branchId(),
                                start, end, null)
                        .ifPresent(conflict -> {
                            throw new ScheduleConflictException(conflict);
                        });
            }
            return candidates;
        }

        // Otherwise take the first candidate that is actually free.
        BookingConflict lastConflict = null;
        for (SchedulableResource resource : candidates) {
            var conflict = availability.check(type, resource, command.companyId(),
                    command.branchId(), start, end, null);
            if (conflict.isEmpty()) {
                return List.of(resource);
            }
            lastConflict = conflict.get();
        }
        throw new ScheduleConflictException(lastConflict != null ? lastConflict
                : BookingConflict.of(BookingConflict.Reason.NO_SUITABLE_RESOURCE, null,
                        "No resource is free in the requested window"));
    }

    private void validateParties(AppointmentType type, BookingCommand command) {
        if (type.isRequiresCustomer() && command.customerId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "%s requires a customer".formatted(type.getName()));
        }
        if (type.isRequiresSupplier() && command.supplierId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "%s requires a supplier".formatted(type.getName()));
        }
        if (command.startsAt() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A start time is required");
        }
    }

    /** Alternatives must never turn a conflict into a 500, so failures are swallowed. */
    private List<TimeSlot> safeAlternatives(BookingCommand command) {
        try {
            AppointmentType type = requireType(command.appointmentTypeCode());
            return availability.findSlots(new SlotRequest(
                    type.getId(), command.branchId(), command.companyId(),
                    command.resourceIds(), command.requiredSkills(),
                    command.startsAt(), null, command.durationMinutes(), null), MAX_ALTERNATIVES);
        } catch (RuntimeException e) {
            log.warn("Could not compute booking alternatives: {}", e.getMessage());
            return List.of();
        }
    }

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------

    @Transactional
    public Schedule cancel(Long scheduleId, String reason) {
        Schedule schedule = require(scheduleId);
        if (!schedule.getStatus().isAllowsCancel()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "An appointment in status '%s' cannot be cancelled"
                            .formatted(schedule.getStatus().getCode()));
        }
        ScheduleStatus cancelled = statusRepository.findFirstByIsCancelledStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No cancelled status is configured"));

        String previous = schedule.getStatus().getCode();
        schedule.setStatus(cancelled);
        schedule.setCancelledAt(Instant.now());
        schedule.setCancellationReason(reason);
        schedule.setCancelledBy(CurrentActor.id());

        // Releasing the reservations is what frees the slot for the waiting
        // list; leaving holds_resource set would keep it blocked forever.
        releaseReservations(schedule, cancelled, reason);

        audit.record(SchedulingAuditService.SCHEDULE, schedule.getId(),
                SchedulingAuditService.CANCELLED,
                Map.of("status", previous),
                Map.of("status", cancelled.getCode(), "reason", reason == null ? "" : reason));

        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.APPOINTMENT_CANCELLED, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(), null,
                Map.of("reason", reason == null ? "" : reason)));

        // A freed slot is exactly when the waiting list should be consulted.
        waitingListService.onSlotFreed(schedule);
        return schedule;
    }

    @Transactional
    public Schedule checkIn(Long scheduleId) {
        Schedule schedule = require(scheduleId);
        if (!schedule.getStatus().isAllowsCheckIn()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "An appointment in status '%s' cannot be checked in"
                            .formatted(schedule.getStatus().getCode()));
        }
        ScheduleStatus checkedIn = statusRepository.findFirstByIsCheckedInStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No checked-in status is configured"));

        Instant now = Instant.now();
        schedule.setCheckedInAt(now);
        schedule.setStatus(checkedIn);
        // Lateness is recorded rather than blocking: a late customer is still
        // a customer, and the threshold is per-type configuration.
        if (schedule.isLateArrival(now)) {
            schedule.setArrivedLate(true);
            schedule.setLateByMinutes(schedule.minutesLate(now));
        }

        audit.record(SchedulingAuditService.SCHEDULE, schedule.getId(),
                SchedulingAuditService.CHECKED_IN, null,
                Map.of("checkedInAt", now.toString(), "late", schedule.isArrivedLate()));

        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.CUSTOMER_CHECKED_IN, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(), null,
                Map.of("late", schedule.isArrivedLate(),
                        "lateByMinutes", schedule.getLateByMinutes() == null
                                ? 0 : schedule.getLateByMinutes())));
        return schedule;
    }

    @Transactional
    public Schedule checkOut(Long scheduleId, String completionNotes) {
        Schedule schedule = require(scheduleId);
        if (schedule.getCheckedInAt() == null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot check out an appointment that was never checked in");
        }
        ScheduleStatus completed = statusRepository.findFirstByIsCompletedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No completed status is configured"));

        Instant now = Instant.now();
        schedule.setCheckedOutAt(now);
        schedule.setCompletionNotes(completionNotes);
        schedule.setStatus(completed);
        releaseReservations(schedule, completed, "Appointment completed");

        audit.record(SchedulingAuditService.SCHEDULE, schedule.getId(),
                SchedulingAuditService.CHECKED_OUT, null,
                Map.of("checkedOutAt", now.toString()));

        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.APPOINTMENT_COMPLETED, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(), null, Map.of()));
        return schedule;
    }

    @Transactional
    public Schedule markNoShow(Long scheduleId) {
        Schedule schedule = require(scheduleId);
        ScheduleStatus noShow = statusRepository.findFirstByIsNoShowStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No no-show status is configured"));
        schedule.setStatus(noShow);
        releaseReservations(schedule, noShow, "Marked as no-show");

        audit.record(SchedulingAuditService.SCHEDULE, schedule.getId(),
                SchedulingAuditService.NO_SHOW, null, Map.of());
        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.APPOINTMENT_NO_SHOW, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(), null, Map.of()));
        waitingListService.onSlotFreed(schedule);
        return schedule;
    }

    /**
     * Moves an appointment. Implemented as release-then-rebook inside one
     * transaction so the exclusion constraint still guards the new window —
     * mutating the reservation in place would let the old row's range mask a
     * genuine clash with a third booking.
     */
    @Transactional
    public BookingResult reschedule(Long scheduleId, Instant newStart, Integer newDurationMinutes) {
        Schedule schedule = require(scheduleId);
        if (!schedule.isEditable()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "An appointment in status '%s' cannot be modified"
                            .formatted(schedule.getStatus().getCode()));
        }
        AppointmentType type = schedule.getAppointmentType();
        int duration = newDurationMinutes != null ? newDurationMinutes
                : (schedule.getDurationMinutes() != null ? schedule.getDurationMinutes()
                        : type.getDefaultDurationMinutes());
        Instant newEnd = newStart.plus(Duration.ofMinutes(duration));

        List<Reservation> existing = reservationRepository.findByScheduleId(scheduleId);
        List<SchedulableResource> resources = existing.stream()
                .map(Reservation::getResource).toList();

        Instant oldStart = schedule.getStartsAt();

        // Free the old window first so the resource does not collide with
        // itself when the new window overlaps the old one.
        for (Reservation reservation : existing) {
            reservation.setHoldsResource(false);
            reservation.setReleasedAt(Instant.now());
            reservation.setReleaseReason("Rescheduled");
        }
        reservationRepository.flush();

        for (SchedulableResource resource : resources) {
            availability.check(type, resource, schedule.getCompanyId(), schedule.getBranchId(),
                            newStart, newEnd, scheduleId)
                    .ifPresent(conflict -> {
                        throw new ScheduleConflictException(conflict);
                    });
        }

        schedule.setStartsAt(newStart);
        schedule.setEndsAt(newEnd);

        SlotArithmetic.Span occupied = slots.resourceSpan(type, newStart, newEnd);
        Long tenantId = tenantDefaults.current();
        for (SchedulableResource resource : resources) {
            createReservation(schedule, resource, schedule.getStatus(), occupied, tenantId);
        }
        reminderPlanner.replanFor(schedule);

        audit.record(SchedulingAuditService.SCHEDULE, schedule.getId(),
                SchedulingAuditService.RESCHEDULED,
                Map.of("startsAt", oldStart.toString()),
                Map.of("startsAt", newStart.toString()));

        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.APPOINTMENT_RESCHEDULED, schedule.getId(),
                schedule.getScheduleNumber(), schedule.getModuleCode(), null,
                Map.of("from", oldStart.toString(), "to", newStart.toString())));

        return new BookingResult(schedule.getId(), schedule.getScheduleNumber(),
                true, false, null, List.of(), null, "Appointment rescheduled");
    }

    /** Drops the resource hold while keeping the row for history. */
    private void releaseReservations(Schedule schedule, ScheduleStatus status, String reason) {
        List<Reservation> reservations = reservationRepository.findByScheduleId(schedule.getId());
        for (Reservation reservation : reservations) {
            reservation.setStatus(status);
            reservation.setHoldsResource(status.isBlocksResource());
            if (!status.isBlocksResource()) {
                reservation.setReleasedAt(Instant.now());
                reservation.setReleaseReason(reason);
                events.publishEvent(SchedulingDomainEvent.of(
                        SchedulingDomainEvent.RESOURCE_RELEASED, schedule.getId(),
                        schedule.getScheduleNumber(), schedule.getModuleCode(),
                        reservation.getResource().getId(), Map.of()));
            }
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Schedule require(Long id) {
        return scheduleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment %d not found".formatted(id)));
    }

    private AppointmentType requireType(String code) {
        return appointmentTypeRepository.findByCode(code)
                .filter(AppointmentType::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment type '%s' not found or inactive".formatted(code)));
    }

    private String nextScheduleNumber() {
        return "APP-%06d".formatted(scheduleRepository.nextScheduleSequence());
    }

    private String nextReservationNumber(Schedule schedule) {
        // Derived from the schedule number plus an ordinal, so a reservation is
        // traceable to its appointment by eye in logs and exports.
        List<Reservation> existing = reservationRepository.findByScheduleId(schedule.getId());
        return "%s-R%d".formatted(schedule.getScheduleNumber(), existing.size() + 1);
    }
}
