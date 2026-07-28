package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.knowledge.domain.ArticleVersion;
import com.sami.app.knowledge.domain.ArticleView;
import com.sami.app.knowledge.domain.KbApproval;
import com.sami.app.knowledge.domain.KbApprovalStage;
import com.sami.app.knowledge.domain.KbCategory;
import com.sami.app.knowledge.domain.KbStatus;
import com.sami.app.knowledge.domain.KnowledgeArticle;
import com.sami.app.knowledge.domain.Sop;
import com.sami.app.knowledge.event.KnowledgeDomainEvent;
import com.sami.app.knowledge.repository.ArticleVersionRepository;
import com.sami.app.knowledge.repository.ArticleViewRepository;
import com.sami.app.knowledge.repository.KbApprovalRepository;
import com.sami.app.knowledge.repository.KbApprovalStageRepository;
import com.sami.app.knowledge.repository.KbCategoryRepository;
import com.sami.app.knowledge.repository.KbStatusRepository;
import com.sami.app.knowledge.repository.KnowledgeArticleRepository;
import com.sami.app.knowledge.repository.SopRepository;
import com.sami.app.knowledge.repository.SopStepRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The module's public service for knowledge assets.
 *
 * <p>Business modules call {@link #forProcess} to display context-sensitive
 * guidance and {@link #recordView} to report usage. They never store operational
 * knowledge themselves.
 *
 * <p>Content lives on versions, never on the article. Publication swaps a
 * pointer, so a procedure someone followed last year is still reproducible
 * exactly as it stood.
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final KnowledgeArticleRepository articleRepository;
    private final ArticleVersionRepository versionRepository;
    private final ArticleViewRepository viewRepository;
    private final KbCategoryRepository categoryRepository;
    private final KbStatusRepository statusRepository;
    private final KbApprovalRepository approvalRepository;
    private final KbApprovalStageRepository stageRepository;
    private final SopRepository sopRepository;
    private final SopStepRepository stepRepository;
    private final ApprovalChain approvalChain;
    private final SopValidator sopValidator;
    private final KbAuditService auditService;
    private final TenantDefaults tenantDefaults;
    private final ApplicationEventPublisher events;

    // ---- Creation and editing ----------------------------------------------

    @Transactional
    public KnowledgeArticle create(String title, String summary, String categoryCode,
                                   String moduleCode, String processCode, String language,
                                   String keywords, String content, String contentFormat,
                                   Long companyId, Long branchId) {
        KbCategory category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown knowledge category: " + categoryCode));
        KbStatus draft = statusRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default knowledge status is configured"));

        KnowledgeArticle article = articleRepository.save(KnowledgeArticle.builder()
                .articleCode("KB-%06d".formatted(articleRepository.nextArticleSequence()))
                .title(title)
                .summary(summary)
                .category(category)
                .status(draft)
                .moduleCode(moduleCode)
                .processCode(processCode)
                .language(language == null || language.isBlank() ? "en" : language)
                .keywords(keywords)
                .companyId(companyId)
                .branchId(branchId)
                .ownerId(CurrentActor.id())
                .ownerEmail(CurrentActor.email())
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .reviewDate(reviewDateFor(category))
                // Explicit: Hibernate includes tenant_id in the INSERT, so the
                // column DEFAULT from V17 never fires for a mapped entity.
                .tenantId(tenantDefaults.current())
                .build());

        ArticleVersion version = versionRepository.save(ArticleVersion.builder()
                .articleId(article.getId())
                .versionMajor(1).versionMinor(0)
                .label(ArticleVersion.label(1, 0))
                .content(content)
                .contentFormat(contentFormat == null ? "markdown" : contentFormat)
                .changeNote("Initial draft")
                .isCurrent(true)
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .tenantId(article.getTenantId())
                .build());

        article.setCurrentVersionId(version.getId());
        articleRepository.save(article);

        auditService.record("article", article.getId(), KbAuditService.CREATED, null,
                Map.of("code", article.getArticleCode(), "title", title, "category", categoryCode));
        publish(KnowledgeDomainEvent.KNOWLEDGE_CREATED, article, Map.of("version", version.getLabel()));
        return article;
    }

    /**
     * Edits the working draft. Refuses once the article has entered review or
     * been published — at that point a new version is the correct move, so the
     * content someone is reviewing cannot shift underneath them.
     */
    @Transactional
    public ArticleVersion updateDraft(String articleCode, String title, String summary,
                                      String keywords, String content, String changeNote) {
        KnowledgeArticle article = require(articleCode);
        if (!article.getStatus().isAllowsEdit()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "An article in status '%s' cannot be edited; create a new version instead"
                            .formatted(article.getStatus().getCode()));
        }
        ArticleVersion draft = currentVersion(article);

        if (title != null && !title.isBlank()) {
            article.setTitle(title);
        }
        if (summary != null) {
            article.setSummary(summary);
        }
        if (keywords != null) {
            article.setKeywords(keywords);
        }
        articleRepository.save(article);

        if (content != null) {
            draft.setContent(content);
        }
        if (changeNote != null) {
            draft.setChangeNote(changeNote);
        }
        versionRepository.save(draft);

        auditService.record("article", article.getId(), KbAuditService.UPDATED, null,
                Map.of("version", draft.getLabel()));
        publish(KnowledgeDomainEvent.KNOWLEDGE_UPDATED, article, Map.of("version", draft.getLabel()));
        return draft;
    }

    /**
     * Opens a new draft on a published article. The published version stays
     * live and readable while the new one is written and approved.
     */
    @Transactional
    public ArticleVersion newVersion(String articleCode, boolean major, String changeNote) {
        KnowledgeArticle article = require(articleCode);
        ArticleVersion latest = versionRepository
                .findAllByArticleIdOrderByVersionMajorDescVersionMinorDesc(article.getId())
                .stream().findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "Article has no version"));

        versionRepository.findFirstByArticleIdAndIsCurrentTrue(article.getId()).ifPresent(current -> {
            // uq_kb_versions_current is a partial unique index; flush the clear
            // before the replacement is inserted.
            current.setCurrent(false);
            versionRepository.saveAndFlush(current);
        });

        int nextMajor = major ? latest.getVersionMajor() + 1 : latest.getVersionMajor();
        int nextMinor = major ? 0 : latest.getVersionMinor() + 1;

        ArticleVersion version = versionRepository.save(ArticleVersion.builder()
                .articleId(article.getId())
                .versionMajor(nextMajor).versionMinor(nextMinor)
                .label(ArticleVersion.label(nextMajor, nextMinor))
                // Seeded from the previous content so an author edits rather than retypes.
                .content(latest.getContent())
                .contentFormat(latest.getContentFormat())
                .changeNote(changeNote)
                .isCurrent(true)
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .tenantId(article.getTenantId())
                .build());

        article.setCurrentVersionId(version.getId());
        // Only an article that has never been published drops back to draft.
        // Resetting the status of a PUBLISHED article would hide its live version
        // from readers the moment an author started editing — the published
        // version must stay visible until the new one is published in its place.
        if (article.getPublishedVersionId() == null) {
            statusRepository.findFirstByIsDefaultTrue().ifPresent(article::setStatus);
        }
        articleRepository.save(article);

        auditService.record("article", article.getId(), KbAuditService.UPDATED,
                Map.of("version", latest.getLabel()), Map.of("version", version.getLabel()));
        return version;
    }

    // ---- Approval and publication -------------------------------------------

    /**
     * Sends the current draft into the approval chain, creating one pending
     * decision per configured stage.
     */
    @Transactional
    public List<KbApproval> submitForApproval(String articleCode) {
        KnowledgeArticle article = require(articleCode);
        ArticleVersion draft = currentVersion(article);

        if (!approvalRepository.findAllByArticleVersionIdOrderByStageStageOrderAsc(draft.getId()).isEmpty()) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Version %s is already in the approval chain".formatted(draft.getLabel()));
        }
        // A procedure is validated before review, not at publication: reviewers
        // should never be asked to approve something structurally incomplete.
        assertProcedureComplete(draft);

        List<KbApprovalStage> stages = stageRepository.findAllByOrderByStageOrderAsc();
        if (stages.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "No approval stages are configured");
        }
        List<KbApproval> approvals = stages.stream()
                .map(stage -> approvalRepository.save(KbApproval.builder()
                        .articleVersionId(draft.getId())
                        .stage(stage)
                        .decision("pending")
                        .tenantId(article.getTenantId())
                        .build()))
                .toList();

        statusRepository.findFirstByIsReviewStateTrue().ifPresent(article::setStatus);
        articleRepository.save(article);

        auditService.record("article", article.getId(), KbAuditService.UPDATED, null,
                Map.of("submittedVersion", draft.getLabel(), "stages", stages.size()));
        publish(KnowledgeDomainEvent.APPROVAL_REQUESTED, article,
                Map.of("version", draft.getLabel()));
        return approvals;
    }

    /**
     * Publishes the approved draft: it becomes the version readers see, and the
     * previously published version is retired but retained.
     */
    @Transactional
    public KnowledgeArticle publish(String articleCode) {
        KnowledgeArticle article = require(articleCode);
        ArticleVersion draft = currentVersion(article);

        List<KbApproval> approvals =
                approvalRepository.findAllByArticleVersionIdOrderByStageStageOrderAsc(draft.getId());
        if (article.getCategory().isRequiresApproval()) {
            if (approvals.isEmpty()) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Category '%s' requires approval before publication"
                                .formatted(article.getCategory().getCode()));
            }
            if (approvalChain.isRejected(approvals)) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "This version was rejected and cannot be published");
            }
            if (!approvalChain.isFullyApproved(approvals)) {
                KbApproval pending = approvalChain.nextPending(approvals).orElseThrow();
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Approval is still pending at stage '%s'".formatted(pending.getStage().getName()));
            }
        }
        assertProcedureComplete(draft);

        // Retire the previously published version. Retained, never deleted — the
        // whole point of versioning is that history stays reproducible.
        versionRepository.findFirstByArticleIdAndIsPublishedTrue(article.getId())
                .ifPresent(previous -> {
                    // uq_kb_versions_published is a partial unique index; flush before
                    // the new version claims the published slot.
                    previous.setPublished(false);
                    previous.setArchivedAt(Instant.now());
                    versionRepository.saveAndFlush(previous);
                });

        draft.setPublished(true);
        draft.setPublishedAt(Instant.now());
        draft.setPublishedBy(CurrentActor.id());
        versionRepository.save(draft);

        KbStatus published = statusRepository.findFirstByIsPublishedStateTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No knowledge status is flagged as the published state"));
        article.setStatus(published);
        article.setPublishedVersionId(draft.getId());
        article.setPublishedAt(Instant.now());
        if (article.getEffectiveDate() == null) {
            article.setEffectiveDate(LocalDate.now());
        }
        article.setReviewDate(reviewDateFor(article.getCategory()));
        articleRepository.save(article);

        auditService.record("article", article.getId(), KbAuditService.PUBLISHED, null,
                Map.of("version", draft.getLabel()));
        publish(KnowledgeDomainEvent.KNOWLEDGE_PUBLISHED, article,
                Map.of("version", draft.getLabel()));
        if (article.getCategory().isProcedure()) {
            publish(KnowledgeDomainEvent.SOP_APPROVED, article, Map.of("version", draft.getLabel()));
        }
        return article;
    }

    @Transactional
    public KnowledgeArticle deprecate(String articleCode, String reason) {
        KnowledgeArticle article = require(articleCode);
        statusRepository.findFirstByIsDeprecatedStateTrue().ifPresent(article::setStatus);
        articleRepository.save(article);
        auditService.record("article", article.getId(), KbAuditService.DEPRECATED, null,
                Map.of("reason", reason == null ? "" : reason));
        publish(KnowledgeDomainEvent.KNOWLEDGE_DEPRECATED, article, Map.of());
        return article;
    }

    @Transactional
    public KnowledgeArticle archive(String articleCode) {
        KnowledgeArticle article = require(articleCode);
        statusRepository.findFirstByIsArchivedStateTrue().ifPresent(article::setStatus);
        article.setArchivedAt(Instant.now());
        articleRepository.save(article);
        auditService.record("article", article.getId(), KbAuditService.ARCHIVED, null, Map.of());
        publish(KnowledgeDomainEvent.KNOWLEDGE_ARCHIVED, article, Map.of());
        return article;
    }

    // ---- Reading ------------------------------------------------------------

    /**
     * Context-sensitive lookup: the visible articles bound to a business process.
     * This is how a repair screen shows the repair intake procedure without the
     * repair module knowing anything about knowledge management.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeArticle> forProcess(String moduleCode, String processCode) {
        return processCode == null || processCode.isBlank()
                ? articleRepository.findAllByModuleCodeAndStatusIsVisibleTrueOrderByTitleAsc(moduleCode)
                : articleRepository.findAllByModuleCodeAndProcessCodeAndStatusIsVisibleTrueOrderByTitleAsc(
                        moduleCode, processCode);
    }

    /** Records a read and returns the version the reader should see. */
    @Transactional
    public ArticleVersion recordView(String articleCode, String contextModule,
                                     String contextEntity, Long contextRecordId) {
        KnowledgeArticle article = require(articleCode);
        ArticleVersion version = readableVersion(article);

        viewRepository.save(ArticleView.builder()
                .articleId(article.getId())
                .versionId(version.getId())
                .viewerId(CurrentActor.id())
                .viewerEmail(CurrentActor.email())
                .contextModule(contextModule)
                .contextEntity(contextEntity)
                .contextRecordId(contextRecordId)
                .tenantId(article.getTenantId())
                .build());

        article.setViewCount(article.getViewCount() + 1);
        article.setLastViewedAt(Instant.now());
        articleRepository.save(article);

        publish(KnowledgeDomainEvent.KNOWLEDGE_VIEWED, article,
                Map.of("version", version.getLabel(),
                        "context", contextModule == null ? "direct" : contextModule));
        return version;
    }

    /**
     * The version a reader should see: the published one where it exists,
     * otherwise the draft — so an author can preview before publication.
     */
    @Transactional(readOnly = true)
    public ArticleVersion readableVersion(KnowledgeArticle article) {
        return versionRepository.findFirstByArticleIdAndIsPublishedTrue(article.getId())
                .orElseGet(() -> currentVersion(article));
    }

    @Transactional(readOnly = true)
    public KnowledgeArticle require(String articleCode) {
        return articleRepository.findByArticleCode(articleCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", articleCode));
    }

    @Transactional(readOnly = true)
    public List<ArticleVersion> versions(String articleCode) {
        return versionRepository.findAllByArticleIdOrderByVersionMajorDescVersionMinorDesc(
                require(articleCode).getId());
    }

    @Transactional(readOnly = true)
    public ArticleVersion currentVersion(KnowledgeArticle article) {
        return versionRepository.findFirstByArticleIdAndIsCurrentTrue(article.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "Article %s has no working version".formatted(article.getArticleCode())));
    }

    // ---- Internals ----------------------------------------------------------

    /** A procedure article must have a structurally complete SOP behind it. */
    private void assertProcedureComplete(ArticleVersion version) {
        Sop sop = sopRepository.findByArticleVersionId(version.getId()).orElse(null);
        if (sop == null) {
            return;
        }
        sopValidator.assertPublishable(sop, stepRepository.findAllBySopIdOrderByStepNumberAsc(sop.getId()));
    }

    private LocalDate reviewDateFor(KbCategory category) {
        return category.getReviewMonths() == null
                ? null
                : LocalDate.now().plusMonths(category.getReviewMonths());
    }

    private void publish(String eventType, KnowledgeArticle article, Map<String, Object> payload) {
        events.publishEvent(KnowledgeDomainEvent.of(eventType, article.getId(),
                article.getArticleCode(), article.getModuleCode(), article.getProcessCode(), payload));
    }
}
