package com.sami.app.scheduling.provider;

import com.sami.app.scheduling.api.SlotRequest;
import com.sami.app.scheduling.api.TimeSlot;
import com.sami.app.scheduling.spi.SlotAllocationStrategy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Default allocation: earliest slot first, breaking ties by resource priority
 * order as supplied by the caller.
 *
 * <p>This is the seam a predictive or AI-driven allocator replaces. Because it
 * only reorders slots the availability engine has already proved free, a
 * smarter strategy changes which appointment is offered — never whether the
 * schedule stays consistent.
 */
@Component
public class EarliestFirstAllocation implements SlotAllocationStrategy {

    public static final String KEY = "default";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public List<TimeSlot> rank(SlotRequest request, List<TimeSlot> freeSlots) {
        return freeSlots.stream()
                .sorted(Comparator.comparing(TimeSlot::start))
                .toList();
    }
}
