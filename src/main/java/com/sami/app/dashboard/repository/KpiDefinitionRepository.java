package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.KpiDefinition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Data-access for {@link KpiDefinition}. */
public interface KpiDefinitionRepository
        extends JpaRepository<KpiDefinition, Long>, JpaSpecificationExecutor<KpiDefinition> {

    @EntityGraph(attributePaths = {"status", "dataSource", "refreshPolicy", "owner", "thresholds"})
    Optional<KpiDefinition> findWithDetailsById(Long id);

    Optional<KpiDefinition> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    long countByDataSourceId(Long dataSourceId);
}
