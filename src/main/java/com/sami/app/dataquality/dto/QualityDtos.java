package com.sami.app.dataquality.dto;

import com.sami.app.dataquality.domain.QualityCorrection;
import com.sami.app.dataquality.domain.QualityIssue;
import com.sami.app.dataquality.domain.QualityRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request/response records for the data-quality module. */
public final class QualityDtos {

    private QualityDtos() {
    }

    // ---- Requests -----------------------------------------------------------

    public record RuleRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$", message = "code must be a lowercase slug") String code,
            @NotBlank String name,
            String description,
            @NotBlank String moduleCode,
            @NotBlank String entityCode,
            String category,
            Integer priority,
            String statusCode,
            String severityCode,
            String dimensionCode,
            @NotBlank String validationType,
            String targetField,
            Map<String, Object> config,
            Map<String, Object> conditionConfig,
            BigDecimal weight
    ) {
    }

    public record ValidateRequest(
            @NotBlank String moduleCode,
            @NotBlank String entityCode,
            Long entityId,
            Map<String, Object> data,
            Boolean persist
    ) {
    }

    public record DuplicateCheckRequest(
            @NotBlank String moduleCode,
            @NotBlank String entityCode,
            @NotBlank String field,
            Long excludeId,
            Map<String, Object> data,
            Map<String, Object> config
    ) {
    }

    public record StatusChangeRequest(@NotBlank String statusCode) {
    }

    public record ResolveRequest(String note) {
    }

    public record CorrectionRequest(String field, String oldValue, String newValue,
                                    boolean automatic, String note) {
    }

    // ---- Responses ----------------------------------------------------------

    public record RuleResponse(Long id, String code, String name, String description, String moduleCode,
                               String entityCode, String category, int priority, String statusCode,
                               String severityCode, String dimensionCode, String validationType,
                               String targetField, Map<String, Object> config,
                               Map<String, Object> conditionConfig, BigDecimal weight,
                               Instant createdAt, long version) {
        public static RuleResponse from(QualityRule r) {
            return new RuleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                    r.getModuleCode(), r.getEntityCode(), r.getCategory(), r.getPriority(),
                    r.getStatus().getCode(), r.getSeverity().getCode(),
                    r.getDimension() == null ? null : r.getDimension().getCode(),
                    r.getValidationType(), r.getTargetField(), r.getConfig(), r.getConditionConfig(),
                    r.getWeight(), r.getCreatedAt(), r.getVersion());
        }
    }

    public record IssueResponse(Long id, Long ruleId, String moduleCode, String entityCode, Long entityId,
                                String fieldName, String severityCode, String dimensionCode, String message,
                                Map<String, Object> detail, String status, String resolutionNote,
                                Instant resolvedAt, String resolvedByEmail, Instant createdAt) {
        public static IssueResponse from(QualityIssue i) {
            return new IssueResponse(i.getId(), i.getRuleId(), i.getModuleCode(), i.getEntityCode(),
                    i.getEntityId(), i.getFieldName(), i.getSeverityCode(), i.getDimensionCode(),
                    i.getMessage(), i.getDetail(), i.getStatus(), i.getResolutionNote(),
                    i.getResolvedAt(), i.getResolvedByEmail(), i.getCreatedAt());
        }
    }

    public record CorrectionResponse(Long id, Long issueId, String fieldName, String oldValue,
                                     String newValue, boolean automatic, String note,
                                     String appliedByEmail, Instant appliedAt) {
        public static CorrectionResponse from(QualityCorrection c) {
            return new CorrectionResponse(c.getId(), c.getIssueId(), c.getFieldName(), c.getOldValue(),
                    c.getNewValue(), c.isAutomatic(), c.getNote(), c.getAppliedByEmail(), c.getAppliedAt());
        }
    }

    public record CatalogResponse(List<String> validationTypes, List<String> duplicateStrategies,
                                  List<String> dimensions, List<String> severities, List<String> statuses,
                                  List<String> scoreBands, List<String> duplicateProviders) {
    }
}
