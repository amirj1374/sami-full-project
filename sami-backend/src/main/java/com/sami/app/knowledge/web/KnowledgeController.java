package com.sami.app.knowledge.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.knowledge.domain.ArticleVersion;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.domain.Sop;
import com.sami.app.knowledge.dto.KnowledgeDtos.ApprovalResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.ArticleResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.CatalogResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.CategoryResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.CreateArticleRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.DecisionRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.DeprecateRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.LinkArticleRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.LinkEntityRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.NewVersionRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.RelationResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.RelationTypeResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.RiskLevelResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.RoleResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.RolesRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.SopRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.SopResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.StageResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.StatusResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.StepResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.StepTypeResponse;
import com.sami.app.knowledge.dto.KnowledgeDtos.StepsRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.UpdateArticleRequest;
import com.sami.app.knowledge.dto.KnowledgeDtos.VersionResponse;
import com.sami.app.knowledge.repository.ArticleVersionRepository;
import com.sami.app.knowledge.repository.KbApprovalStageRepository;
import com.sami.app.knowledge.repository.KbCategoryRepository;
import com.sami.app.knowledge.repository.KbRelationTypeRepository;
import com.sami.app.knowledge.repository.KbRiskLevelRepository;
import com.sami.app.knowledge.repository.KbStatusRepository;
import com.sami.app.knowledge.repository.KbStepTypeRepository;
import com.sami.app.knowledge.repository.KnowledgeArticleRepository;
import com.sami.app.knowledge.service.ApprovalService;
import com.sami.app.knowledge.service.ArticleService;
import com.sami.app.knowledge.service.KnowledgeReportService;
import com.sami.app.knowledge.service.RelationService;
import com.sami.app.knowledge.service.SopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST surface for the knowledge base.
 *
 * <p>Articles are addressed by {@code articleCode}, which is the identifier
 * business modules hold — database ids are never part of the contract.
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base", description = "Articles, standard operating procedures and policies")
public class KnowledgeController {

    private final ArticleService articleService;
    private final ApprovalService approvalService;
    private final SopService sopService;
    private final RelationService relationService;
    private final KnowledgeReportService reportService;
    private final KnowledgeArticleRepository articleRepository;
    private final ArticleVersionRepository versionRepository;
    private final KbCategoryRepository categoryRepository;
    private final KbStatusRepository statusRepository;
    private final KbApprovalStageRepository stageRepository;
    private final KbRiskLevelRepository riskLevelRepository;
    private final KbRelationTypeRepository relationTypeRepository;
    private final KbStepTypeRepository stepTypeRepository;

    // ---- Articles -----------------------------------------------------------

