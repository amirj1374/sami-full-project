package com.sami.app.organization.repository;

import com.sami.app.organization.domain.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByTenantIdOrderByDisplayOrderAscNameAsc(Long tenantId);
    Optional<Company> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
