package com.sami.app.dashboard.dto;

import com.sami.app.dashboard.domain.KpiDefinition;
import com.sami.app.dashboard.domain.KpiThreshold;
import com.sami.app.dashboard.domain.KpiValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request/response payloads for KPI definitions, thresholds and values. */
public final class KpiDtos {

    private KpiDtos() {
    }

    public record ThresholdResponse(Long id, String levelName, String color,
                                    BigDecimal minValue, BigDecimal maxValue, int sortOrder) {
        public static ThresholdResponse from(KpiThreshold t) {
            return new ThresholdResponse(t.getId(), t.getLevelName(), t.getColor(),
                    t.getMinValue(), t.getMaxValue(), t.getSortOrder());
        }
    }

    public record KpiResponse(
            Long id, String code, String name, String description,
            String calculationMethod, String formula, String aggregation,
            Long dataSourceId, String dataSourceCode,
            Long refreshPolicyId, String refreshPolicyCode,
            BigDecimal targetValue, String unit,
            Long statusId, String statusCode, String statusName,
            Long ownerId, Long version,
            List<ThresholdResponse> thresholds,
            BigDecimal latestValue, String latestThresholdLevel, Instant latestComputedAt) {

        public static KpiResponse from(KpiDefinition k, KpiValue latest) {
            return new KpiResponse(
                    k.getId(), k.getCode(), k.getName(), k.getDescription(),
                    k.getCalculationMethod(), k.getFormula(), k.getAggregation(),
                    k.getDataSource() != null ? k.getDataSource().getId() : null,
                    k.getDataSource() != null ? k.getDataSource().getCode() : null,
                    k.getRefreshPolicy() != null ? k.getRefreshPolicy().getId() : null,
                    k.getRefreshPolicy() != null ? k.getRefreshPolicy().getCode() : null,
                    k.getTargetValue(), k.getUnit(),
                    k.getStatus().getId(), k.getStatus().getCode(), k.getStatus().getName(),
                    k.getOwner() != null ? k.getOwner().getId() : null,
                    k.getVersion(),
                    k.getThresholds().stream().map(ThresholdResponse::from).toList(),
                    latest != null ? latest.getValue() : null,
                    latest != null ? latest.getThresholdLevel() : null,
                    latest != null ? latest.getComputedAt() : null);
        }
    }

    public record ThresholdRequest(
            @NotBlank @Size(max = 64) String levelName,
            @Size(max = 32) String color,
            BigDecimal minValue,
            BigDecimal maxValue,
            int sortOrder) {
    }

    public record KpiRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                    message = "Code must be a lowercase slug") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 32) String calculationMethod,
            @Size(max = 1000) String formula,
            @NotBlank @Size(max = 16) String aggregation,
            Long dataSourceId,
            Long refreshPolicyId,
            BigDecimal targetValue,
            @Size(max = 32) String unit,
            Long statusId,
            Long ownerId,
            @Valid List<ThresholdRequest> thresholds,
            Long expectedVersion) {
    }

    public record KpiValueResponse(BigDecimal value, String periodKey, String thresholdLevel,
                                   Instant computedAt) {
        public static KpiValueResponse from(KpiValue v) {
            return new KpiValueResponse(v.getValue(), v.getPeriodKey(), v.getThresholdLevel(),
                    v.getComputedAt());
        }
    }

    /** Result of a KPI calculation: the value, its band, and target attainment. */
    public record KpiCalculationResult(
            Long kpiId, String code, BigDecimal value, String thresholdLevel,
            BigDecimal target, BigDecimal attainmentPercent, Instant computedAt) {
    }

    /** Formula-validation outcome (KPI engine: formula validation). */
    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
