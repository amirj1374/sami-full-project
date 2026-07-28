package com.sami.app.scheduling.service;

import com.sami.app.calendar.api.WorkingTimeProvider;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.scheduling.domain.AppointmentTypeReminder;
import com.sami.app.scheduling.domain.Schedule;
import com.sami.app.scheduling.domain.ScheduleReminder;
import com.sami.app.scheduling.repository.AppointmentTypeReminderRepository;
import com.sami.app.scheduling.repository.ScheduleReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Materialises an appointment's reminders from its type's rules.
 *
 * <p>Rows are written even though every channel ships inactive, because the
 * alternative — generating reminders lazily at send time — would mean turning
 * a channel on could never recover reminders for appointments already booked.
 * Queuing them now makes activation a pure configuration change.
 *
 * <p>A reminder whose computed time is already past is skipped rather than
 * fired immediately: nobody wants a "your appointment is tomorrow" message for
 * an appointment booked this morning for this afternoon.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderPlanner {

    private final AppointmentTypeReminderRepository ruleRepository;
    private final ScheduleReminderRepository reminderRepository;
    private final WorkingTimeProvider workingTime;
    private final TenantDefaults tenantDefaults;

    @Transactional(propagation = Propagation.MANDATORY)
    public void planFor(Schedule schedule) {
        List<AppointmentTypeReminder> rules = ruleRepository
                .findByAppointmentTypeIdAndIsActiveTrue(schedule.getAppointmentType().getId());
        Instant now = Instant.now();

        for (AppointmentTypeReminder rule : rules) {
            Instant fireAt = fireTime(schedule, rule);
            if (fireAt.isBefore(now)) {
                continue;
            }
            reminderRepository.save(ScheduleReminder.builder()
                    .scheduleId(schedule.getId())
                    .channel(rule.getChannel())
                    .scheduledFor(fireAt)
                    .deliveryState("PENDING")
                    .tenantId(tenantDefaults.current())
                    .build());
        }
    }

    /** Rescheduling invalidates every pending reminder; replace them wholesale. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void replanFor(Schedule schedule) {
        List<ScheduleReminder> existing = reminderRepository.findByScheduleId(schedule.getId());
        for (ScheduleReminder reminder : existing) {
            if ("PENDING".equals(reminder.getDeliveryState())) {
                reminder.setDeliveryState("CANCELLED");
            }
        }
        planFor(schedule);
    }

    /**
     * When the reminder should fire.
     *
     * <p>A business-time offset counts back in WORKING days rather than
     * wall-clock hours, so a "one working day before" reminder for a Saturday
     * appointment fires on Thursday when Friday is the weekend — the spec's
     * Business Day Reminder.
     */
    private Instant fireTime(Schedule schedule, AppointmentTypeReminder rule) {
        Instant start = schedule.getStartsAt();
        if (!rule.isUseBusinessTime() || schedule.getCalendarId() == null) {
            return start.minus(Duration.ofMinutes(rule.getOffsetMinutes()));
        }
        try {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate appointmentDay = start.atZone(zone).toLocalDate();
            int businessDays = Math.max(1, rule.getOffsetMinutes() / (60 * 24));
            LocalDate fireDay = workingTime.addBusinessDays(
                    schedule.getCalendarId(), appointmentDay, -businessDays);
            return fireDay.atTime(start.atZone(zone).toLocalTime()).atZone(zone).toInstant();
        } catch (RuntimeException e) {
            // A misconfigured calendar must not prevent the appointment being
            // booked; fall back to wall-clock and record why.
            log.warn("Business-time reminder offset failed for schedule {}: {}",
                    schedule.getId(), e.getMessage());
            return start.minus(Duration.ofMinutes(rule.getOffsetMinutes()));
        }
    }
}
