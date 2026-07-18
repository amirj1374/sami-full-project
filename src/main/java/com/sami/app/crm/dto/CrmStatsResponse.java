package com.sami.app.crm.dto;

import java.util.List;

/** Aggregate customer counts for dashboards and reports. */
public record CrmStatsResponse(
        long total,
        long newLast30Days,
        List<Bucket> byStatus,
        List<Bucket> byType,
        List<Bucket> bySource
) {

    public record Bucket(String name, long count) {
    }
}
