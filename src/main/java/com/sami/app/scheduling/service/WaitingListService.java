package com.sami.app.scheduling.service;

import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.scheduling.api.BookingCommand;
import com.sami.app.scheduling.domain.AppointmentType;
import com.sami.app.scheduling.domain.Schedule;
import com.sami.app.scheduling.domain.WaitingListEntry;
import com.sami.app.scheduling.event.SchedulingDomainEvent;
import com.sami.app.scheduling.repository.AppointmentTypeRepository;
import com.sami.app.scheduling.repository.WaitingListEntryRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The waiting list: standing requests for slots that were not free.
 *
 * <p><b>Why promotion only ever notifies.</b> When a slot frees, this service
 * identifies the next entitled entry and publishes an event — it does NOT
 * silently create the appointment. Auto-booking someone into a slot they asked
 * about days ago, without their knowledge, produces no-shows; and doing it
 * inside the cancelling transaction would make one customer's cancellation fail
 * because a different customer's booking hit a conflict. Promotion to a real
 * appointment is an explicit call to {@link #promote}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingListService {

    /** How long an entry stays live by default. */
    private static final int DEFAULT_TTL_DAYS = 30;

    private final WaitingListEntryRepository repository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final SchedulingAuditService audit;
    private final TenantDefaults tenantDefaults;
    private final ApplicationEventPublisher events;

    /**
     * Adds a failed booking attempt to the list.
     *
     * @return the new entry id, or empty when the type forbids waiting lists
     */
    @Transactional
    public Optional<Long> enrol(BookingCommand command) {
        AppointmentType type = appointmentTypeRepository.findByCode(command.appointmentTypeCode())
                .filter(AppointmentType::isActive)
                .orElse(null);
        if (type == null || !type.isAllowsWaitingList()) {
            return Optional.empty();
        }

        int duration = command.durationMinutes() != null
                ? command.durationMinutes() : type.getDefaultDurationMinutes();
        Instant from = command.startsAt();
        // Accept anything within the type's advance window unless the caller
        // narrowed it; a one-minute window would never match anything.
        Instant to = from.plus(Duration.ofDays(Math.min(type.getMaxAdvanceDays(), DEFAULT_TTL_DAYS)));

        WaitingListEntry entry = repository.save(WaitingListEntry.builder()
                .entryNumber("WL-%d".formatted(System.nanoTime() % 1_000_000_000L))
                .appointmentType(type)
                .customerId(command.customerId())
                .supplierId(command.supplierId())
                .branchId(command.branchId())
                .resourceId(command.resourceIds() != null && command.resourceIds().size() == 1
                        ? command.resourceIds().get(0) : null)
                .desiredFrom(from)
                .desiredTo(to)
                .durationMinutes(duration)
                .priority(command.priority() != null ? command.priority() : 0)
                .isActive(true)
                .expiresAt(to)
                .createdBy(CurrentActor.id())
                .tenantId(tenantDefaults.current())
                .build());

        audit.record(SchedulingAuditService.WAITING_LIST, entry.getId(),
                SchedulingAuditService.CREATED, null,
                Map.of("desiredFrom", from.toString(), "durationMinutes", duration));

        events.publishEvent(SchedulingDomainEvent.of(
                SchedulingDomainEvent.WAITING_LIST_ADDED, null, entry.getEntryNumber(),
                type.getModuleCode(), null, Map.of("entryId", entry.getId())));

        return Optional.of(entry.getId());
    }

    /**
     * Announces that a slot became free. Publishes an event for the best
     * candidate; see the class comment for why it does not book.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onSlotFreed(Schedule schedule) {
        List<WaitingListEntry> queue = repository.findQueue(
                schedule.getAppointmentType().getId(), schedule.getBranchId(),
                schedule.getStartsAt(), schedule.getEndsAt());

        queue.stream()
                .filter(entry -> !entry.isExpired(Instant.now()))
                .filter(entry -> entry.accepts(schedule.getStartsAt(), schedule.getEndsAt()))
                .findFirst()
                .ifPresent(entry -> events.publishEvent(SchedulingDomainEvent.of(
                        SchedulingDomainEvent.WAITING_LIST_PROMOTED, schedule.getId(),
                        schedule.getScheduleNumber(), schedule.getModuleCode(), null,
                        Map.of("waitingListEntryId", entry.getId(),
                               "freedFrom", schedule.getStartsAt().toString(),
                               "freedTo", schedule.getEndsAt().toString()))));
    }

    /** Records that an entry became a real appointment. */
    @Transactional
    public WaitingListEntry markPromoted(Long entryId, Long scheduleId, boolean manual) {
        WaitingListEntry entry = repository.findById(entryId)
                .orElseThrow(() -> new com.sami.app.common.exception.ResourceNotFoundException(
                        "Waiting list entry %d not found".formatted(entryId)));
        entry.setPromotedAt(Instant.now());
        entry.setPromotedScheduleId(scheduleId);
        entry.setPromotedManually(manual);
        entry.setActive(false);

        audit.record(SchedulingAuditService.WAITING_LIST, entry.getId(),
                SchedulingAuditService.PROMOTED, null,
                Map.of("scheduleId", scheduleId, "manual", manual));
        return entry;
    }

    @Transactional
    public WaitingListEntry cancel(Long entryId, String reason) {
        WaitingListEntry entry = repository.findById(entryId)
                .orElseThrow(() -> new com.sami.app.common.exception.ResourceNotFoundException(
                        "Waiting list entry %d not found".formatted(entryId)));
        entry.setCancelledAt(Instant.now());
        entry.setCancellationReason(reason);
        entry.setActive(false);
        return entry;
    }

    /** Expires lapsed entries. Driven by the shared scheduler (V19). */
    @Transactional
    public int expireLapsed() {
        List<WaitingListEntry> expired = repository.findExpired(Instant.now());
        for (WaitingListEntry entry : expired) {
            entry.setActive(false);
            entry.setCancellationReason("Expired");
            entry.setCancelledAt(Instant.now());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} waiting list entries", expired.size());
        }
        return expired.size();
    }
}