    /**
     * Read-only transactional: the full-text search is a native query, which
     * cannot carry an entity graph, so the session must stay open while the
     * response mapper dereferences category and status.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/articles")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Full-text search across the knowledge base")
    public ApiResponse<PageResponse<ArticleResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean onlyVisible,
            @PageableDefault(size = 20) Pageable pageable) {

        if (q == null || q.isBlank()) {
            return ApiResponse.ok(PageResponse.from(
                    articleRepository.findAll(pageable), this::toResponse));
        }
        return ApiResponse.ok(PageResponse.from(
                articleRepository.search(q, onlyVisible, pageable), this::toResponse));
    }

    @GetMapping("/articles/{code}")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Get an article")
    public ApiResponse<ArticleResponse> article(@PathVariable String code) {
        return ApiResponse.ok(toResponse(articleService.require(code)));
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('knowledge:create')")
    @Operation(summary = "Create an article")
    public ApiResponse<ArticleResponse> create(@Valid @RequestBody CreateArticleRequest request) {
        return ApiResponse.ok(toResponse(articleService.create(
                request.title(), request.summary(), request.categoryCode(),
                request.moduleCode(), request.processCode(), request.language(),
                request.keywords(), request.content(), request.contentFormat(),
                request.companyId(), request.branchId())));
    }

    @PutMapping("/articles/{code}")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Edit the working draft")
    public ApiResponse<VersionResponse> update(@PathVariable String code,
                                               @Valid @RequestBody UpdateArticleRequest request) {
        return ApiResponse.ok(VersionResponse.from(articleService.updateDraft(
                code, request.title(), request.summary(), request.keywords(),
                request.content(), request.changeNote())));
    }

    @PostMapping("/articles/{code}/versions")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Open a new draft version")
    public ApiResponse<VersionResponse> newVersion(@PathVariable String code,
                                                   @RequestBody NewVersionRequest request) {
        return ApiResponse.ok(VersionResponse.from(
                articleService.newVersion(code, request.major(), request.changeNote())));
    }

    @GetMapping("/articles/{code}/versions")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Version history")
    public ApiResponse<List<VersionResponse>> versions(@PathVariable String code) {
        return ApiResponse.ok(articleService.versions(code).stream()
                .map(VersionResponse::from).toList());
    }

    @GetMapping("/articles/{code}/content")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Read an article and record the view")
    public ApiResponse<VersionResponse> read(@PathVariable String code,
                                             @RequestParam(required = false) String contextModule,
                                             @RequestParam(required = false) String contextEntity,
                                             @RequestParam(required = false) Long contextRecordId) {
        return ApiResponse.ok(VersionResponse.from(articleService.recordView(
                code, contextModule, contextEntity, contextRecordId)));
    }

    @PostMapping("/articles/{code}/submit")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Submit the draft for approval")
    public ApiResponse<List<ApprovalResponse>> submit(@PathVariable String code) {
        return ApiResponse.ok(articleService.submitForApproval(code).stream()
                .map(ApprovalResponse::from).toList());
    }

    @PostMapping("/articles/{code}/publish")
    @PreAuthorize("@authz.has('knowledge:publish')")
    @Operation(summary = "Publish the approved draft")
    public ApiResponse<ArticleResponse> publish(@PathVariable String code) {
        return ApiResponse.ok(toResponse(articleService.publish(code)));
    }

    @PostMapping("/articles/{code}/deprecate")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Deprecate an article")
    public ApiResponse<ArticleResponse> deprecate(@PathVariable String code,
                                                  @RequestBody DeprecateRequest request) {
        return ApiResponse.ok(toResponse(articleService.deprecate(code, request.reason())));
    }

    @PostMapping("/articles/{code}/archive")
    @PreAuthorize("@authz.has('knowledge:delete')")
    @Operation(summary = "Archive an article")
    public ApiResponse<ArticleResponse> archive(@PathVariable String code) {
        return ApiResponse.ok(toResponse(articleService.archive(code)));
    }

    /** Context-sensitive knowledge for a business screen. */
    @GetMapping("/for-process")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Articles bound to a business process")
    public ApiResponse<List<ArticleResponse>> forProcess(@RequestParam String moduleCode,
                                                         @RequestParam(required = false) String processCode) {
        return ApiResponse.ok(articleService.forProcess(moduleCode, processCode).stream()
                .map(this::toResponse).toList());
    }

    @GetMapping("/for-record")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Articles linked to a specific business record")
    public ApiResponse<List<ArticleResponse>> forRecord(@RequestParam String module,
                                                        @RequestParam String entity,
                                                        @RequestParam Long recordId) {
        return ApiResponse.ok(relationService.forRecord(module, entity, recordId).stream()
                .map(this::toResponse).toList());
    }

    // ---- Approvals ----------------------------------------------------------

    @GetMapping("/articles/{code}/approvals")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Approval chain for the working draft")
    public ApiResponse<List<ApprovalResponse>> approvals(@PathVariable String code) {
        KnowledgeArticle article = articleService.require(code);
        return ApiResponse.ok(approvalService.forVersion(article.getCurrentVersionId()).stream()
                .map(ApprovalResponse::from).toList());
    }

    @PostMapping("/approvals/{id}/decide")
    @PreAuthorize("@authz.has('knowledge:approve')")
    @Operation(summary = "Approve or reject at a stage")
    public ApiResponse<ApprovalResponse> decide(@PathVariable Long id,
                                                @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.ok(ApprovalResponse.from(approvalService.decide(
                id, request.approve(), request.comment(), request.signatureRef())));
    }

    // ---- SOPs ---------------------------------------------------------------

    @PutMapping("/articles/{code}/sop")
    @PreAuthorize("@authz.has('knowledge:manage-sops')")
    @Operation(summary = "Create or update the procedure on the working draft")
    public ApiResponse<SopResponse> upsertSop(@PathVariable String code,
                                              @Valid @RequestBody SopRequest request) {
        Sop sop = sopService.upsert(code, request.purpose(), request.scope(),
                request.requiredInputs(), request.requiredOutputs(), request.estimatedMinutes(),
                request.riskLevelCode(), request.effectiveDate(), request.reviewDate());
        return ApiResponse.ok(toSopResponse(sop));
    }

    @GetMapping("/articles/{code}/sop")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Get the procedure a reader should see")
    public ApiResponse<SopResponse> sop(@PathVariable String code) {
        return ApiResponse.ok(toSopResponse(sopService.forArticle(code)));
    }

