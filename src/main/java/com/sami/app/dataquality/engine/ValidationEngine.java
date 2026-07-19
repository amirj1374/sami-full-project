package com.sami.app.dataquality.engine;

import com.sami.app.dataquality.domain.QualityIssue;
import com.sami.app.dataquality.domain.QualityRule;
import com.sami.app.dataquality.domain.ValidationRun;
import com.sami.app.dataquality.event.DataQualityDomainEvent;
import com.sami.app.dataquality.repository.QualityIssueRepository;
import com.sami.app.dataquality.repository.QualityRuleRepository;
import com.sami.app.dataquality.repository.ValidationRunRepository;
import com.sami.app.dataquality.service.QualityScoreService;
import com.sami.app.dataquality.service.ValidationReport;
import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import com.sami.app.dataquality.spi.ValidationRuleRegistry;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The validation core. Loads the active rules configured for an entity, honours
 * each rule's condition tree, resolves its validator plugin and records the run,
 * the resulting issues and the weighted quality score. It contains no business
 * rules of its own — everything it enforces is configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationEngine {

    private final QualityRuleRepository ruleRepository;
    private final ValidationRunRepository runRepository;
    private final QualityIssueRepository issueRepository;
    private final ValidationRuleRegistry validatorRegistry;
    private final QualityConditionEvaluator conditionEvaluator;
    private final QualityScoreService scoreService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Validates one entity payload against its configured rules.
     *
     * @param persist when false the run/issues are not stored (dry-run preview)
     */
    @Transactional
    public ValidationReport validate(String moduleCode, String entityCode, Long entityId,
                                     Map<String, Object> data, boolean persist) {
        Instant started = Instant.now();
        List<QualityRule> rules = ruleRepository.findActiveFor(moduleCode, entityCode);

        List<ValidationReport.ValidationFinding> findings = new ArrayList<>();
        List<QualityIssue> issues = new ArrayList<>();
        BigDecimal totalPenalty = BigDecimal.ZERO;
        BigDecimal failedPenalty = BigDecimal.ZERO;
        int passed = 0;
        int skipped = 0;
        boolean blocking = false;

        for (QualityRule rule : rules) {
            if (!conditionEvaluator.evaluate(rule.getConditionConfig(), data)) {
                skipped++;
                continue;
            }
            ValidationRule validator = validatorRegistry.find(rule.getValidationType()).orElse(null);
            if (validator == null) {
                log.warn("Quality rule {} references unknown validation type {}",
                        rule.getCode(), rule.getValidationType());
                skipped++;
                continue;
            }

            ValidationOutcome outcome;
            try {
                outcome = validator.validate(new ValidationContext(moduleCode, entityCode, entityId,
                        rule.getTargetField(), data, rule.getConfig()));
            } catch (RuntimeException ex) {
                // A misbehaving validator must never break the whole run.
                log.warn("Validator {} threw: {}", rule.getValidationType(), ex.getMessage());
                skipped++;
                continue;
            }
            if (outcome.skipped()) {
                skipped++;
                continue;
            }

            BigDecimal penalty = scoreService.penaltyOf(rule);
            totalPenalty = totalPenalty.add(penalty);
            if (outcome.valid()) {
                passed++;
                continue;
            }

            failedPenalty = failedPenalty.add(penalty);
            String severity = rule.getSeverity().getCode();
            String dimension = rule.getDimension() == null ? null : rule.getDimension().getCode();
            blocking = blocking || rule.getSeverity().isBlocksSave();
            findings.add(new ValidationReport.ValidationFinding(
                    rule.getCode(), rule.getTargetField(), severity, dimension, outcome.message()));
            issues.add(QualityIssue.builder()
                    .ruleId(rule.getId())
                    .moduleCode(moduleCode)
                    .entityCode(entityCode)
                    .entityId(entityId)
                    .fieldName(rule.getTargetField())
                    .severityCode(severity)
                    .dimensionCode(dimension)
                    .message(outcome.message())
                    .detail(outcome.detail() == null || outcome.detail().isEmpty() ? null : outcome.detail())
                    .build());
        }

        BigDecimal score = scoreService.score(failedPenalty, totalPenalty);
        String band = scoreService.bandFor(score);
        int failed = findings.size();
        String runNumber = "DQ-" + String.format("%08d", runRepository.nextNumber());

        if (persist) {
            Instant ended = Instant.now();
            ValidationRun run = runRepository.save(ValidationRun.builder()
                    .runNumber(runNumber)
                    .moduleCode(moduleCode)
                    .entityCode(entityCode)
                    .entityId(entityId)
                    .status(failed == 0 ? ValidationRun.Status.PASSED.name() : ValidationRun.Status.FAILED.name())
                    .ruleCount(rules.size())
                    .passedCount(passed)
                    .failedCount(failed)
                    .score(score)
                    .bandCode(band)
                    .blocking(blocking)
                    .payload(data == null || data.isEmpty() ? null : new HashMap<>(data))
                    .startedAt(started)
                    .endedAt(ended)
                    .durationMs(ended.toEpochMilli() - started.toEpochMilli())
                    .executedBy(CurrentActor.id())
                    .executedByEmail(CurrentActor.email())
                    .build());
            issues.forEach(issue -> issue.setRunId(run.getId()));
            issueRepository.saveAll(issues);
            scoreService.record(moduleCode, entityCode, null, score, 1,
                    Map.of("runNumber", runNumber, "failed", failed));
            publishIssueEvents(moduleCode, entityCode, entityId, issues);
        }

        eventPublisher.publishEvent(new DataQualityDomainEvent(
                "dq-" + runNumber, DataQualityDomainEvent.VALIDATION_COMPLETED,
                moduleCode, entityCode, entityId,
                Map.of("score", score, "band", String.valueOf(band), "failed", failed, "blocking", blocking),
                Instant.now()));

        return new ValidationReport(runNumber, moduleCode, entityCode, entityId,
                failed == 0, blocking, rules.size(), passed, failed, skipped, score, band, findings);
    }

    private void publishIssueEvents(String moduleCode, String entityCode, Long entityId,
                                    List<QualityIssue> issues) {
        for (QualityIssue issue : issues) {
            boolean duplicate = issue.getDetail() != null && issue.getDetail().containsKey("matches");
            eventPublisher.publishEvent(new DataQualityDomainEvent(
                    "dq-issue-" + issue.getId(),
                    duplicate ? DataQualityDomainEvent.DUPLICATE_DETECTED
                            : DataQualityDomainEvent.ISSUE_DETECTED,
                    moduleCode, entityCode, entityId,
                    Map.of("severity", issue.getSeverityCode(), "message", issue.getMessage()),
                    Instant.now()));
        }
    }
}
