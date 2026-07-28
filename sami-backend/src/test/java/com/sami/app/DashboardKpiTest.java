package com.sami.app;

import com.sami.app.dashboard.domain.KpiThreshold;
import com.sami.app.dashboard.provider.FormulaEvaluator;
import com.sami.app.dashboard.service.KpiCalculationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the KPI engine's pure logic: the formula evaluator and
 * threshold-band evaluation. No Spring context or database.
 */
class DashboardKpiTest {

    // --- FormulaEvaluator ----------------------------------------------------

    @Test
    void evaluatesArithmeticWithPrecedenceAndParentheses() {
        assertThat(FormulaEvaluator.evaluate("2 + 3 * 4", ref -> null))
                .isEqualByComparingTo("14");
        assertThat(FormulaEvaluator.evaluate("(2 + 3) * 4", ref -> null))
                .isEqualByComparingTo("20");
        assertThat(FormulaEvaluator.evaluate("-5 + 10", ref -> null))
                .isEqualByComparingTo("5");
    }

    @Test
    void resolvesKpiReferences() {
        Map<String, BigDecimal> values = Map.of("revenue", new BigDecimal("1000"),
                "cost", new BigDecimal("400"));
        BigDecimal profit = FormulaEvaluator.evaluate("revenue - cost", values::get);
        assertThat(profit).isEqualByComparingTo("600");

        BigDecimal margin = FormulaEvaluator.evaluate("(revenue - cost) / revenue * 100", values::get);
        assertThat(margin).isEqualByComparingTo("60");
    }

    @Test
    void rejectsUnknownReferenceDivisionByZeroAndSyntaxErrors() {
        assertThatThrownBy(() -> FormulaEvaluator.evaluate("missing + 1", ref -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown KPI reference");
        assertThatThrownBy(() -> FormulaEvaluator.evaluate("1 / 0", ref -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Division by zero");
        assertThatThrownBy(() -> FormulaEvaluator.evaluate("2 +", ref -> null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Threshold evaluation ------------------------------------------------

    @Test
    void evaluatesThresholdBands() {
        KpiCalculationService service = new KpiCalculationService(null, null, null, null);
        List<KpiThreshold> bands = List.of(
                band("Critical", null, new BigDecimal("50"), 0),
                band("Warning", new BigDecimal("50"), new BigDecimal("80"), 1),
                band("Good", new BigDecimal("80"), null, 2));

        assertThat(service.evaluateThreshold(bands, new BigDecimal("20"))).isEqualTo("Critical");
        assertThat(service.evaluateThreshold(bands, new BigDecimal("50"))).isEqualTo("Warning");
        assertThat(service.evaluateThreshold(bands, new BigDecimal("79.99"))).isEqualTo("Warning");
        assertThat(service.evaluateThreshold(bands, new BigDecimal("80"))).isEqualTo("Good");
        assertThat(service.evaluateThreshold(bands, new BigDecimal("999"))).isEqualTo("Good");
    }

    @Test
    void returnsNullWhenNoBandMatches() {
        KpiCalculationService service = new KpiCalculationService(null, null, null, null);
        List<KpiThreshold> bands = List.of(
                band("Only", new BigDecimal("0"), new BigDecimal("10"), 0));
        assertThat(service.evaluateThreshold(bands, new BigDecimal("50"))).isNull();
    }

    private KpiThreshold band(String name, BigDecimal min, BigDecimal max, int order) {
        return KpiThreshold.builder().levelName(name).minValue(min).maxValue(max).sortOrder(order).build();
    }
}
