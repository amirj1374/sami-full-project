package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link CustomerStatus} lookup. */
public interface CustomerStatusRepository extends JpaRepository<CustomerStatus, Long> {

    @Query("SELECT s FROM CustomerStatus s WHERE s.tenantId IS NULL OR s.tenantId = :tenantId ORDER BY s.displayOrder, s.id")
    List<CustomerStatus> findVisible(@Param("tenantId") Long tenantId);

    Optional<CustomerStatus> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
