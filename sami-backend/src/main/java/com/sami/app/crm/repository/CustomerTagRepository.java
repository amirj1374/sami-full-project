package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for the dynamic {@link CustomerTag} lookup. */
public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {

    @Query("SELECT t FROM CustomerTag t WHERE t.tenantId IS NULL OR t.tenantId = :tenantId ORDER BY t.name, t.id")
    List<CustomerTag> findVisible(@Param("tenantId") Long tenantId);

    java.util.Optional<CustomerTag> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(Long tenantId, String name, Long id);
}
