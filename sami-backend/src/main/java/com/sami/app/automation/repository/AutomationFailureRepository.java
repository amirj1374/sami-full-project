package com.sami.app.automation.repository;

import com.sami.app.automation.domain.AutomationFailure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AutomationFailureRepository extends JpaRepository<AutomationFailure, Long> {

    /** Open failures whose retry is due, drained through the shared scheduler. */
    List<AutomationFailure> findByTenantIdAndResolvedFalseAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            Long tenantId, Instant now, Pageable pageable);

    Page<AutomationFailure> findByTenantIdAndResolvedOrderByCreatedAtDesc(
            Long tenantId, boolean resolved, Pageable pageable);

    Optional<AutomationFailure> findByIdAndTenantId(Long id, Long tenantId);

    long countByTenantIdAndResolved(Long tenantId, boolean resolved);
}
