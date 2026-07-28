package com.sami.app.crm.dto;

import java.util.List;

/**
 * Query filters for the customer listing; all combinable, all optional.
 * {@code eventType} matches customers with at least one timeline event of that
 * type — the hook for purchase/repair/installment history searches.
 */
public record CustomerFilter(
        String search,
        String phone,
        String email,
        String nationalCode,
        String city,
        Long statusId,
        Long typeId,
        Long sourceId,
        List<Long> tagIds,
        String eventType,
        /** With eventType: only events in the last N days ("purchased in last 30 days"). */
        Integer eventSinceDays,
        /** Customers with NO timeline activity in the last N days ("inactive"). */
        Integer noEventSinceDays,
        Boolean includeHidden
) {
}
