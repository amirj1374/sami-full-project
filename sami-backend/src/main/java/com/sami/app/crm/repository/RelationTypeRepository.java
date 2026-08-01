package com.sami.app.crm.repository;

import com.sami.app.crm.domain.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for the configurable {@link RelationType} lookup. */
public interface RelationTypeRepository extends JpaRepository<RelationType, Long> {

    @Query("SELECT t FROM RelationType t WHERE t.tenantId IS NULL OR t.tenantId = :tenantId ORDER BY t.displayOrder, t.id")
    List<RelationType> findVisible(@Param("tenantId") Long tenantId);

    java.util.Optional<RelationType> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
