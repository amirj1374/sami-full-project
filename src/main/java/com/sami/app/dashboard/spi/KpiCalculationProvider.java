package com.sami.app.dashboard.spi;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Extension point for KPI calculation strategies. A KPI's
 * {@code calculation_method} selects the provider; the dashboard ships PROVIDER,
 * FORMULA and MANUAL strategies, and new strategies (e.g. a future
 * PREDICTIVE/AI forecaster) register as beans without touching the KPI engine.
 */
public interface KpiCalculationProvider {

    /** Strategy key matching {@code kpi_definitions.calculation_method}. */
    String method();

    /** Computes the KPI's current value for the given request. */
    BigDecimal calculate(KpiCalculationRequest request);

    /**
     * Optional cheap validation of a KPI definition before it is saved
     * (e.g. formula parses). Default: always valid.
     *
     * @return null if valid, otherwise a human-readable error message
     */
    default String validate(KpiCalculationRequest request) {
        return null;
    }

    /**
     * The inputs available to a KPI calculation.
     *
     * @param formula        the KPI's formula / metric expression
     * @param dataProviderKey the resolved data-source provider key (may be null)
     * @param aggregation    aggregation hint (SUM/AVG/COUNT/MIN/MAX/LAST)
     * @param context        filter context (company/branch/date range as a map)
     * @param resolveKpi     resolves another KPI's latest value by code (for formulas)
     */
    record KpiCalculationRequest(
            String formula,
            String dataProviderKey,
            String aggregation,
            Map<String, Object> context,
            java.util.function.Function<String, BigDecimal> resolveKpi
    ) {
    }
}
