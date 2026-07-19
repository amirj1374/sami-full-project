package com.sami.app.scheduling;

import com.sami.app.calendar.api.DaySchedule;
import com.sami.app.calendar.api.WorkingWindow;
import com.sami.app.scheduling.domain.AppointmentType;
import com.sami.app.scheduling.service.SlotArithmetic;
import com.sami.app.scheduling.service.SlotArithmetic.Span;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The interval algebra behind every booking decision.
 *
 * <p>These rules are the difference between "back-to-back appointments work"
 * and "the technician has no turnaround", and between a correct schedule and a
 * double booking — so they are tested directly rather than through the service.
 */
class SlotArithmeticTest {

    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final LocalDate DAY = LocalDate.of(2026, 8, 1);

    private final SlotArithmetic arithmetic = new SlotArithmetic();

    /** An appointment type with configurable prep/cleanup, defaults to none. */
    private AppointmentType type(int prep, int cleanup, int bufferBefore, int bufferAfter) {
        return AppointmentType.builder()
                .code("TEST").name("Test")
                .defaultDurationMinutes(30).minDurationMinutes(15).maxDurationMinutes(240)
                .preparationMinutes(prep).cleanupMinutes(cleanup)
                .bufferBeforeMinutes(bufferBefore).bufferAfterMinutes(bufferAfter)
                .isActive(true)
                .build();
    }

    private Instant at(int hour, int minute) {
        return DAY.atTime(hour, minute).atZone(TEHRAN).toInstant();
    }

    private DaySchedule openDay(WorkingWindow... windows) {
        return DaySchedule.open(DAY, List.of(windows));
    }

