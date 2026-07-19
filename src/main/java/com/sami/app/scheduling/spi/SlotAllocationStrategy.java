package com.sami.app.scheduling.spi;

import com.sami.app.scheduling.api.SlotRequest;
import com.sami.app.scheduling.api.TimeSlot;

import java.util.List;

/**
 * Chooses which of the free slots to offer, and in what order.
 *
 * <p>This is the AI-optimisation seam the spec asks for. The default
 * implementation returns slots earliest-first; a predictive implementation can
 * later rank by technician skill match, expected job duration, travel time or
 * no-show probability without any caller changing. Because it only ever
 * REORDERS or FILTERS candidates that the availability engine has already
 * proved free, a bad strategy can never produce a double booking.
 */
public interface SlotAllocationStrategy {

    /** Matches configuration; {@code default} is the built-in earliest-first. */
    String key();

    List<TimeSlot> rank(SlotRequest request, List<TimeSlot> freeSlots);
}
