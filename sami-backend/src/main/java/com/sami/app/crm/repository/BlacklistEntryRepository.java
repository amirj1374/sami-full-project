package com.sami.app.crm.repository;

import com.sami.app.crm.domain.BlacklistEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for {@link BlacklistEntry}. */
public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntry, Long> {

    @EntityGraph(attributePaths = {"reason"})
    List<BlacklistEntry> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(Long tenantId, Long customerId);

    /** The open (unlifted) episode, if the customer is currently blacklisted. */
    Optional<BlacklistEntry> findFirstByTenantIdAndCustomerIdAndLiftedAtIsNullOrderByCreatedAtDesc(
            Long tenantId, Long customerId);
}
