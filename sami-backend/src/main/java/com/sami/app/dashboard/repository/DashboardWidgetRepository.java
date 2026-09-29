package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashboardWidget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data-access for {@link DashboardWidget}. */
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    @EntityGraph(attributePaths = {"dashboard", "widgetType", "chartType", "dataSource",
            "refreshPolicy", "kpi"})
    Optional<DashboardWidget> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"dashboard", "widgetType", "chartType", "dataSource",
            "refreshPolicy", "kpi"})
    Optional<DashboardWidget> findWithDetailsByIdAndTenantId(Long id, Long tenantId);

    long countByTenantId(Long tenantId);

    boolean existsByDashboardIdAndCodeIgnoreCase(Long dashboardId, String code);

    boolean existsByDashboardIdAndCodeIgnoreCaseAndIdNot(Long dashboardId, String code, Long id);

    long countByWidgetTypeId(Long widgetTypeId);
}
