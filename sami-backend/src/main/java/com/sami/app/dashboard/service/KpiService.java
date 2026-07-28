package com.sami.app.dashboard.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dashboard.domain.DashKpiStatus;
import com.sami.app.dashboard.domain.KpiDefinition;
import com.sami.app.dashboard.domain.KpiThreshold;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.KpiDtos.KpiCalculationResult;
import com.sami.app.dashboard.dto.KpiDtos.KpiRequest;
import com.sami.app.dashboard.dto.KpiDtos.KpiResponse;
import com.sami.app.dashboard.dto.KpiDtos.KpiValueResponse;
import com.sami.app.dashboard.dto.KpiDtos.ThresholdRequest;
import com.sami.app.dashboard.dto.KpiDtos.ValidationResult;
import com.sami.app.dashboard.event.DashboardDomainEvent;
import com.sami.app.dashboard.repository.DashDataSourceRepository;
import com.sami.app.dashboard.repository.DashKpiStatusRepository;
import com.sami.app.dashboard.repository.DashRefreshPolicyRepository;
import com.sami.app.dashboard.repository.KpiDefinitionRepository;
import com.sami.app.dashboard.spi.KpiCalculationRegistry;
import com.sami.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KPI definition management: CRUD, threshold configuration, formula validation,
 * calculation and historical retrieval. Calculation itself lives in
 * {@link KpiCalculationService}.
 */
@Service
@RequiredArgsConstructor
public class KpiService {

    private static final Set<String> AGGREGATIONS = Set.of("SUM", "AVG", "COUNT", "MIN", "MAX", "LAST");

    private final KpiDefinitionRepository kpiRepository;
    private final DashKpiStatusRepository statusRepository;
    private final DashDataSourceRepository dataSourceRepository;
    private final DashRefreshPolicyRepository refreshPolicyRepository;
    private final UserRepository userRepository;
    private final KpiCalculationRegistry calculationRegistry;
    private final KpiCalculationService calculationService;
    private final DashboardAuditService audit;

    @Transactional(readOnly = true)
    public Page<KpiResponse> list(String search, Long statusId, Pageable pageable) {
        Specification<KpiDefinition> spec = Specification.allOf(
                searchSpec(search), statusSpec(statusId));
        return kpiRepository.findAll(spec, pageable)
                .map(k -> KpiResponse.from(k, calculationService.latest(k.getId()).orElse(null)));
    }

    @Transactional(readOnly = true)
    public KpiResponse getDetail(Long id) {
        KpiDefinition kpi = kpiRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KPI", id));
        return KpiResponse.from(kpi, calculationService.latest(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<KpiValueResponse> history(Long id, int limit) {
        kpiRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("KPI", id));
        return calculationService.history(id, Math.min(Math.max(limit, 1), 500)).stream()
                .map(KpiValueResponse::from).toList();
    }

    @Transactional
    public KpiResponse create(KpiRequest request) {
        if (kpiRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A KPI with code '%s' already exists".formatted(request.code()));
        }
        validateMethodAndAggregation(request);

        KpiDefinition kpi = new KpiDefinition();
        kpi.setCode(request.code());
        applyFields(kpi, request);
        applyThresholds(kpi, request.thresholds());
        requireValidFormula(kpi);

        kpiRepository.save(kpi);
        audit.record(DashboardAuditService.KPI, kpi.getId(), "CREATED",
                DashboardDomainEvent.KPI_CREATED, null, Map.of("code", kpi.getCode(), "name", kpi.getName()));
        return KpiResponse.from(kpi, null);
    }

    @Transactional
    public KpiResponse update(Long id, KpiRequest request) {
        KpiDefinition kpi = kpiRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KPI", id));
        if (request.expectedVersion() != null && !request.expectedVersion().equals(kpi.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The KPI was modified by someone else; reload and retry");
        }
        if (kpiRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A KPI with code '%s' already exists".formatted(request.code()));
        }
        validateMethodAndAggregation(request);

        kpi.setCode(request.code());
        applyFields(kpi, request);
        kpi.getThresholds().clear();
        applyThresholds(kpi, request.thresholds());
        requireValidFormula(kpi);

        audit.record(DashboardAuditService.KPI, id, "UPDATED",
                DashboardDomainEvent.KPI_MODIFIED, null, Map.of("code", kpi.getCode()));
        return KpiResponse.from(kpi, calculationService.latest(id).orElse(null));
    }

    @Transactional
    public void delete(Long id) {
        KpiDefinition kpi = kpiRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KPI", id));
        kpiRepository.delete(kpi);
        audit.record(DashboardAuditService.KPI, id, "DELETED",
                DashboardDomainEvent.KPI_MODIFIED, Map.of("code", kpi.getCode()), null);
    }

    @Transactional(readOnly = true)
    public ValidationResult validate(Long id) {
        KpiDefinition kpi = kpiRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KPI", id));
        String error = calculationService.validateFormula(kpi);
        return error == null ? ValidationResult.ok() : ValidationResult.error(error);
    }

    @Transactional
    public KpiCalculationResult calculate(Long id, RefreshContext context, BigDecimal manualValue) {
        return calculationService.calculate(id, context == null ? RefreshContext.empty() : context,
                manualValue);
    }

    /** Executive KPI report as CSV (code, name, latest value, band, target). */
    @Transactional(readOnly = true)
    public String exportCsv() {
        StringBuilder csv = new StringBuilder(
                "code,name,status,latestValue,thresholdLevel,target,unit,computedAt\n");
        for (KpiDefinition kpi : kpiRepository.findAll()) {
            var latest = calculationService.latest(kpi.getId()).orElse(null);
            csv.append(csvRow(
                    kpi.getCode(), kpi.getName(), kpi.getStatus().getName(),
                    latest != null ? latest.getValue().toPlainString() : "",
                    latest != null && latest.getThresholdLevel() != null ? latest.getThresholdLevel() : "",
                    kpi.getTargetValue() != null ? kpi.getTargetValue().toPlainString() : "",
                    kpi.getUnit() != null ? kpi.getUnit() : "",
                    latest != null ? latest.getComputedAt().toString() : ""));
        }
        return csv.toString();
    }

    private String csvRow(String... cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                row.append(',');
            }
            String cell = cells[i] == null ? "" : cells[i];
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                cell = '"' + cell.replace("\"", "\"\"") + '"';
            }
            row.append(cell);
        }
        return row.append('\n').toString();
    }

