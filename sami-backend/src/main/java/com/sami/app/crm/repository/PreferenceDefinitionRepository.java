package com.sami.app.crm.repository;

import com.sami.app.crm.domain.PreferenceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for {@link PreferenceDefinition}. */
public interface PreferenceDefinitionRepository extends JpaRepository<PreferenceDefinition, Long> {

    @Query("SELECT d FROM PreferenceDefinition d WHERE d.tenantId IS NULL OR d.tenantId = :tenantId ORDER BY d.displayOrder, d.id")
    List<PreferenceDefinition> findVisible(@Param("tenantId") Long tenantId);

    java.util.Optional<PreferenceDefinition> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndPrefKeyIgnoreCase(Long tenantId, String prefKey);
}
