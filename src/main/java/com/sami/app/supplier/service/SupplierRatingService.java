package com.sami.app.supplier.service;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.security.CurrentActor;
import com.sami.app.supplier.domain.SupRating;
import com.sami.app.supplier.domain.SupRatingCriterion;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.dto.SupplierDtos.RateRequest;
import com.sami.app.supplier.dto.SupplierDtos.RatingResponse;
import com.sami.app.supplier.repository.SupRatingCriterionRepository;
import com.sami.app.supplier.repository.SupRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Supplier evaluation. Scores (0–5) are upserted per criterion; the overall
 * rating is the weight-averaged score across ACTIVE criteria — weights and the
 * criteria themselves are configuration, so the calculation rule changes
 * without code. The average is cached on the supplier for filtering/sorting.
 */
@Service
@RequiredArgsConstructor
public class SupplierRatingService {

    private final SupplierService supplierService;
    private final SupRatingRepository ratingRepository;
    private final SupRatingCriterionRepository criterionRepository;
    private final SupLogService logs;

    @Transactional(readOnly = true)
    public List<RatingResponse> ratings(Long supplierId) {
        supplierService.findOrThrow(supplierId);
        return ratingRepository.findBySupplierId(supplierId).stream()
                .map(RatingResponse::from)
                .toList();
    }

    @Transactional
    public List<RatingResponse> rate(Long supplierId, RateRequest request) {
        Supplier supplier = supplierService.findOrThrow(supplierId);

        for (RateRequest.Score score : request.scores()) {
            SupRatingCriterion criterion = criterionRepository.findById(score.criterionId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Rating criterion",
                            score.criterionId()));
            SupRating rating = ratingRepository
                    .findBySupplierIdAndCriterionId(supplierId, criterion.getId())
                    .orElseGet(() -> SupRating.builder()
                            .supplier(supplier)
                            .criterion(criterion)
                            .score(score.score())
                            .build());
            rating.setScore(score.score());
            rating.setNote(score.note());
            rating.setRatedBy(CurrentActor.id());
            rating.setRatedByEmail(CurrentActor.email());
            ratingRepository.save(rating);
        }

        supplier.setRatingAvg(computeWeightedAverage(supplierId));
        logs.record(supplier, SupLogService.RATED, "Supplier rated",
                Map.of("overall", supplier.getRatingAvg() == null ? "n/a"
                        : supplier.getRatingAvg().toPlainString()));
        return ratings(supplierId);
    }

    /** Weighted average over active criteria that have a score. */
    private BigDecimal computeWeightedAverage(Long supplierId) {
        List<SupRating> ratings = ratingRepository.findBySupplierId(supplierId);
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;
        for (SupRating rating : ratings) {
            if (!rating.getCriterion().isActive()) {
                continue;
            }
            BigDecimal weight = rating.getCriterion().getWeight();
            weightedSum = weightedSum.add(rating.getScore().multiply(weight));
            weightTotal = weightTotal.add(weight);
        }
        if (weightTotal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return weightedSum.divide(weightTotal, 2, RoundingMode.HALF_UP);
    }
}
