package com.sami.app.scheduling.service;

import com.sami.app.calendar.api.DaySchedule;
import com.sami.app.calendar.api.WorkingWindow;
import com.sami.app.scheduling.domain.AppointmentType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Slot generation and interval algebra.
 *
 * <p>Deliberately free of repositories and Spring state so the arithmetic — the
 * part most likely to be subtly wrong — can be tested exhaustively without a
 * database. Everything here is a pure function of its arguments.
 *
 * <p><b>Half-open intervals throughout.</b> A span is {@code [start, end)},
 * matching the {@code '[)'} range used by the database exclusion constraint.
 * This is what makes a 10:00–11:00 and an 11:00–12:00 booking non-overlapping;
 * getting it wrong in either direction would either forbid back-to-back
 * appointments or permit genuine double bookings.
 */
@Component
public class SlotArithmetic {

    /** A half-open time span. */
    public record Span(Instant start, Instant end) {

        public Span {
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("span end must follow its start");
            }
        }

        public boolean overlaps(Span other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }

        public int minutes() {
            return (int) Duration.between(start, end).toMinutes();
        }
    }

    /**
     * Widens a customer-facing window to the span the RESOURCE is actually
     * occupied, by adding the type's preparation/buffer before and
     * cleanup/buffer after.
     *
     * <p>This is the single most important conversion in the module: booking
     * checks that use the customer window instead of this one will schedule a
     * technician with no turnaround between repairs.
     */
    public Span resourceSpan(AppointmentType type, Instant start, Instant end) {
        return new Span(
                start.minus(Duration.ofMinutes(type.leadInMinutes())),
                end.plus(Duration.ofMinutes(type.leadOutMinutes())));
    }

    /**
     * Candidate start times within one day's working windows.
     *
     * <p>A slot is offered only when the whole RESOURCE span fits inside a
     * single window — an appointment may not straddle a lunch closure, and the
     * prep time before it must fall inside opening hours too.
     *
     * @param stepMinutes granularity of offered starts, from the calendar
     */
    public List<Span> candidateSlots(DaySchedule day, ZoneId zone, AppointmentType type,
                                     int durationMinutes, int stepMinutes) {
        if (!day.bookable() || durationMinutes <= 0 || stepMinutes <= 0) {
            return List.of();
        }
        int lead = type.leadInMinutes();
        int trail = type.leadOutMinutes();
        int occupied = lead + durationMinutes + trail;

        List<Span> slots = new ArrayList<>();
        for (WorkingWindow window : day.windows()) {
            if (window.minutes() < occupied) {
                continue;
            }
            // Walk candidate RESOURCE starts; the customer-facing start sits
            // `lead` minutes later.
            LocalTime cursor = window.start();
            while (true) {
                LocalTime resourceEnd = cursor.plusMinutes(occupied);
                // plusMinutes wraps past midnight; a wrapped value is past the
                // window end by construction, so treat it as the terminator.
                if (resourceEnd.isBefore(cursor) || resourceEnd.isAfter(window.end())) {
                    break;
                }
                Instant customerStart = toInstant(day.date(), cursor.plusMinutes(lead), zone);
                Instant customerEnd = customerStart.plus(Duration.ofMinutes(durationMinutes));
                slots.add(new Span(customerStart, customerEnd));

                LocalTime next = cursor.plusMinutes(stepMinutes);
                if (!next.isAfter(cursor)) {
                    break;
                }
                cursor = next;
            }
        }
        return slots;
    }

    /**
     * Removes candidates that collide with an already-occupied span.
     *
     * <p>Both sides are compared as RESOURCE spans, not customer windows, for
     * the reason given on {@link #resourceSpan}.
     */
    public List<Span> removeBusy(List<Span> candidates, AppointmentType type, List<Span> busy) {
        if (busy.isEmpty()) {
            return candidates;
        }
        List<Span> free = new ArrayList<>();
        for (Span candidate : candidates) {
            Span occupied = resourceSpan(type, candidate.start(), candidate.end());
            if (busy.stream().noneMatch(occupied::overlaps)) {
                free.add(candidate);
            }
        }
        return free;
    }

    /** True when any pair in the list overlaps — used to validate bulk input. */
    public boolean hasInternalOverlap(List<Span> spans) {
        List<Span> sorted = spans.stream()
                .sorted((a, b) -> a.start().compareTo(b.start()))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).overlaps(sorted.get(i))) {
                return true;
            }
        }
        return false;
    }

    private Instant toInstant(LocalDate date, LocalTime time, ZoneId zone) {
        // Via the zone rather than a fixed offset so a DST transition — or a
        // future change to Iran's offset rules — resolves correctly.
        return date.atTime(time).atZone(zone).toInstant();
    }
}
