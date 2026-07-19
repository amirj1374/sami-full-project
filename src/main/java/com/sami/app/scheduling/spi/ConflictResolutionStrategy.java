package com.sami.app.scheduling.spi;

import com.sami.app.scheduling.api.BookingConflict;
import com.sami.app.scheduling.api.SlotRequest;

/**
 * Decides what happens when a requested slot is not free.
 *
 * <p>Resolves from {@code conflict_policies.handler_key}, so "prevent",
 * "suggest alternatives", "replace the resource", "warn" and "require manager
 * approval" are configuration rows rather than branches inside a service.
 */
public interface ConflictResolutionStrategy {

    /** Matches {@code conflict_policies.handler_key}. */
    String key();

    /**
     * @param conflict what the availability engine found
     * @return the outcome the caller must honour
     */
    ConflictOutcome resolve(SlotRequest request, BookingConflict conflict);
}
