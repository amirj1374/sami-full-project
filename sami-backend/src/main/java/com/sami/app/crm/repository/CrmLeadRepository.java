package com.sami.app.crm.repository;
import com.sami.app.crm.domain.CrmLead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CrmLeadRepository extends JpaRepository<CrmLead, Long> {
    List<CrmLead> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    Optional<CrmLead> findByIdAndTenantId(Long id, Long tenantId);
}
