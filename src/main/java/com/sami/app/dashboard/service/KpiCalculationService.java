package com.sami.app.dashboard.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dashboard.domain.KpiDefinition;
import com.sami.app.dashboard.domain.KpiThreshold;
import com.sami.app.dashboard.domain.KpiValue;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.KpiDtos.KpiCalculationResult;
import com.sami.app.dashboard.event.DashboardDomainEvent;
import com.sami.app.dashboard.repository.KpiDefinitionRepository;
import com.sami.app.dashboard.repository.KpiValueRepository;
import com.sami.app.dashboard.spi.KpiCalculationProvider;
import com.sami.app.dashboard.spi.KpiCalculationProvider.KpiCalculationRequest;
import com.sami.app.dashboard.spi.KpiCalculationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The KPI engine: computes a KPI's value via its calculation strategy, evaluates
 * the configured threshold bands, stores the result historically, and publishes
 * {@code KPICalculated} / {@code ThresholdExceeded} events. Formula validation
 * is delegated to the selected {@link KpiCalculationProvider}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiCalculationService {

    private final KpiDefinitionRepository kpiRepository;
    private final KpiValueRepository valueRepository;
    private final KpiCalculationRegistry calculationRegistry;
    private final DashboardAuditService audit;

    /** Latest stored value of a KPI (no recomputation). */
    @Transactional(readOnly = true)
    public Optional<KpiValue> latest(Long kpiId) {
        return valueRepository.findTopByKpiIdOrderByComputedAtDesc(kpiId);
    }

    @Transactional(readOnly = true)
    public List<KpiValue> history(Long kpiId, int limit) {
        return valueRepository.findByKpiIdOrderByComputedAtDesc(kpiId, PageRequest.of(0, limit));
    }

    /** Validates a KPI's formula against its calculation strategy. */
    @Transactional(readOnly = true)
    public String validateFormula(KpiDefinition kpi) {
        KpiCalculationProvider provider = calculationRegistry.find(kpi.getCalculationMethod())
                .orElse(null);
        if (provider == null) {
            return "Unknown calculation method '" + kpi.getCalculationMethod() + "'";
        }
        return provider.validate(buildRequest(kpi, RefreshContext.empty()));
    }

    /**
     * Computes and stores the KPI's current value, evaluating thresholds and
     * publishing events. {@code manualOverride} (optional) supplies the value
     * for MANUAL KPIs directly.
     */
    @Transactional
    public KpiCalculationResult calculate(Long kpiId, RefreshContext context, BigDecimal manualOverride) {
        KpiDefinition kpi = kpiRepository.findWithDetailsById(kpiId)
                .orElseThrow(() -> ResourceNotFoundException.of("KPI", kpiId));

        BigDecimal value;
        if (manualOverride != null) {
            value = manualOverride;
        } else {
            KpiCalculationProvider provider = calculationRegistry.find(kpi.getCalculationMethod())
                    .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                            "Unknown calculation method '" + kpi.getCalculationMethod() + "'"));
            try {
                value = provider.calculate(buildRequest(kpi, context));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                // Edge case: formula references unavailable data / provider missing.
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "KPI '%s' could not be calculated: %s".formatted(kpi.getCode(), ex.getMessage()));
            }
        }

        String previousLevel = valueRepository.findTopByKpiIdOrderByComputedAtDesc(kpiId)
                .map(KpiValue::getThresholdLevel).orElse(null);
        String level = evaluateThreshold(kpi.getThresholds(), value);

        valueRepository.save(KpiValue.builder()
                .kpiId(kpiId).value(value).periodKey("ALL").thresholdLevel(level).build());

        Map<String, Object> payload = new HashMap<>();
        payload.put("kpiId", kpiId);
        payload.put("code", kpi.getCode());
        payload.put("value", value);
        payload.put("thresholdLevel", level);
        audit.publish(DashboardDomainEvent.KPI_CALCULATED, DashboardAuditService.KPI, kpiId, payload);

        // A change of threshold band is the "threshold exceeded" signal.
        if (level != null && !level.equals(previousLevel)) {
            Map<String, Object> tp = new HashMap<>(payload);
            tp.put("previousLevel", previousLevel);
            audit.publish(DashboardDomainEvent.THRESHOLD_EXCEEDED, DashboardAuditService.KPI, kpiId, tp);
        }

        BigDecimal attainment = kpi.getTargetValue() != null
                && kpi.getTargetValue().signum() != 0
                ? value.multiply(BigDecimal.valueOf(100))
                        .divide(kpi.getTargetValue(), MathContext.DECIMAL64)
                : null;

        return new KpiCalculationResult(kpiId, kpi.getCode(), value, level,
                kpi.getTargetValue(), attainment, java.time.Instant.now());
    }

    /** The band whose {@code [min, max)} range contains the value (null = none). */
    public String evaluateThreshold(List<KpiThreshold> thresholds, BigDecimal value) {
        return thresholds.stream()
                .filter(t -> (t.getMinValue() == null || value.compareTo(t.getMinValue()) >= 0)
                        && (t.getMaxValue() == null || value.compareTo(t.getMaxValue()) < 0))
                .map(KpiThreshold::getLevelName)
                .findFirst()
                .orElse(null);
    }

    private KpiCalculationRequest buildRequest(KpiDefinition kpi, RefreshContext context) {
        Function<String, BigDecimal> resolver = code -> kpiRepository.findByCode(code)
                .flatMap(k -> valueRepository.findTopByKpiIdOrderByComputedAtDesc(k.getId()))
                .map(KpiValue::getValue)
                .orElse(null);
        Map<String, Object> ctx = new HashMap<>();
        if (context != null) {
            if (context.companyId() != null) {
                ctx.put("companyId", context.companyId());
            }
            if (context.branchId() != null) {
                ctx.put("branchId", context.branchId());
            }
            if (context.from() != null) {
                ctx.put("from", context.from().toString());
            }
            if (context.to() != null) {
                ctx.put("to", context.to().toString());
            }
        }
        return new KpiCalculationRequest(
                kpi.getFormula(),
                kpi.getDataSource() != null ? kpi.getDataSource().getProviderKey() : null,
                kpi.getAggregation(),
                ctx,
                resolver);
    }
}
