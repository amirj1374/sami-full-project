package com.sami.app.dashboard.service;

import com.sami.app.dashboard.domain.DashboardWidget;
import com.sami.app.dashboard.domain.KpiDefinition;
import com.sami.app.dashboard.domain.KpiValue;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetDataResponse;
import com.sami.app.dashboard.repository.DashboardWidgetRepository;
import com.sami.app.dashboard.spi.ReportingProvider;
import com.sami.app.dashboard.spi.ReportingProviderRegistry;
import com.sami.app.dashboard.spi.WidgetData;
import com.sami.app.dashboard.spi.WidgetDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves a widget to its live data at request time. A widget is either
 * KPI-backed (value + thresholds from the KPI engine) or provider-backed (data
 * from the resolved {@link ReportingProvider}). Failures are contained to the
 * single widget — a missing provider or a bad formula yields a widget-level
 * error, never a broken dashboard (edge case: KPI/data unavailable).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetDataService {

    private final DashboardWidgetRepository widgetRepository;
    private final ReportingProviderRegistry providerRegistry;
    private final KpiCalculationService kpiCalculationService;

    @Transactional
    public WidgetDataResponse resolveById(Long widgetId, RefreshContext context) {
        DashboardWidget widget = widgetRepository.findWithDetailsById(widgetId)
                .orElseThrow(() -> com.sami.app.common.exception.ResourceNotFoundException
                        .of("Widget", widgetId));
        return resolve(widget, context);
    }

    /**
     * Computes one widget's data. Never throws for data problems — the failure is
     * reported in {@link WidgetDataResponse#error()} so the rest of the dashboard
     * still renders.
     */
    @Transactional
    public WidgetDataResponse resolve(DashboardWidget widget, RefreshContext context) {
        int refreshInterval = widget.getRefreshPolicy() != null
                ? widget.getRefreshPolicy().getIntervalSeconds() : 0;
        String chartTypeCode = widget.getChartType() != null ? widget.getChartType().getCode() : null;
        try {
            WidgetData data = widget.getKpi() != null
                    ? resolveKpi(widget, context)
                    : resolveProvider(widget, context);
            return new WidgetDataResponse(widget.getId(), widget.getCode(),
                    widget.getWidgetType().getCode(), chartTypeCode, widget.getTitle(),
                    refreshInterval, widget.getConfig(), data, null);
        } catch (Exception ex) {
            log.debug("Widget {} data resolution failed: {}", widget.getId(), ex.getMessage());
            return new WidgetDataResponse(widget.getId(), widget.getCode(),
                    widget.getWidgetType().getCode(), chartTypeCode, widget.getTitle(),
                    refreshInterval, widget.getConfig(), WidgetData.empty(),
                    friendly(ex.getMessage()));
        }
    }

    private WidgetData resolveKpi(DashboardWidget widget, RefreshContext context) {
        KpiDefinition kpi = widget.getKpi();
        KpiValue latest = kpiCalculationService.latest(kpi.getId()).orElse(null);
        java.math.BigDecimal value = latest != null
                ? latest.getValue()
                : kpiCalculationService.calculate(kpi.getId(), context, null).value();
        String level = latest != null
                ? latest.getThresholdLevel()
                : kpiCalculationService.evaluateThreshold(kpi.getThresholds(), value);

        Map<String, Object> meta = new HashMap<>();
        meta.put("kpiCode", kpi.getCode());
        meta.put("unit", kpi.getUnit());
        meta.put("target", kpi.getTargetValue());
        meta.put("thresholdLevel", level);
        meta.put("thresholds", kpi.getThresholds().stream()
                .map(t -> Map.of("level", t.getLevelName(), "color", nullSafe(t.getColor()),
                        "min", t.getMinValue(), "max", t.getMaxValue()))
                .toList());
        return WidgetData.scalar(value, meta);
    }

    private WidgetData resolveProvider(DashboardWidget widget, RefreshContext context) {
        String providerKey = widget.getDataSource() != null
                ? widget.getDataSource().getProviderKey() : "manual";
        ReportingProvider provider = providerRegistry.find(providerKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Data source is not available (no provider '" + providerKey
                                + "' is registered yet)"));
        Map<String, Object> config = widget.getConfig() == null ? Map.of() : widget.getConfig();
        String metric = config.get("metric") != null ? String.valueOf(config.get("metric")) : null;

        WidgetDataRequest request = new WidgetDataRequest(
                providerKey, metric, config,
                context != null ? context.companyId() : null,
                context != null ? context.branchId() : null,
                context != null ? context.salespersonId() : null,
                context != null ? context.from() : null,
                context != null ? context.to() : null,
                context != null && context.filters() != null ? context.filters() : Map.of());
        return provider.fetch(request);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String friendly(String message) {
        return message == null ? "Widget data is unavailable" : message;
    }
}
