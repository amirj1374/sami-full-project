package com.sami.app.dashboard.spi;

/**
 * The primary dashboard extension point. A business module (Sales, Inventory,
 * Repair, …) supplies dashboard data by publishing a Spring bean that
 * implements this interface — it is discovered automatically by
 * {@code ReportingProviderRegistry} via its {@link #key()} and never requires a
 * change to the dashboard core.
 *
 * <p>This is how the architecture stays generic: there is no
 * {@code SalesDashboardService} here; there is a {@code sales} data source whose
 * {@code provider_key} resolves to a {@code ReportingProvider} living inside the
 * Sales module.
 *
 * <pre>
 * &#64;Component
 * class SalesReportingProvider implements ReportingProvider {
 *     public String key() { return "sales"; }
 *     public WidgetData fetch(WidgetDataRequest r) { ... }
 * }
 * </pre>
 */
public interface ReportingProvider {

    /** Stable key matching {@code dash_data_sources.provider_key}. */
    String key();

    /** Computes the widget's data for the given request. */
    WidgetData fetch(WidgetDataRequest request);
}
