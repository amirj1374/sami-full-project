package com.sami.app.scheduling.provider;

import com.sami.app.scheduling.api.BookingConflict;
import com.sami.app.scheduling.api.SlotRequest;
import com.sami.app.scheduling.spi.ConflictOutcome;
import com.sami.app.scheduling.spi.ConflictResolutionStrategy;
import org.springframework.stereotype.Component;

/**
 * The conflict strategies seeded by V24.
 *
 * <p>Grouped in one file because each is a handful of lines; they are separate
 * beans so a deployment can replace any one of them individually.
 *
 * <p>Note what none of them do: none can cause a double booking. A strategy
 * only decides how to REPORT or REROUTE a conflict the availability engine has
 * already found, and the database constraint remains the final arbiter, so even
 * a buggy custom strategy cannot corrupt the schedule.
 */
public final class BuiltinConflictStrategies {

    private BuiltinConflictStrategies() { }

    /** Reject outright — the default and the safest. */
    @Component
    public static class Prevent implements ConflictResolutionStrategy {
        @Override public String key() { return "prevent"; }

        @Override
        public ConflictOutcome resolve(SlotRequest request, BookingConflict conflict) {
            return ConflictOutcome.reject(conflict.detail());
        }
    }

    /**
     * Reject, but hand back free alternatives. The alternatives are computed by
     * the availability engine before the strategy runs, so this only decides
     * whether to surface them.
     */
    @Component
    public static class Suggest implements ConflictResolutionStrategy {
        @Override public String key() { return "suggest"; }

        @Override
        public ConflictOutcome resolve(SlotRequest request, BookingConflict conflict) {
            return ConflictOutcome.suggest(java.util.List.of(), conflict.detail());
        }
    }

    /**
     * Ask the engine to retry on a different suitable resource. Signalled by
     * suggesting with no alternatives of its own; SchedulingService interprets
     * the policy's {@code suggestsAlternatives} flag and re-runs allocation
     * across the remaining candidates.
     */
    @Component
    public static class Replace implements ConflictResolutionStrategy {
        @Override public String key() { return "replace"; }

        @Override
        public ConflictOutcome resolve(SlotRequest request, BookingConflict conflict) {
            return ConflictOutcome.suggest(java.util.List.of(),
                    "Requested resource is unavailable; another will be attempted");
        }
    }

    /**
     * Allow the booking and record a warning.
     *
     * <p>This can only ever succeed for NON-overlap conflicts — an outside-hours
     * or notice-period breach. A genuine resource overlap is still rejected by
     * the database constraint regardless of what this returns, which is the
     * point: policy can relax business rules, never data integrity.
     */
    @Component
    public static class Warn implements ConflictResolutionStrategy {
        @Override public String key() { return "warn"; }

        @Override
        public ConflictOutcome resolve(SlotRequest request, BookingConflict conflict) {
            if (conflict.reason() == BookingConflict.Reason.RESOURCE_DOUBLE_BOOKED) {
                return ConflictOutcome.reject(
                        "Double booking cannot be overridden: " + conflict.detail());
            }
            return ConflictOutcome.allow("Booked with warning: " + conflict.detail());
        }
    }

    /** Hold the booking for a manager decision. */
    @Component
    public static class Approval implements ConflictResolutionStrategy {
        @Override public String key() { return "approval"; }

        @Override
        public ConflictOutcome resolve(SlotRequest request, BookingConflict conflict) {
            if (conflict.reason() == BookingConflict.Reason.RESOURCE_DOUBLE_BOOKED) {
                return ConflictOutcome.reject(
                        "Double booking cannot be approved: " + conflict.detail());
            }
            return ConflictOutcome.needsApproval(
                    "Requires manager approval: " + conflict.detail());
        }
    }
}
