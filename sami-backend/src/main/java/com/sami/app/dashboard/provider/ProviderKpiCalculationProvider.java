package com.sami.app.dashboard.provider;

import com.sami.app.dashboard.spi.KpiCalculationProvider;
import com.sami.app.dashboard.spi.ReportingProvider;
import com.sami.app.dashboard.spi.ReportingProviderRegistry;
import com.sami.app.dashboard.spi.WidgetData;
import com.sami.app.dashboard.spi.WidgetDataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * PROVIDER KPIs: the value comes from a {@link ReportingProvider} (resolved via
 * the KPI's data source). The {@code formula} field holds the metric key the
 * provider should compute. This is how a KPI is backed by live business data
 * from another module without any coupling here.
 */
@Component
@RequiredArgsConstructor
public class ProviderKpiCalculationProvider implements KpiCalculationProvider {

    private final ReportingProviderRegistry registry;

    @Override
    public String method() {
        return "PROVIDER";
    }

    @Override
    public BigDecimal calculate(KpiCalculationRequest request) {
        ReportingProvider provider = registry.find(request.dataProviderKey())
                .orElseThrow(() -> new IllegalStateException(
                        "No reporting provider is registered for data source '"
                                + request.dataProviderKey() + "'"));
        WidgetData data = provider.fetch(new WidgetDataRequest(
                request.dataProviderKey(), request.formula(),
                Map.of("metric", request.formula() == null ? "" : request.formula()),
                null, null, null, null, null,
                request.context() == null ? Map.of() : request.context()));

        // A scalar source is already aggregated; a series is reduced per the
        // KPI's aggregation rule (SUM/AVG/COUNT/MIN/MAX/LAST).
        if (data.value() != null) {
            return data.value();
        }
        if (data.series() != null && !data.series().isEmpty()) {
            return aggregate(data.series().stream().map(WidgetData.SeriesPoint::value).toList(),
                    request.aggregation());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal aggregate(List<BigDecimal> values, String aggregation) {
        String agg = aggregation == null ? "LAST" : aggregation.toUpperCase();
        return switch (agg) {
            case "SUM" -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "AVG" -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), java.math.MathContext.DECIMAL64);
            case "COUNT" -> BigDecimal.valueOf(values.size());
            case "MIN" -> values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case "MAX" -> values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            default -> values.get(values.size() - 1); // LAST
        };
    }

    @Override
    public String validate(KpiCalculationRequest request) {
        if (request.dataProviderKey() == null || request.dataProviderKey().isBlank()) {
            return "A PROVIDER KPI requires a data source";
        }
        return null;
    }
}
