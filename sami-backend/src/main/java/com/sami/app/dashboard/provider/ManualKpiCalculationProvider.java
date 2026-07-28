package com.sami.app.dashboard.provider;

import com.sami.app.dashboard.spi.KpiCalculationProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * MANUAL KPIs: the value is a constant held in the {@code formula} field (or 0
 * when blank). Manual KPIs are typically updated by explicit recalculation with
 * a supplied value.
 */
@Component
public class ManualKpiCalculationProvider implements KpiCalculationProvider {

    @Override
    public String method() {
        return "MANUAL";
    }

    @Override
    public BigDecimal calculate(KpiCalculationRequest request) {
        String formula = request.formula();
        if (formula == null || formula.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(formula.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String validate(KpiCalculationRequest request) {
        String formula = request.formula();
        if (formula == null || formula.isBlank()) {
            return null;
        }
        try {
            new BigDecimal(formula.trim());
            return null;
        } catch (NumberFormatException e) {
            return "Manual KPI value must be a number";
        }
    }
}
