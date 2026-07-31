package com.sami.app.dataquality.service;

import com.sami.app.dataquality.domain.QualityRule;
import com.sami.app.dataquality.domain.QualityScore;
import com.sami.app.dataquality.domain.QualityScoreBand;
import com.sami.app.dataquality.repository.QualityScoreBandRepository;
import com.sami.app.dataquality.repository.QualityScoreRepository;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Configurable quality scoring. A failure's penalty is the product of three
 * configured weights — rule × severity × dimension — so the same engine supports
 * rule-based, severity-weighted and dimension-weighted scoring without code
 * changes. The resulting 0–100 score is mapped to a configurable band.
 */
@Service
@RequiredArgsConstructor
public class QualityScoreService {

    private final QualityScoreBandRepository bandRepository;
    private final QualityScoreRepository scoreRepository;
    private final TenantContext tenantContext;

    /** Weighted penalty for one rule (used for both the numerator and the max). */
    public BigDecimal penaltyOf(QualityRule rule) {
        BigDecimal ruleWeight = rule.getWeight() == null ? BigDecimal.ONE : rule.getWeight();
        BigDecimal severityWeight = rule.getSeverity() == null || rule.getSeverity().getWeight() == null
                ? BigDecimal.ONE : rule.getSeverity().getWeight();
        BigDecimal dimensionWeight = rule.getDimension() == null || rule.getDimension().getWeight() == null
                ? BigDecimal.ONE : rule.getDimension().getWeight();
        return ruleWeight.multiply(severityWeight).multiply(dimensionWeight);
    }

    /** 100 when nothing failed; 0 when every evaluated rule failed. */
    public BigDecimal score(BigDecimal failedPenalty, BigDecimal totalPenalty) {
        if (totalPenalty == null || totalPenalty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal ratio = failedPenalty.divide(totalPenalty, 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100)
                .multiply(BigDecimal.ONE.subtract(ratio))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Highest band whose threshold the score reaches. */
    public String bandFor(BigDecimal score) {
        if (score == null) {
            return null;
        }
        return bandRepository.findAllByOrderByMinScoreDesc().stream()
                .filter(b -> score.compareTo(b.getMinScore()) >= 0)
                .map(QualityScoreBand::getCode)
                .findFirst()
                .orElse(null);
    }

    /** Persists a score snapshot for trend analysis. */
    @Transactional
    public QualityScore record(String moduleCode, String entityCode, String dimensionCode,
                               BigDecimal score, int sampleSize, Map<String, Object> detail) {
        return scoreRepository.save(QualityScore.builder()
                .tenantId(tenantContext.requireTenantId())
                .moduleCode(moduleCode)
                .entityCode(entityCode)
                .dimensionCode(dimensionCode)
                .score(score)
                .bandCode(bandFor(score))
                .sampleSize(sampleSize)
                .detail(detail == null || detail.isEmpty() ? null : detail)
                .build());
    }

    @Transactional(readOnly = true)
    public List<QualityScore> trend(String moduleCode, String entityCode) {
        return scoreRepository.findTop30ByTenantIdAndModuleCodeAndEntityCodeOrderByComputedAtDesc(
                tenantContext.requireTenantId(), moduleCode, entityCode);
    }

    @Transactional(readOnly = true)
    public List<QualityScoreBand> bands() {
        return bandRepository.findAllByOrderByDisplayOrderAsc();
    }
}