    @PutMapping("/sops/{id}/steps")
    @PreAuthorize("@authz.has('knowledge:manage-sops')")
    @Operation(summary = "Replace the procedure's steps")
    public ApiResponse<SopResponse> replaceSteps(@PathVariable Long id,
                                                 @Valid @RequestBody StepsRequest request) {
        sopService.replaceSteps(id, request.steps().stream()
                .map(s -> new SopService.StepInput(s.stepNumber(), s.stepTypeCode(), s.title(),
                        s.instruction(), s.expectedResult(), s.mandatory(), s.parallelGroup(),
                        s.conditionConfig(), s.branchConfig(), s.estimatedMinutes(),
                        s.warning(), s.checklist()))
                .toList());
        return ApiResponse.ok(toSopResponse(sopService.forArticleBySopId(id)));
    }

    @PutMapping("/sops/{id}/roles")
    @PreAuthorize("@authz.has('knowledge:manage-sops')")
    @Operation(summary = "Replace the procedure's responsible roles")
    public ApiResponse<List<RoleResponse>> replaceRoles(@PathVariable Long id,
                                                        @Valid @RequestBody RolesRequest request) {
        return ApiResponse.ok(sopService.replaceRoles(id, request.roles().stream()
                        .map(r -> new SopService.RoleInput(r.roleId(), r.roleLabel(),
                                r.responsibility(), r.accountable()))
                        .toList()).stream()
                .map(RoleResponse::from).toList());
    }

    // ---- Relations ----------------------------------------------------------

    @GetMapping("/articles/{code}/relations")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Relationships from an article")
    public ApiResponse<List<RelationResponse>> relations(@PathVariable String code) {
        return ApiResponse.ok(relationService.forArticle(code).stream()
                .map(RelationResponse::from).toList());
    }

    @PostMapping("/articles/{code}/relations/article")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Link to another article")
    public ApiResponse<RelationResponse> linkArticle(@PathVariable String code,
                                                     @Valid @RequestBody LinkArticleRequest request) {
        return ApiResponse.ok(RelationResponse.from(relationService.linkArticle(
                code, request.relationTypeCode(), request.targetArticleCode(), request.note())));
    }

    @PostMapping("/articles/{code}/relations/entity")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Link to a business record")
    public ApiResponse<RelationResponse> linkEntity(@PathVariable String code,
                                                    @Valid @RequestBody LinkEntityRequest request) {
        return ApiResponse.ok(RelationResponse.from(relationService.linkEntity(
                code, request.relationTypeCode(), request.module(), request.entity(),
                request.recordId(), request.note())));
    }

    @DeleteMapping("/relations/{id}")
    @PreAuthorize("@authz.has('knowledge:edit')")
    @Operation(summary = "Remove a relationship")
    public ApiResponse<Void> deleteRelation(@PathVariable Long id) {
        relationService.delete(id);
        return ApiResponse.ok();
    }

    // ---- Reports and catalogue ----------------------------------------------

    @GetMapping("/reports/{report}")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Run a knowledge report")
    public ApiResponse<List<Map<String, Object>>> report(@PathVariable String report) {
        return ApiResponse.ok(reportService.run(report));
    }

    @GetMapping(value = "/reports/{report}/export.csv", produces = "text/csv")
    @PreAuthorize("@authz.has('knowledge:export')")
    @Operation(summary = "Export a knowledge report as CSV")
    public ResponseEntity<String> exportReport(@PathVariable String report) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"knowledge-" + report + ".csv\"")
                .body(reportService.toCsv(reportService.run(report)));
    }

    @GetMapping("/catalog")
    @PreAuthorize("@authz.has('knowledge:view')")
    @Operation(summary = "Configuration catalogue")
    public ApiResponse<CatalogResponse> catalog() {
        return ApiResponse.ok(new CatalogResponse(
                categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(CategoryResponse::from).toList(),
                statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(StatusResponse::from).toList(),
                stageRepository.findAllByOrderByStageOrderAsc().stream()
                        .map(StageResponse::from).toList(),
                riskLevelRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(RiskLevelResponse::from).toList(),
                relationTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(RelationTypeResponse::from).toList(),
                stepTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                        .map(StepTypeResponse::from).toList(),
                reportService.available()));
    }

    // ---- Mapping ------------------------------------------------------------

    private ArticleResponse toResponse(KnowledgeArticle article) {
        String current = label(article.getCurrentVersionId());
        String published = label(article.getPublishedVersionId());
        return ArticleResponse.from(article, current, published);
    }

    private String label(Long versionId) {
        return versionId == null ? null
                : versionRepository.findById(versionId).map(ArticleVersion::getLabel).orElse(null);
    }

    private SopResponse toSopResponse(Sop sop) {
        List<StepResponse> steps = sopService.steps(sop.getId()).stream()
                .map(s -> StepResponse.from(s, sopService.checklist(s.getId()).stream()
                        .map(c -> c.getText()).toList()))
                .toList();
        List<RoleResponse> roles = sopService.roles(sop.getId()).stream()
                .map(RoleResponse::from).toList();
        return SopResponse.from(sop, steps, roles);
    }
}
