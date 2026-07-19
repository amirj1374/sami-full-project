package com.sami.app.dashboard.provider;

import com.sami.app.dashboard.repository.DashboardRepository;
import com.sami.app.dashboard.repository.DashboardWidgetRepository;
import com.sami.app.dashboard.repository.KpiDefinitionRepository;
import com.sami.app.dashboard.spi.ReportingProvider;
import com.sami.app.dashboard.spi.WidgetData;
import com.sami.app.dashboard.spi.WidgetDataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Built-in provider exposing self-contained dashboard-module metrics
 * (counts of dashboards, KPIs, widgets). Reads only this module's own
 * repositories, so it demonstrates a DB-backed provider without reaching into
 * any business module. Metrics are selected via the widget config's
 * {@code metric} key.
 */
@Component
@RequiredArgsConstructor
public class SystemReportingProvider implements ReportingProvider {

    private final DashboardRepository dashboardRepository;
    private final KpiDefinitionRepository kpiRepository;
    private final DashboardWidgetRepository widgetRepository;

    @Override
    public String key() {
        return "system";
    }

    @Override
    public WidgetData fetch(WidgetDataRequest request) {
        String metric = request.metric() != null ? request.metric()
                : String.valueOf(request.config() == null ? "" : request.config().getOrDefault("metric", ""));
        long count = switch (metric) {
            case "dashboards.count" -> dashboardRepository.count();
            case "kpis.count" -> kpiRepository.count();
            case "widgets.count" -> widgetRepository.count();
            default -> 0L;
        };
        return WidgetData.scalar(BigDecimal.valueOf(count), Map.of("metric", metric, "source", "system"));
    }
}
