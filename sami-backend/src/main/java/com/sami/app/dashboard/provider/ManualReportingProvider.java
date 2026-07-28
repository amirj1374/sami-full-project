package com.sami.app.dashboard.provider;

import com.sami.app.dashboard.spi.ReportingProvider;
import com.sami.app.dashboard.spi.WidgetData;
import com.sami.app.dashboard.spi.WidgetData.SeriesPoint;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Built-in provider for manual/static widgets: it renders values entered
 * directly in the widget configuration. Zero coupling — proves the pipeline and
 * powers "manual" KPI cards and static reference charts.
 *
 * <p>Config shapes:
 * <ul>
 *   <li>{@code {"value": 42}} → scalar</li>
 *   <li>{@code {"series": [{"label":"Jan","value":10}, ...]}} → series</li>
 * </ul>
 */
@Component
public class ManualReportingProvider implements ReportingProvider {

    @Override
    public String key() {
        return "manual";
    }

    @Override
    @SuppressWarnings("unchecked")
    public WidgetData fetch(com.sami.app.dashboard.spi.WidgetDataRequest request) {
        Map<String, Object> config = request.config() == null ? Map.of() : request.config();

        Object series = config.get("series");
        if (series instanceof List<?> list) {
            List<SeriesPoint> points = list.stream()
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<String, Object>) o)
                    .map(m -> new SeriesPoint(
                            String.valueOf(m.getOrDefault("label", "")),
                            toDecimal(m.get("value"))))
                    .toList();
            return WidgetData.series(points, Map.of("source", "manual"));
        }

        return WidgetData.scalar(toDecimal(config.get("value")), Map.of("source", "manual"));
    }

    private BigDecimal toDecimal(Object value) {
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
}
