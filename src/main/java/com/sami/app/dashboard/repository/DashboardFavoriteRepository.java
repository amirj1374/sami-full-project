package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashboardFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link DashboardFavorite} (composite key). */
public interface DashboardFavoriteRepository
        extends JpaRepository<DashboardFavorite, DashboardFavorite.Key> {

    List<DashboardFavorite> findByUserId(Long userId);

    boolean existsByUserIdAndDashboardId(Long userId, Long dashboardId);
}
