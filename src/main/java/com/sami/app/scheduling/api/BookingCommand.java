package com.sami.app.scheduling.api;

import java.time.Instant;
import java.util.List;

/**
 * A request to create an appointment.
 *
 * @param resourceIds   resources to reserve; empty asks the allocator to choose
 * @param durationMinutes null uses the appointment type's default
 * @param joinWaitingList when true, a conflict enrols the requester on the
 *                        waiting list instead of simply failing
 */
public record BookingCommand(
        String appointmentTypeCode,
        String title,
        String description,
        Long companyId,
        Long branchId,
        Long customerId,
        Long supplierId,
        List<Long> resourceIds,
        List<String> requiredSkills,
        Instant startsAt,
        Integer durationMinutes,
        Integer priority,
        String moduleCode,
        String relatedEntityType,
        Long relatedEntityId,
        String sourceChannel,
        boolean joinWaitingList
) { }
