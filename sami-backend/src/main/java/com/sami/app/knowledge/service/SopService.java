package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.knowledge.domain.ArticleVersion;
import com.sami.app.knowledge.domain.KbStepType;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.domain.Sop;
import com.sami.app.knowledge.domain.SopRole;
import com.sami.app.knowledge.domain.SopStep;
import com.sami.app.knowledge.domain.StepChecklistItem;
import com.sami.app.knowledge.repository.KbRiskLevelRepository;
import com.sami.app.knowledge.repository.KbStepTypeRepository;
import com.sami.app.knowledge.repository.KnowledgeArticleRepository;
import com.sami.app.knowledge.repository.SopRepository;
import com.sami.app.knowledge.repository.SopRoleRepository;
import com.sami.app.knowledge.repository.SopStepRepository;
import com.sami.app.knowledge.repository.StepChecklistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Standard operating procedures.
 *
 * <p>An SOP is attached to an article VERSION, so its steps are versioned with
 * the content and a published procedure cannot change underneath the people
 * following it.
 */
@Service
@RequiredArgsConstructor
public class SopService {

    private final SopRepository sopRepository;
    private final SopStepRepository stepRepository;
    private final SopRoleRepository roleRepository;
    private final StepChecklistItemRepository checklistRepository;
    private final KbStepTypeRepository stepTypeRepository;
    private final KbRiskLevelRepository riskLevelRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final ArticleService articleService;
    private final SopValidator validator;
    private final KbAuditService auditService;