    private WorkingWindow window(String code, int fromHour, int toHour) {
        return new WorkingWindow(code, code, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0), 0, false);
    }

    @Nested
    @DisplayName("half-open intervals")
    class HalfOpen {

        /**
         * The single most important property: an appointment ending at 11:00
         * and one starting at 11:00 must not collide, or no clinic could ever
         * run consecutive appointments.
         */
        @Test
        void backToBackSpansDoNotOverlap() {
            Span first = new Span(at(10, 0), at(11, 0));
            Span second = new Span(at(11, 0), at(12, 0));

            assertThat(first.overlaps(second)).isFalse();
            assertThat(second.overlaps(first)).isFalse();
        }

        @Test
        void genuineOverlapIsDetectedFromEitherSide() {
            Span first = new Span(at(10, 0), at(11, 0));
            Span straddling = new Span(at(10, 30), at(11, 30));

            assertThat(first.overlaps(straddling)).isTrue();
            assertThat(straddling.overlaps(first)).isTrue();
        }

        @Test
        void containmentCountsAsOverlap() {
            Span outer = new Span(at(9, 0), at(17, 0));
            Span inner = new Span(at(12, 0), at(13, 0));

            assertThat(outer.overlaps(inner)).isTrue();
            assertThat(inner.overlaps(outer)).isTrue();
        }

        @Test
        void aSpanMustHavePositiveLength() {
            assertThatThrownBy(() -> new Span(at(10, 0), at(10, 0)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Span(at(11, 0), at(10, 0)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("resource span vs customer window")
    class ResourceSpan {

        /**
         * The resource is busy before the customer arrives and after they
         * leave. A check that used the customer window would book two repairs
         * with zero bench turnaround.
         */
        @Test
        void widensTheWindowByPrepAndCleanup() {
            AppointmentType repair = type(10, 15, 0, 0);
            Span occupied = arithmetic.resourceSpan(repair, at(10, 0), at(11, 0));

            assertThat(occupied.start()).isEqualTo(at(9, 50));
            assertThat(occupied.end()).isEqualTo(at(11, 15));
            assertThat(occupied.minutes()).isEqualTo(85);
        }

        @Test
        void buffersAddOnTopOfPrepAndCleanup() {
            AppointmentType repair = type(10, 15, 5, 10);
            Span occupied = arithmetic.resourceSpan(repair, at(10, 0), at(11, 0));

            assertThat(occupied.start()).isEqualTo(at(9, 45));
            assertThat(occupied.end()).isEqualTo(at(11, 25));
        }

        @Test
        void isIdentityWhenNoBuffersAreConfigured() {
            Span occupied = arithmetic.resourceSpan(type(0, 0, 0, 0), at(10, 0), at(11, 0));

            assertThat(occupied.start()).isEqualTo(at(10, 0));
            assertThat(occupied.end()).isEqualTo(at(11, 0));
        }

        /**
         * Two appointments that look back-to-back on the customer's calendar
         * DO collide once buffers are applied. This is the case that motivates
         * storing the wider span on the reservation row.
         */
        @Test
        void buffersMakeApparentlyAdjacentAppointmentsCollide() {
            AppointmentType repair = type(10, 15, 0, 0);
            Span first = arithmetic.resourceSpan(repair, at(10, 0), at(11, 0));
            Span second = arithmetic.resourceSpan(repair, at(11, 0), at(12, 0));

            assertThat(first.overlaps(second))
                    .as("11:15 cleanup runs into the 10:50 prep of the next job")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("candidate slot generation")
    class Candidates {

        @Test
        void stepsThroughTheWindowAtTheConfiguredGranularity() {
            List<Span> generated = arithmetic.candidateSlots(
                    openDay(window("MORNING", 9, 12)), TEHRAN, type(0, 0, 0, 0), 60, 30);

            assertThat(generated).hasSize(5);
            assertThat(generated.get(0).start()).isEqualTo(at(9, 0));
            assertThat(generated.get(1).start()).isEqualTo(at(9, 30));
            assertThat(generated.get(4).start()).isEqualTo(at(11, 0));
        }

        /** The last offered slot must END by closing time, not start by it. */
        @Test
        void neverOffersASlotThatWouldRunPastClosing() {
            List<Span> generated = arithmetic.candidateSlots(
                    openDay(window("MORNING", 9, 12)), TEHRAN, type(0, 0, 0, 0), 60, 30);

            assertThat(generated).allSatisfy(span ->
                    assertThat(span.end()).isBeforeOrEqualTo(at(12, 0)));
        }

        /** Prep and cleanup must fit inside opening hours too. */
        @Test
        void accountsForBuffersWhenFittingSlotsIntoAWindow() {
            List<Span> generated = arithmetic.candidateSlots(
                    openDay(window("MORNING", 9, 12)), TEHRAN, type(15, 15, 0, 0), 60, 30);

            // First customer start is 09:15 — 09:00 opening plus 15 min prep.
            assertThat(generated.get(0).start()).isEqualTo(at(9, 15));
            // Last must leave room for 15 min cleanup before 12:00.
            Span last = generated.get(generated.size() - 1);
            assertThat(last.end()).isBeforeOrEqualTo(at(11, 45));
        }

        /**
         * An appointment may not straddle a lunch closure: two shifts produce
         * two independent runs of slots, never one spanning the gap.
         */
        @Test
        void doesNotBridgeTwoShifts() {
            List<Span> generated = arithmetic.candidateSlots(
                    openDay(window("MORNING", 9, 13), window("AFTERNOON", 16, 20)),
                    TEHRAN, type(0, 0, 0, 0), 60, 60);

            assertThat(generated).noneSatisfy(span -> {
                assertThat(span.start()).isBefore(at(13, 0));
                assertThat(span.end()).isAfter(at(16, 0));
            });
            assertThat(generated).hasSize(8);
        }

        @Test
        void yieldsNothingWhenTheWindowIsShorterThanTheAppointment() {
            List<Span> generated = arithmetic.candidateSlots(
                    openDay(window("SHORT", 9, 10)), TEHRAN, type(0, 0, 0, 0), 120, 30);

            assertThat(generated).isEmpty();
        }

        @Test
        void yieldsNothingOnAClosedDay() {
            List<Span> generated = arithmetic.candidateSlots(
                    DaySchedule.closed(DAY, "Nowruz"), TEHRAN, type(0, 0, 0, 0), 30, 30);

            assertThat(generated).isEmpty();
        }
    }

    @Nested
    @DisplayName("busy-time subtraction")
    class RemoveBusy {

        @Test
        void dropsCandidatesCollidingWithAnExistingBooking() {
            AppointmentType plain = type(0, 0, 0, 0);
            List<Span> candidates = arithmetic.candidateSlots(
                    openDay(window("DAY", 9, 12)), TEHRAN, plain, 60, 60);
            List<Span> busy = List.of(new Span(at(10, 0), at(11, 0)));

            List<Span> free = arithmetic.removeBusy(candidates, plain, busy);

            assertThat(free).extracting(Span::start).containsExactly(at(9, 0), at(11, 0));
        }

        /**
         * Buffers widen the candidate before comparison, so a booking that
         * merely abuts the customer window still removes the slot.
         */
        @Test
        void appliesBuffersWhenTestingCollision() {
            AppointmentType buffered = type(15, 15, 0, 0);
            List<Span> candidates = List.of(new Span(at(11, 0), at(12, 0)));
            // Ends exactly when the 11:00 appointment's prep would begin.
            List<Span> busy = List.of(new Span(at(10, 0), at(10, 50)));

            assertThat(arithmetic.removeBusy(candidates, buffered, busy)).isEmpty();
        }

        @Test
        void keepsEverythingWhenNothingIsBooked() {
            AppointmentType plain = type(0, 0, 0, 0);
            List<Span> candidates = arithmetic.candidateSlots(
                    openDay(window("DAY", 9, 12)), TEHRAN, plain, 60, 60);

            assertThat(arithmetic.removeBusy(candidates, plain, List.of()))
                    .isEqualTo(candidates);
        }
    }

    @Nested
    @DisplayName("internal overlap detection")
    class InternalOverlap {

        @Test
        void acceptsAChainOfBackToBackSpans() {
            assertThat(arithmetic.hasInternalOverlap(List.of(
                    new Span(at(9, 0), at(10, 0)),
                    new Span(at(10, 0), at(11, 0)),
                    new Span(at(11, 0), at(12, 0))))).isFalse();
        }

        @Test
        void detectsOverlapRegardlessOfInputOrder() {
            assertThat(arithmetic.hasInternalOverlap(List.of(
                    new Span(at(11, 0), at(12, 0)),
                    new Span(at(9, 0), at(10, 30)),
                    new Span(at(10, 0), at(11, 0))))).isTrue();
        }
    }

    @Test
    void spanReportsItsLengthInMinutes() {
        assertThat(new Span(at(9, 0), at(10, 30)).minutes()).isEqualTo(90);
        assertThat(new Span(at(9, 0), at(9, 0).plus(Duration.ofMinutes(45))).minutes())
                .isEqualTo(45);
    }
}
