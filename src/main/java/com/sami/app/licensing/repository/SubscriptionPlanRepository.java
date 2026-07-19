package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.SubscriptionPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    boolean existsByCode(String code);

    Optional<SubscriptionPlan> findByCode(String code);

    Optional<SubscriptionPlan> findByIsDefaultTrue();

    @EntityGraph(attributePaths = {"status", "features", "limits"})
    Optional<SubscriptionPlan> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"status"})
    List<SubscriptionPlan> findAllByOrderByDisplayOrderAsc();
}