    @Transactional
    public Sop upsert(String articleCode, String purpose, String scope,
                      List<String> inputs, List<String> outputs, Integer estimatedMinutes,
                      String riskLevelCode, LocalDate effectiveDate, LocalDate reviewDate) {
        KnowledgeArticle article = articleService.require(articleCode);
        if (!article.getCategory().isProcedure()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Category '%s' does not hold procedures".formatted(article.getCategory().getCode()));
        }
        ArticleVersion draft = articleService.currentVersion(article);
        if (!article.getStatus().isAllowsEdit()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A procedure in status '%s' cannot be edited".formatted(article.getStatus().getCode()));
        }

        Sop sop = sopRepository.findByArticleVersionId(draft.getId())
                .orElseGet(() -> Sop.builder()
                        .articleVersionId(draft.getId())
                        .sopNumber("SOP-%05d".formatted(articleRepository.nextSopSequence()))
                        .tenantId(article.getTenantId())
                        .build());

        sop.setPurpose(purpose);
        sop.setScope(scope);
        sop.setRequiredInputs(inputs == null ? List.of() : inputs);
        sop.setRequiredOutputs(outputs == null ? List.of() : outputs);
        sop.setEstimatedMinutes(estimatedMinutes);
        sop.setEffectiveDate(effectiveDate);
        sop.setReviewDate(reviewDate);
        if (riskLevelCode != null) {
            sop.setRiskLevel(riskLevelRepository.findByCode(riskLevelCode)
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Unknown risk level: " + riskLevelCode)));
        } else if (sop.getRiskLevel() == null) {
            riskLevelRepository.findFirstByIsDefaultTrue().ifPresent(sop::setRiskLevel);
        }

        Sop saved = sopRepository.save(sop);
        auditService.record("sop", saved.getId(), KbAuditService.UPDATED, null,
                Map.of("sopNumber", saved.getSopNumber(), "version", draft.getLabel()));
        return saved;
    }

    /**
     * Replaces the whole step list. Wholesale replacement rather than per-step
     * edits keeps step numbering and branch targets consistent — a partial edit
     * is how a branch ends up pointing at a step that no longer exists.
     */
    @Transactional
    public List<SopStep> replaceSteps(Long sopId, List<StepInput> steps) {
        Sop sop = sopRepository.findById(sopId)
                .orElseThrow(() -> ResourceNotFoundException.of("SOP", sopId));

        stepRepository.findAllBySopIdOrderByStepNumberAsc(sopId)
                .forEach(existing -> checklistRepository.deleteAllByStepId(existing.getId()));
        stepRepository.deleteAllBySopId(sopId);

        List<SopStep> saved = steps.stream().map(input -> {
            KbStepType type = stepTypeRepository.findByCode(
                            input.stepTypeCode() == null ? "sequential" : input.stepTypeCode())
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Unknown step type: " + input.stepTypeCode()));

            SopStep step = stepRepository.save(SopStep.builder()
                    .sopId(sopId)
                    .stepNumber(input.stepNumber())
                    .stepType(type)
                    .title(input.title())
                    .instruction(input.instruction())
                    .expectedResult(input.expectedResult())
                    .isMandatory(input.mandatory())
                    .parallelGroup(input.parallelGroup())
                    .conditionConfig(input.conditionConfig() == null ? Map.of() : input.conditionConfig())
                    .branchConfig(input.branchConfig() == null ? Map.of() : input.branchConfig())
                    .estimatedMinutes(input.estimatedMinutes())
                    .warning(input.warning())
                    .displayOrder(input.stepNumber())
                    .tenantId(sop.getTenantId())
                    .build());

            if (input.checklist() != null) {
                int order = 0;
                for (String text : input.checklist()) {
                    checklistRepository.save(StepChecklistItem.builder()
                            .stepId(step.getId()).text(text).isMandatory(true)
                            .displayOrder(order++).build());
                }
            }
            return step;
        }).toList();

        // Validate the finished shape, so branch targets are checked against the
        // complete step set rather than whatever existed mid-write.
        validator.assertPublishable(sop, stepRepository.findAllBySopIdOrderByStepNumberAsc(sopId));

        auditService.record("sop", sopId, KbAuditService.UPDATED, null, Map.of("steps", saved.size()));
        return saved;
    }

    @Transactional
    public List<SopRole> replaceRoles(Long sopId, List<RoleInput> roles) {
        sopRepository.findById(sopId).orElseThrow(() -> ResourceNotFoundException.of("SOP", sopId));
        roleRepository.deleteAllBySopId(sopId);
        int order = 0;
        for (RoleInput input : roles) {
            roleRepository.save(SopRole.builder()
                    .sopId(sopId).roleId(input.roleId()).roleLabel(input.roleLabel())
                    .responsibility(input.responsibility()).isAccountable(input.accountable())
                    .displayOrder(order++).build());
        }
        return roleRepository.findAllBySopIdOrderByDisplayOrderAsc(sopId);
    }

    @Transactional(readOnly = true)
    public Sop forArticle(String articleCode) {
        KnowledgeArticle article = articleService.require(articleCode);
        ArticleVersion version = articleService.readableVersion(article);
        return sopRepository.findByArticleVersionId(version.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("SOP", articleCode));
    }

    /** Re-reads a procedure by its own id, for endpoints addressed by SOP rather than article. */
    @Transactional(readOnly = true)
    public Sop forArticleBySopId(Long sopId) {
        return sopRepository.findById(sopId)
                .orElseThrow(() -> ResourceNotFoundException.of("SOP", sopId));
    }

    @Transactional(readOnly = true)
    public List<SopStep> steps(Long sopId) {
        return stepRepository.findAllBySopIdOrderByStepNumberAsc(sopId);
    }

    @Transactional(readOnly = true)
    public List<SopRole> roles(Long sopId) {
        return roleRepository.findAllBySopIdOrderByDisplayOrderAsc(sopId);
    }

    @Transactional(readOnly = true)
    public List<StepChecklistItem> checklist(Long stepId) {
        return checklistRepository.findAllByStepIdOrderByDisplayOrderAsc(stepId);
    }

    public record StepInput(int stepNumber, String stepTypeCode, String title, String instruction,
                            String expectedResult, boolean mandatory, String parallelGroup,
                            Map<String, Object> conditionConfig, Map<String, Object> branchConfig,
                            Integer estimatedMinutes, String warning, List<String> checklist) {
    }

    public record RoleInput(Long roleId, String roleLabel, String responsibility, boolean accountable) {
    }
}
