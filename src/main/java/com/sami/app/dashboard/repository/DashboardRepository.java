package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Data-access for {@link Dashboard}. */
public interface DashboardRepository
        extends JpaRepository<Dashboard, Long>, JpaSpecificationExecutor<Dashboard> {

    @EntityGraph(attributePaths = {
            "status", "visibility", "owner", "role",
            "widgets", "widgets.widgetType", "widgets.chartType",
            "widgets.dataSource", "widgets.refreshPolicy", "widgets.kpi"})
    Optional<Dashboard> findWithWidgetsById(Long id);

    Optional<Dashboard> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    long countByStatusId(Long statusId);

    long countByVisibilityId(Long visibilityId);

    @Override
    @EntityGraph(attributePaths = {"status", "visibility", "owner", "role"})
    Page<Dashboard> findAll(Specification<Dashboard> spec, Pageable pageable);
}
