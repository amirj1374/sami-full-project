package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupRating;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for {@link SupRating} (current score per supplier+criterion). */
public interface SupRatingRepository extends JpaRepository<SupRating, Long> {

    @EntityGraph(attributePaths = {"criterion"})
    List<SupRating> findBySupplierId(Long supplierId);

    Optional<SupRating> findBySupplierIdAndCriterionId(Long supplierId, Long criterionId);
}
