package com.sami.app.dataquality.service;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dataquality.domain.QualityCorrection;
import com.sami.app.dataquality.domain.QualityIssue;
import com.sami.app.dataquality.event.DataQualityDomainEvent;
import com.sami.app.dataquality.repository.QualityCorrectionRepository;
import com.sami.app.dataquality.repository.QualityIssueRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Issue lifecycle and correction tracking. Corrections are *recorded* here — the
 * owning business module remains responsible for writing its own data, so this
 * module never mutates business tables.
 */
@Service
@RequiredArgsConstructor
public class QualityIssueService {

    private final QualityIssueRepository issueRepository;
    private final QualityCorrectionRepository correctionRepository;
    private final QualityAuditService audit;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<QualityIssue> list(String status, Pageable pageable) {
        return issueRepository.findByStatusOrderByCreatedAtDesc(
                status == null ? QualityIssue.Status.OPEN.name() : status, pageable);
    }

    @Transactional(readOnly = true)
    public List<QualityIssue> forEntity(String moduleCode, String entityCode, Long entityId) {
        return issueRepository.findByModuleCodeAndEntityCodeAndEntityId(moduleCode, entityCode, entityId);
    }

    @Transactional
    public QualityIssue resolve(Long id, String note) {
        QualityIssue issue = load(id);
        issue.setStatus(QualityIssue.Status.RESOLVED.name());
        issue.setResolutionNote(note);
        issue.setResolvedAt(Instant.now());
        issue.setResolvedBy(CurrentActor.id());
        issue.setResolvedByEmail(CurrentActor.email());
        QualityIssue saved = issueRepository.save(issue);

        audit.record("ISSUE", id, "RESOLVED", Map.of("status", "OPEN"), Map.of("status", "RESOLVED"));
        eventPublisher.publishEvent(new DataQualityDomainEvent(
                "dq-issue-" + id, DataQualityDomainEvent.ISSUE_RESOLVED,
                saved.getModuleCode(), saved.getEntityCode(), saved.getEntityId(),
                Map.of("note", String.valueOf(note)), Instant.now()));
        return saved;
    }

    @Transactional
    public QualityIssue ignore(Long id, String note) {
        QualityIssue issue = load(id);
        issue.setStatus(QualityIssue.Status.IGNORED.name());
        issue.setResolutionNote(note);
        issue.setResolvedAt(Instant.now());
        issue.setResolvedBy(CurrentActor.id());
        issue.setResolvedByEmail(CurrentActor.email());
        QualityIssue saved = issueRepository.save(issue);
        audit.record("ISSUE", id, "IGNORED", Map.of("status", "OPEN"), Map.of("status", "IGNORED"));
        return saved;
    }

    /** Records a correction (manual or automatic) against an issue. */
    @Transactional
    public QualityCorrection correct(Long issueId, String field, String oldValue, String newValue,
                                     boolean automatic, String note) {
        QualityIssue issue = load(issueId);
        QualityCorrection correction = correctionRepository.save(QualityCorrection.builder()
                .issueId(issueId)
                .fieldName(field == null ? issue.getFieldName() : field)
                .oldValue(oldValue)
                .newValue(newValue)
                .automatic(automatic)
                .note(note)
                .appliedBy(CurrentActor.id())
                .appliedByEmail(CurrentActor.email())
                .build());

        audit.record("CORRECTION", correction.getId(), automatic ? "AUTO_CORRECTED" : "CORRECTED",
                Map.of("value", String.valueOf(oldValue)), Map.of("value", String.valueOf(newValue)));
        if (automatic) {
            eventPublisher.publishEvent(new DataQualityDomainEvent(
                    "dq-correction-" + correction.getId(),
                    DataQualityDomainEvent.AUTOMATIC_CORRECTION_EXECUTED,
                    issue.getModuleCode(), issue.getEntityCode(), issue.getEntityId(),
                    Map.of("field", String.valueOf(correction.getFieldName())), Instant.now()));
        }
        return correction;
    }

    @Transactional(readOnly = true)
    public List<QualityCorrection> corrections(Long issueId) {
        return correctionRepository.findByIssueIdOrderByAppliedAtDesc(issueId);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        return Map.of(
                "open", issueRepository.countByStatus(QualityIssue.Status.OPEN.name()),
                "resolved", issueRepository.countByStatus(QualityIssue.Status.RESOLVED.name()),
                "ignored", issueRepository.countByStatus(QualityIssue.Status.IGNORED.name()));
    }

    private QualityIssue load(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quality issue not found: " + id));
    }
}
