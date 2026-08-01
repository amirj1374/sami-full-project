package com.sami.app.crm.repository;

import com.sami.app.crm.domain.BlacklistReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for the configurable {@link BlacklistReason} lookup. */
public interface BlacklistReasonRepository extends JpaRepository<BlacklistReason, Long> {

    @Query("SELECT r FROM BlacklistReason r WHERE r.tenantId IS NULL OR r.tenantId = :tenantId ORDER BY r.displayOrder, r.id")
    List<BlacklistReason> findVisible(@Param("tenantId") Long tenantId);

    java.util.Optional<BlacklistReason> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
