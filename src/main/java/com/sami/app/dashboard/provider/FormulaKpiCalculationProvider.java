package com.sami.app.dashboard.provider;

import com.sami.app.dashboard.spi.KpiCalculationProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * FORMULA KPIs: the {@code formula} is an arithmetic expression that may
 * reference other KPI codes (resolved to their latest values). Provides both
 * calculation and pre-save formula validation.
 */
@Component
public class FormulaKpiCalculationProvider implements KpiCalculationProvider {

    @Override
    public String method() {
        return "FORMULA";
    }

    @Override
    public BigDecimal calculate(KpiCalculationRequest request) {
        return FormulaEvaluator.evaluate(request.formula(), request.resolveKpi());
    }

    @Override
    public String validate(KpiCalculationRequest request) {
        try {
            // Validate structure with a resolver that treats every reference as 0,
            // so we catch syntax errors without needing live values.
            FormulaEvaluator.evaluate(request.formula(), ref -> BigDecimal.ZERO);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
