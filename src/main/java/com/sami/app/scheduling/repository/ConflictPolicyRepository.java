package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.ConflictPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConflictPolicyRepository extends JpaRepository<ConflictPolicy, Long> {
    Optional<ConflictPolicy> findByCode(String code);
    Optional<ConflictPolicy> findFirstByIsDefaultTrueAndIsActiveTrue();
    List<ConflictPolicy> findByIsActiveTrueOrderByDisplayOrderAsc();
}
