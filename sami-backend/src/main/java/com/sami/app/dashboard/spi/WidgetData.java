package com.sami.app.dashboard.spi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The normalized result a {@link ReportingProvider} returns for a widget. One
 * flexible shape covers every widget/chart type: a scalar (KPI card, gauge,
 * progress), a labelled series (charts, leaderboards), or tabular rows (tables,
 * pivots, feeds). The frontend renders it according to the widget type, so new
 * widget types never require a new backend contract.
 *
 * @param kind    SCALAR | SERIES | TABLE | ROWS | EMPTY
 * @param value   populated for SCALAR
 * @param series  populated for SERIES (charts, leaderboards)
 * @param columns column keys for TABLE
 * @param rows    row maps for TABLE / ROWS (activity feeds, transaction lists)
 * @param meta    free-form extras (unit, trend, target, drill-down hints…)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WidgetData(
        Kind kind,
        BigDecimal value,
        List<SeriesPoint> series,
        List<String> columns,
        List<Map<String, Object>> rows,
        Map<String, Object> meta
) {
    public enum Kind { SCALAR, SERIES, TABLE, ROWS, EMPTY }

    /** A single labelled data point (chart category, leaderboard entry…). */
    public record SeriesPoint(String label, BigDecimal value) {
    }

    public static WidgetData scalar(BigDecimal value, Map<String, Object> meta) {
        return new WidgetData(Kind.SCALAR, value, null, null, null, meta);
    }

    public static WidgetData series(List<SeriesPoint> series, Map<String, Object> meta) {
        return new WidgetData(Kind.SERIES, null, series, null, null, meta);
    }

    public static WidgetData table(List<String> columns, List<Map<String, Object>> rows,
                                   Map<String, Object> meta) {
        return new WidgetData(Kind.TABLE, null, null, columns, rows, meta);
    }

    public static WidgetData rows(List<Map<String, Object>> rows, Map<String, Object> meta) {
        return new WidgetData(Kind.ROWS, null, null, null, rows, meta);
    }

    public static WidgetData empty() {
        return new WidgetData(Kind.EMPTY, null, null, null, null, null);
    }
}
