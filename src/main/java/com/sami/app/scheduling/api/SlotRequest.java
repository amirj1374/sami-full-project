package com.sami.app.scheduling.api;

import java.time.Instant;
import java.util.List;

/**
 * What a caller wants to book.
 *
 * @param resourceIds candidate resources; empty means "any suitable resource"
 * @param requiredSkills matched against {@code resources.skills} when the
 *                       resource category requires it
 */
public record SlotRequest(
        Long appointmentTypeId,
        Long branchId,
        Long companyId,
        List<Long> resourceIds,
        List<String> requiredSkills,
        Instant desiredStart,
        Instant desiredEnd,
        Integer durationMinutes,
        Long excludeScheduleId
) { }
