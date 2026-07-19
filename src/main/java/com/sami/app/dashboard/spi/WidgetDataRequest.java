package com.sami.app.dashboard.spi;

import java.time.LocalDate;
import java.util.Map;

/**
 * Everything a {@link ReportingProvider} needs to compute a widget's data.
 *
 * <p>The {@code filters} carry the dashboard-level filter engine values
 * (company, branch, salesperson, date range) plus any widget-specific filters,
 * so a provider is filter-aware without the dashboard core knowing domain
 * semantics. {@code config} is the widget's raw configuration JSON (e.g. which
 * metric to fetch).
 *
 * @param providerKey    the resolved data-source provider key
 * @param metric         the metric/measure requested (from widget config)
 * @param config         the widget's configuration map
 * @param companyId      optional company scope (multi-tenant, future)
 * @param branchId       optional branch scope
 * @param salespersonId  optional salesperson scope
 * @param from           optional date-range start
 * @param to             optional date-range end
 * @param filters        additional dynamic filters (AND-combined by convention)
 */
public record WidgetDataRequest(
        String providerKey,
        String metric,
        Map<String, Object> config,
        Long companyId,
        Long branchId,
        Long salespersonId,
        LocalDate from,
        LocalDate to,
        Map<String, Object> filters
) {
}
