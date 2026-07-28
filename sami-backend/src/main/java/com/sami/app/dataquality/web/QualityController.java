package com.sami.app.dataquality.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.dataquality.domain.QualityScoreBand;
import com.sami.app.dataquality.dto.QualityDtos.CatalogResponse;
import com.sami.app.dataquality.dto.QualityDtos.CorrectionRequest;
import com.sami.app.dataquality.dto.QualityDtos.CorrectionResponse;
import com.sami.app.dataquality.dto.QualityDtos.DuplicateCheckRequest;
import com.sami.app.dataquality.dto.QualityDtos.IssueResponse;
import com.sami.app.dataquality.dto.QualityDtos.ResolveRequest;
import com.sami.app.dataquality.dto.QualityDtos.RuleRequest;
import com.sami.app.dataquality.dto.QualityDtos.RuleResponse;
import com.sami.app.dataquality.dto.QualityDtos.StatusChangeRequest;
import com.sami.app.dataquality.dto.QualityDtos.ValidateRequest;
import com.sami.app.dataquality.engine.ValidationEngine;
import com.sami.app.dataquality.repository.QualityDimensionRepository;
import com.sami.app.dataquality.repository.QualitySeverityRepository;
import com.sami.app.dataquality.repository.QualityStatusRepository;
import com.sami.app.dataquality.service.DuplicateDetectionService;
import com.sami.app.dataquality.service.DuplicateResult;
import com.sami.app.dataquality.service.QualityIssueService;
import com.sami.app.dataquality.service.QualityRuleService;
import com.sami.app.dataquality.service.QualityScoreService;
import com.sami.app.dataquality.service.ValidationReport;
import com.sami.app.dataquality.spi.DuplicateCandidateRegistry;
import com.sami.app.dataquality.spi.DuplicateMatcher;
import com.sami.app.dataquality.spi.DuplicateMatcherRegistry;
import com.sami.app.dataquality.spi.ValidationRule;
import com.sami.app.dataquality.spi.ValidationRuleRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Data quality API. Business modules call {@code POST /validate} (or the
 * services directly) instead of embedding validation logic.
 */
@RestController
@RequestMapping("/api/v1/quality")
@RequiredArgsConstructor
@Tag(name = "Data Quality", description = "Quality rules, validation, duplicates, issues and scoring")
public class QualityController {

    private final QualityRuleService ruleService;
    private final QualityIssueService issueService;
    private final QualityScoreService scoreService;
    private final ValidationEngine validationEngine;
    private final DuplicateDetectionService duplicateDetection;
    private final ValidationRuleRegistry validatorRegistry;
    private final DuplicateMatcherRegistry matcherRegistry;
    private final DuplicateCandidateRegistry candidateRegistry;
    private final QualityStatusRepository statusRepository;
    private final QualitySeverityRepository severityRepository;
    private final QualityDimensionRepository dimensionRepository;

    // ---- Rules --------------------------------------------------------------

    @GetMapping("/rules")
    @PreAuthorize("@authz.has('data-quality:view')")
    @Operation(summary = "List quality rules")
    public ApiResponse<List<RuleResponse>> rules() {
        return ApiResponse.ok(ruleService.list().stream().map(RuleResponse::from).toList());
    }

