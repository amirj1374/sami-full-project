package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.knowledge.domain.ArticleVersion;
import com.sami.app.knowledge.domain.KbApproval;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.event.KnowledgeDomainEvent;
import com.sami.app.knowledge.repository.KbApprovalRepository;
import com.sami.app.knowledge.repository.KbStatusRepository;
import com.sami.app.knowledge.repository.KnowledgeArticleRepository;
import com.sami.app.security.Authz;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Decisions on the configurable approval chain. */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final KbApprovalRepository approvalRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final KbStatusRepository statusRepository;
    private final ApprovalChain approvalChain;
    private final KbAuditService auditService;
    private final Authz authz;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<KbApproval> forVersion(Long versionId) {
        return approvalRepository.findAllByArticleVersionIdOrderByStageStageOrderAsc(versionId);
    }

    @Transactional
    public KbApproval decide(Long approvalId, boolean approve, String comment, String signatureRef) {
        KbApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> ResourceNotFoundException.of("Approval", approvalId));

        List<KbApproval> chain = forVersion(approval.getArticleVersionId());

        // Sequence first: an out-of-order decision must be refused before any
        // permission or signature check, so the error explains the real problem.
        approvalChain.assertCanDecide(chain, approval);

        // The stage names the permission its approver must hold. Checked here
        // rather than only on the endpoint, so the rule holds for every caller.
        String required = approval.getStage().getRequiredPermission();
        if (required != null && !required.isBlank() && !authz.has(required)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Stage '%s' requires the '%s' permission"
                            .formatted(approval.getStage().getName(), required));
        }
        if (approve) {
            approvalChain.assertSignatureSupplied(approval.getStage(), signatureRef);
        }

        approval.setDecision(approve ? "approved" : "rejected");
        approval.setApproverId(CurrentActor.id());
        approval.setApproverEmail(CurrentActor.email());
        approval.setDecidedAt(Instant.now());
        approval.setComment(comment);
        approval.setSignatureRef(signatureRef);
        approvalRepository.save(approval);

        applyChainOutcome(approval, approve);

        auditService.record("approval", approval.getId(),
                approve ? KbAuditService.APPROVED : KbAuditService.REJECTED, null,
                Map.of("stage", approval.getStage().getCode(),
                        "comment", comment == null ? "" : comment));
        return approval;
    }

    /**
     * A rejection sends the article back to draft immediately; the remaining
     * stages are not asked to decide on content that is already going to change.
     */
    private void applyChainOutcome(KbApproval approval, boolean approved) {
        articleRepository.findAll().stream()
                .filter(a -> approval.getArticleVersionId().equals(a.getCurrentVersionId()))
                .findFirst()
                .ifPresent(article -> {
                    List<KbApproval> chain = forVersion(approval.getArticleVersionId());
                    if (!approved) {
                        statusRepository.findFirstByIsDefaultTrue().ifPresent(article::setStatus);
                        articleRepository.save(article);
                        publish(KnowledgeDomainEvent.APPROVAL_REJECTED, article,
                                Map.of("stage", approval.getStage().getCode()));
                    } else if (approvalChain.isFullyApproved(chain)) {
                        statusRepository.findFirstByIsApprovedStateTrue().ifPresent(article::setStatus);
                        articleRepository.save(article);
                    }
                });
    }

    private void publish(String eventType, KnowledgeArticle article, Map<String, Object> payload) {
        events.publishEvent(KnowledgeDomainEvent.of(eventType, article.getId(),
                article.getArticleCode(), article.getModuleCode(), article.getProcessCode(), payload));
    }
}
