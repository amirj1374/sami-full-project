package com.sami.app.dataquality.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.dataquality.domain.QualityRule;
import com.sami.app.dataquality.repository.QualityDimensionRepository;
import com.sami.app.dataquality.repository.QualityRuleRepository;
import com.sami.app.dataquality.repository.QualitySeverityRepository;
import com.sami.app.dataquality.repository.QualityStatusRepository;
import com.sami.app.dataquality.spi.ValidationRule;
import com.sami.app.dataquality.spi.ValidationRuleRegistry;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quality-rule configuration. Rules are validated against the plugin registry on
 * save, so a rule can never reference an unknown validation type or carry an
 * invalid config.
 */
@Service
@RequiredArgsConstructor
public class QualityRuleService {

    private final QualityRuleRepository ruleRepository;
    private final QualityStatusRepository statusRepository;
    private final QualitySeverityRepository severityRepository;
    private final QualityDimensionRepository dimensionRepository;
    private final ValidationRuleRegistry validatorRegistry;
    private final QualityAuditService audit;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<QualityRule> list() {
        return ruleRepository.findAllVisible(tenantContext.requireTenantId());
    }

    @Transactional(readOnly = true)
    public QualityRule get(Long id) {
        return ruleRepository.findWithDetailsByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Quality rule not found: " + id));
    }

    @Transactional
    public QualityRule create(String code, String name, String description, String moduleCode,
                              String entityCode, String category, Integer priority, String statusCode,
                              String severityCode, String dimensionCode, String validationType,
                              String targetField, Map<String, Object> config,
                              Map<String, Object> conditionConfig, BigDecimal weight) {
        Long tenantId = tenantContext.requireTenantId();
        if (ruleRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Quality rule code already exists: " + code);
        }
        ValidationRule validator = validatorRegistry.find(validationType)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Unknown validation type: " + validationType));
        Map<String, Object> safeConfig = config == null ? new HashMap<>() : new HashMap<>(config);
        String configError = validator.validateConfig(safeConfig);
        if (configError != null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, configError);
        }

        QualityRule rule = QualityRule.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .description(description)
                .moduleCode(moduleCode)
                .entityCode(entityCode)
                .category(category)
                .priority(priority == null ? 100 : priority)
                .status(statusRepository.findByCode(statusCode == null ? "draft" : statusCode)
                        .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown status")))
                .severity(severityRepository.findByCode(severityCode == null ? "warning" : severityCode)
                        .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown severity")))
                .dimension(dimensionCode == null
                        ? dimensionRepository.findByCode(validator.defaultDimension()).orElse(null)
                        : dimensionRepository.findByCode(dimensionCode).orElse(null))
                .validationType(validationType)
                .targetField(targetField)
                .config(safeConfig)
                .conditionConfig(conditionConfig == null ? new HashMap<>() : new HashMap<>(conditionConfig))
                .weight(weight == null ? BigDecimal.ONE : weight)
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();
        QualityRule saved = ruleRepository.save(rule);
        audit.record("RULE", saved.getId(), "CREATED", null,
                Map.of("code", code, "validationType", validationType));
        return saved;
    }

    @Transactional
    public QualityRule changeStatus(Long id, String statusCode) {
        QualityRule rule = get(id);
        String from = rule.getStatus().getCode();
        rule.setStatus(statusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown status: " + statusCode)));
        QualityRule saved = ruleRepository.save(rule);
        audit.record("RULE", id, "STATUS_CHANGED", Map.of("status", from), Map.of("status", statusCode));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        QualityRule rule = get(id);
        audit.record("RULE", id, "DELETED", Map.of("code", rule.getCode()), null);
        ruleRepository.delete(rule);
    }
}
