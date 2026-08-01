package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Data-access for the configurable {@link CustomerType} lookup. */
public interface CustomerTypeRepository extends JpaRepository<CustomerType, Long> {

    @Query("SELECT t FROM CustomerType t WHERE t.tenantId IS NULL OR t.tenantId = :tenantId ORDER BY t.displayOrder, t.id")
    List<CustomerType> findVisible(@Param("tenantId") Long tenantId);

    Optional<CustomerType> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
