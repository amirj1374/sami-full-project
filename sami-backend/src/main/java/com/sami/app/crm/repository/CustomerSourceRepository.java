package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for the configurable {@link CustomerSource} lookup. */
public interface CustomerSourceRepository extends JpaRepository<CustomerSource, Long> {

    @Query("SELECT s FROM CustomerSource s WHERE s.tenantId IS NULL OR s.tenantId = :tenantId ORDER BY s.displayOrder, s.id")
    List<CustomerSource> findVisible(@Param("tenantId") Long tenantId);

    java.util.Optional<CustomerSource> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);
}