    @GetMapping("/rules/{id}")
    @PreAuthorize("@authz.has('data-quality:view')")
    @Operation(summary = "Get a quality rule")
    public ApiResponse<RuleResponse> rule(@PathVariable Long id) {
        return ApiResponse.ok(RuleResponse.from(ruleService.get(id)));
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('data-quality:create')")
    @Operation(summary = "Create a quality rule")
    public ApiResponse<RuleResponse> createRule(@Valid @RequestBody RuleRequest r) {
        return ApiResponse.ok(RuleResponse.from(ruleService.create(r.code(), r.name(), r.description(),
                r.moduleCode(), r.entityCode(), r.category(), r.priority(), r.statusCode(),
                r.severityCode(), r.dimensionCode(), r.validationType(), r.targetField(),
                r.config(), r.conditionConfig(), r.weight())));
    }

    @PatchMapping("/rules/{id}/status")
    @PreAuthorize("@authz.has('data-quality:edit')")
    @Operation(summary = "Change a rule's status")
    public ApiResponse<RuleResponse> changeRuleStatus(@PathVariable Long id,
                                                      @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(RuleResponse.from(ruleService.changeStatus(id, request.statusCode())));
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('data-quality:delete')")
    @Operation(summary = "Delete a quality rule")
    public void deleteRule(@PathVariable Long id) {
        ruleService.delete(id);
    }

    // ---- Validation ---------------------------------------------------------

    @PostMapping("/validate")
    @PreAuthorize("@authz.has('data-quality:execute')")
    @Operation(summary = "Validate an entity payload against its configured rules")
    public ApiResponse<ValidationReport> validate(@Valid @RequestBody ValidateRequest request) {
        return ApiResponse.ok(validationEngine.validate(request.moduleCode(), request.entityCode(),
                request.entityId(), request.data() == null ? Map.of() : request.data(),
                request.persist() == null || request.persist()));
    }

    @PostMapping("/duplicates/check")
    @PreAuthorize("@authz.has('data-quality:execute')")
    @Operation(summary = "Check a payload for duplicates")
    public ApiResponse<DuplicateResult> checkDuplicates(@Valid @RequestBody DuplicateCheckRequest request) {
        return ApiResponse.ok(duplicateDetection.detect(request.moduleCode(), request.entityCode(),
                request.field(), request.data() == null ? Map.of() : request.data(),
                request.excludeId(), request.config() == null ? Map.of() : request.config()));
    }

    // ---- Issues -------------------------------------------------------------

    @GetMapping("/issues")
    @PreAuthorize("@authz.has('data-quality:view')")
    @Operation(summary = "List quality issues")
    public ApiResponse<PageResponse<IssueResponse>> issues(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(issueService.list(status, pageable), IssueResponse::from));
    }

    @PostMapping("/issues/{id}/resolve")
    @PreAuthorize("@authz.has('data-quality:resolve')")
    @Operation(summary = "Resolve an issue")
    public ApiResponse<IssueResponse> resolve(@PathVariable Long id,
                                              @RequestBody(required = false) ResolveRequest request) {
        return ApiResponse.ok(IssueResponse.from(
                issueService.resolve(id, request == null ? null : request.note())));
    }

    @PostMapping("/issues/{id}/ignore")
    @PreAuthorize("@authz.has('data-quality:resolve')")
    @Operation(summary = "Ignore an issue")
    public ApiResponse<IssueResponse> ignore(@PathVariable Long id,
                                             @RequestBody(required = false) ResolveRequest request) {
        return ApiResponse.ok(IssueResponse.from(
                issueService.ignore(id, request == null ? null : request.note())));
    }

    @PostMapping("/issues/{id}/corrections")
    @PreAuthorize("@authz.has('data-quality:manage-corrections')")
    @Operation(summary = "Record a correction for an issue")
    public ApiResponse<CorrectionResponse> correct(@PathVariable Long id,
                                                   @RequestBody CorrectionRequest request) {
        return ApiResponse.ok(CorrectionResponse.from(issueService.correct(id, request.field(),
                request.oldValue(), request.newValue(), request.automatic(), request.note())));
    }

    @GetMapping("/issues/summary")
    @PreAuthorize("@authz.has('data-quality:view')")
    @Operation(summary = "Issue counts by status")
    public ApiResponse<Map<String, Long>> issueSummary() {
        return ApiResponse.ok(issueService.summary());
    }

    // ---- Scores & catalog ---------------------------------------------------

    @GetMapping("/scores/trend")
    @PreAuthorize("@authz.has('data-quality:view')")
    @Operation(summary = "Quality score trend for an entity")
    public ApiResponse<List<Map<String, Object>>> trend(@RequestParam String moduleCode,
                                                        @RequestParam String entityCode) {
        return ApiResponse.ok(scoreService.trend(moduleCode, entityCode).stream()
                .map(s -> Map.<String, Object>of(
                        "score", s.getScore(),
                        "band", String.valueOf(s.getBandCode()),
                        "computedAt", s.getComputedAt()))
                .toList());
    }

    @GetMapping("/catalog")
    @PreAuthorize("@authz.has('data-quality:view')")
    @Operation(summary = "Configurable catalogues and registered plugins")
    public ApiResponse<CatalogResponse> catalog() {
        return ApiResponse.ok(new CatalogResponse(
                validatorRegistry.all().stream().map(ValidationRule::type).toList(),
                matcherRegistry.all().stream().map(DuplicateMatcher::strategy).toList(),
                dimensionRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(d -> d.getCode()).toList(),
                severityRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(s -> s.getCode()).toList(),
                statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(s -> s.getCode()).toList(),
                scoreService.bands().stream().map(QualityScoreBand::getCode).toList(),
                candidateRegistry.registeredTargets()));
    }
}
