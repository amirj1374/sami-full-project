package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupRatingCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for the configurable {@link SupRatingCriterion} lookup. */
public interface SupRatingCriterionRepository extends JpaRepository<SupRatingCriterion, Long> {

    List<SupRatingCriterion> findAllByOrderByDisplayOrderAsc();

    List<SupRatingCriterion> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCodeIgnoreCase(String code);
}