    // -------------------------------------------------------------- internals

    private void applyFields(KpiDefinition kpi, KpiRequest r) {
        kpi.setName(r.name());
        kpi.setDescription(r.description());
        kpi.setCalculationMethod(r.calculationMethod());
        kpi.setFormula(r.formula());
        kpi.setAggregation(r.aggregation().toUpperCase());
        kpi.setDataSource(r.dataSourceId() == null ? null : dataSourceRepository.findById(r.dataSourceId())
                .orElseThrow(() -> ResourceNotFoundException.of("Data source", r.dataSourceId())));
        kpi.setRefreshPolicy(r.refreshPolicyId() == null ? null
                : refreshPolicyRepository.findById(r.refreshPolicyId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Refresh policy", r.refreshPolicyId())));
        kpi.setTargetValue(r.targetValue());
        kpi.setUnit(r.unit());
        kpi.setStatus(resolveStatus(r.statusId()));
        kpi.setOwner(r.ownerId() == null ? null : userRepository.findById(r.ownerId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", r.ownerId())));
        if (kpi.getCreatedBy() == null) {
            kpi.setCreatedBy(com.sami.app.security.CurrentActor.id());
            kpi.setCreatedByEmail(com.sami.app.security.CurrentActor.email());
        }
    }

    private void applyThresholds(KpiDefinition kpi, List<ThresholdRequest> thresholds) {
        if (thresholds == null) {
            return;
        }
        for (ThresholdRequest t : thresholds) {
            if (t.minValue() != null && t.maxValue() != null
                    && t.minValue().compareTo(t.maxValue()) > 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Threshold '%s' has min greater than max".formatted(t.levelName()));
            }
            kpi.getThresholds().add(KpiThreshold.builder()
                    .kpi(kpi).levelName(t.levelName()).color(t.color())
                    .minValue(t.minValue()).maxValue(t.maxValue()).sortOrder(t.sortOrder())
                    .build());
        }
    }

    private void validateMethodAndAggregation(KpiRequest r) {
        if (calculationRegistry.find(r.calculationMethod()).isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Unknown calculation method '%s'".formatted(r.calculationMethod()));
        }
        if (!AGGREGATIONS.contains(r.aggregation().toUpperCase())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Aggregation must be one of " + AGGREGATIONS);
        }
    }

    private void requireValidFormula(KpiDefinition kpi) {
        String error = calculationService.validateFormula(kpi);
        if (error != null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid KPI formula: " + error);
        }
    }

    private DashKpiStatus resolveStatus(Long statusId) {
        if (statusId != null) {
            return statusRepository.findById(statusId)
                    .orElseThrow(() -> ResourceNotFoundException.of("KPI status", statusId));
        }
        return statusRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default KPI status is configured"));
    }

    private Specification<KpiDefinition> searchSpec(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern));
    }

    private Specification<KpiDefinition> statusSpec(Long statusId) {
        return statusId == null ? null
                : (root, query, cb) -> cb.equal(root.get("status").get("id"), statusId);
    }
}
