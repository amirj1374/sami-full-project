package com.sami.app.organization.repository;

import com.sami.app.organization.domain.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByTenantIdAndCompanyIdOrderByDisplayOrderAscNameAsc(Long tenantId, Long companyId);
    Optional<Branch> findByIdAndTenantIdAndCompanyId(Long id, Long tenantId, Long companyId);
    boolean existsByTenantIdAndCompanyIdAndCodeIgnoreCase(Long tenantId, Long companyId, String code);
    boolean existsByTenantIdAndCompanyIdAndCodeIgnoreCaseAndIdNot(Long tenantId, Long companyId, String code, Long id);
}
