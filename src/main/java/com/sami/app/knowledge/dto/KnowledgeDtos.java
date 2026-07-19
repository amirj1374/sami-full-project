package com.sami.app.knowledge.dto;

import com.sami.app.knowledge.domain.ArticleVersion;
import com.sami.app.knowledge.domain.KbApproval;
import com.sami.app.knowledge.domain.KbApprovalStage;
import com.sami.app.knowledge.domain.KbCategory;
import com.sami.app.knowledge.domain.KbRelation;
import com.sami.app.knowledge.domain.KbRelationType;
import com.sami.app.knowledge.domain.KbRiskLevel;
import com.sami.app.knowledge.domain.KbStatus;
import com.sami.app.knowledge.domain.KbStepType;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.domain.Sop;
import com.sami.app.knowledge.domain.SopRole;
import com.sami.app.knowledge.domain.SopStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request/response records for the knowledge module. */
public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record ArticleResponse(Long id, String articleCode, String title, String summary,
                                  String categoryCode, String categoryName, boolean procedure,
                                  String statusCode, String statusName, boolean visible,
                                  String moduleCode, String processCode, String language,
                                  String keywords, Long ownerId, String ownerEmail,
                                  String currentVersion, String publishedVersion,
                                  long viewCount, Instant publishedAt, LocalDate effectiveDate,
                                  LocalDate reviewDate, boolean reviewOverdue,
                                  Instant createdAt, Instant updatedAt) {

        public static ArticleResponse from(KnowledgeArticle a, String current, String published) {
            return new ArticleResponse(a.getId(), a.getArticleCode(), a.getTitle(), a.getSummary(),
                    a.getCategory().getCode(), a.getCategory().getName(), a.getCategory().isProcedure(),
                    a.getStatus().getCode(), a.getStatus().getName(), a.getStatus().isVisible(),
                    a.getModuleCode(), a.getProcessCode(), a.getLanguage(), a.getKeywords(),
                    a.getOwnerId(), a.getOwnerEmail(), current, published,
                    a.getViewCount(), a.getPublishedAt(), a.getEffectiveDate(),
                    a.getReviewDate(), a.isReviewOverdue(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    public record VersionResponse(Long id, String label, int major, int minor, String content,
                                  String contentFormat, String changeNote, boolean current,
                                  boolean published, Instant publishedAt, String createdByEmail,
                                  Instant createdAt) {

        public static VersionResponse from(ArticleVersion v) {
            return new VersionResponse(v.getId(), v.getLabel(), v.getVersionMajor(),
                    v.getVersionMinor(), v.getContent(), v.getContentFormat(), v.getChangeNote(),
                    v.isCurrent(), v.isPublished(), v.getPublishedAt(),
                    v.getCreatedByEmail(), v.getCreatedAt());
        }
    }

    public record SopResponse(Long id, String sopNumber, String purpose, String scope,
                              List<String> requiredInputs, List<String> requiredOutputs,
                              Integer estimatedMinutes, String riskLevelCode,
                              LocalDate effectiveDate, LocalDate reviewDate,
                              List<StepResponse> steps, List<RoleResponse> roles) {

        public static SopResponse from(Sop s, List<StepResponse> steps, List<RoleResponse> roles) {
            return new SopResponse(s.getId(), s.getSopNumber(), s.getPurpose(), s.getScope(),
                    s.getRequiredInputs(), s.getRequiredOutputs(), s.getEstimatedMinutes(),
                    s.getRiskLevel() == null ? null : s.getRiskLevel().getCode(),
                    s.getEffectiveDate(), s.getReviewDate(), steps, roles);
        }
    }

    public record StepResponse(Long id, int stepNumber, String stepTypeCode, String title,
                               String instruction, String expectedResult, boolean mandatory,
                               String parallelGroup, Map<String, Object> conditionConfig,
                               Map<String, Object> branchConfig, Integer estimatedMinutes,
                               String warning, List<String> checklist) {

        public static StepResponse from(SopStep s, List<String> checklist) {
            return new StepResponse(s.getId(), s.getStepNumber(), s.getStepType().getCode(),
                    s.getTitle(), s.getInstruction(), s.getExpectedResult(), s.isMandatory(),
                    s.getParallelGroup(), s.getConditionConfig(), s.getBranchConfig(),
                    s.getEstimatedMinutes(), s.getWarning(), checklist);
        }
    }

    public record RoleResponse(Long id, Long roleId, String roleLabel, String responsibility,
                               boolean accountable) {

        public static RoleResponse from(SopRole r) {
            return new RoleResponse(r.getId(), r.getRoleId(), r.getRoleLabel(),
                    r.getResponsibility(), r.isAccountable());
        }
    }

    public record ApprovalResponse(Long id, String stageCode, String stageName, int stageOrder,
                                   String decision, boolean optional, boolean requiresSignature,
                                   String approverEmail, Instant decidedAt, String comment) {

        public static ApprovalResponse from(KbApproval a) {
            return new ApprovalResponse(a.getId(), a.getStage().getCode(), a.getStage().getName(),
                    a.getStage().getStageOrder(), a.getDecision(), a.getStage().isOptional(),
                    a.getStage().isRequiresSignature(), a.getApproverEmail(),
                    a.getDecidedAt(), a.getComment());
        }
    }

    public record RelationResponse(Long id, String relationTypeCode, String relationTypeName,
                                   Long targetArticleId, String targetModule, String targetEntity,
                                   Long targetRecordId, String note) {

        public static RelationResponse from(KbRelation r) {
            return new RelationResponse(r.getId(), r.getRelationType().getCode(),
                    r.getRelationType().getName(), r.getTargetArticleId(), r.getTargetModule(),
                    r.getTargetEntity(), r.getTargetRecordId(), r.getNote());
        }
    }

    // ---- Catalog ------------------------------------------------------------

    public record CategoryResponse(Long id, String code, String name, String description,
                                   String icon, boolean requiresApproval, boolean procedure,
                                   Integer reviewMonths) {
        public static CategoryResponse from(KbCategory c) {
            return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                    c.getIcon(), c.isRequiresApproval(), c.isProcedure(), c.getReviewMonths());
        }
    }

    public record StatusResponse(Long id, String code, String name, boolean allowsEdit,
                                 boolean visible, boolean published, boolean archived) {
        public static StatusResponse from(KbStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.isAllowsEdit(),
                    s.isVisible(), s.isPublishedState(), s.isArchivedState());
        }
    }

    public record StageResponse(Long id, String code, String name, int order,
                                String requiredPermission, boolean isFinal, boolean optional,
                                boolean requiresSignature) {
        public static StageResponse from(KbApprovalStage s) {
            return new StageResponse(s.getId(), s.getCode(), s.getName(), s.getStageOrder(),
                    s.getRequiredPermission(), s.isFinal(), s.isOptional(), s.isRequiresSignature());
        }
    }

    public record RiskLevelResponse(Long id, String code, String name, boolean requiresApproval) {
        public static RiskLevelResponse from(KbRiskLevel r) {
            return new RiskLevelResponse(r.getId(), r.getCode(), r.getName(), r.isRequiresApproval());
        }
    }

    public record RelationTypeResponse(Long id, String code, String name, String targetKind,
                                       boolean symmetric) {
        public static RelationTypeResponse from(KbRelationType t) {
            return new RelationTypeResponse(t.getId(), t.getCode(), t.getName(),
                    t.getTargetKind(), t.isSymmetric());
        }
    }

    public record StepTypeResponse(Long id, String code, String name, boolean decision,
                                   boolean parallel, boolean conditional) {
        public static StepTypeResponse from(KbStepType t) {
            return new StepTypeResponse(t.getId(), t.getCode(), t.getName(), t.isDecision(),
                    t.isParallel(), t.isConditional());
        }
    }

    public record CatalogResponse(List<CategoryResponse> categories, List<StatusResponse> statuses,
                                  List<StageResponse> approvalStages, List<RiskLevelResponse> riskLevels,
                                  List<RelationTypeResponse> relationTypes,
                                  List<StepTypeResponse> stepTypes, List<String> reports) {
    }

    // ---- Requests -----------------------------------------------------------

    public record CreateArticleRequest(@NotBlank @Size(max = 255) String title,
                                       @Size(max = 2000) String summary,
                                       @NotBlank String categoryCode,
                                       String moduleCode, String processCode,
                                       String language, String keywords,
                                       String content, String contentFormat,
                                       Long companyId, Long branchId) {
    }

    public record UpdateArticleRequest(String title, String summary, String keywords,
                                       String content, String changeNote) {
    }

    public record NewVersionRequest(boolean major, String changeNote) {
    }

    public record DecisionRequest(boolean approve, String comment, String signatureRef) {
    }

    public record SopRequest(String purpose, String scope, List<String> requiredInputs,
                             List<String> requiredOutputs, Integer estimatedMinutes,
                             String riskLevelCode, LocalDate effectiveDate, LocalDate reviewDate) {
    }

    public record StepsRequest(List<StepRequest> steps) {
    }

    public record StepRequest(int stepNumber, String stepTypeCode,
                              @NotBlank String title, String instruction,
                              String expectedResult, boolean mandatory, String parallelGroup,
                              Map<String, Object> conditionConfig, Map<String, Object> branchConfig,
                              Integer estimatedMinutes, String warning, List<String> checklist) {
    }

    public record RolesRequest(List<RoleRequest> roles) {
    }

    public record RoleRequest(Long roleId, String roleLabel, String responsibility,
                              boolean accountable) {
    }

    public record LinkArticleRequest(@NotBlank String relationTypeCode,
                                     @NotBlank String targetArticleCode, String note) {
    }

    public record LinkEntityRequest(@NotBlank String relationTypeCode, @NotBlank String module,
                                    @NotBlank String entity, Long recordId, String note) {
    }

    public record DeprecateRequest(String reason) {
    }
}
