package com.sami.app.crm.repository;
import com.sami.app.crm.domain.CrmOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CrmOpportunityRepository extends JpaRepository<CrmOpportunity, Long> {
    List<CrmOpportunity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    Optional<CrmOpportunity> findByIdAndTenantId(Long id, Long tenantId);
}
