package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashboardSavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link DashboardSavedFilter}. */
public interface DashboardSavedFilterRepository extends JpaRepository<DashboardSavedFilter, Long> {

    List<DashboardSavedFilter> findByOwnerIdOrderByNameAsc(Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}
