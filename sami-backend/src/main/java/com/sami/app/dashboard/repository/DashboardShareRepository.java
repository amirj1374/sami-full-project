package com.sami.app.dashboard.repository;

import com.sami.app.dashboard.domain.DashboardShare;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for {@link DashboardShare}. */
public interface DashboardShareRepository extends JpaRepository<DashboardShare, Long> {

    @EntityGraph(attributePaths = {"targetUser", "targetRole"})
    List<DashboardShare> findByDashboardId(Long dashboardId);

    /** Dashboard ids visible to a user through a direct or role-based share. */
    @Query("""
            SELECT DISTINCT s.dashboard.id FROM DashboardShare s
            WHERE s.targetUser.id = :userId
               OR (:roleId IS NOT NULL AND s.targetRole.id = :roleId)
            """)
    List<Long> findDashboardIdsSharedWith(@Param("userId") Long userId,
                                          @Param("roleId") Long roleId);

    /** Dashboard ids the user may EDIT through a direct or role-based share. */
    @Query("""
            SELECT DISTINCT s.dashboard.id FROM DashboardShare s
            WHERE s.canEdit = true
              AND (s.targetUser.id = :userId
                   OR (:roleId IS NOT NULL AND s.targetRole.id = :roleId))
            """)
    List<Long> findEditableDashboardIdsSharedWith(@Param("userId") Long userId,
                                                  @Param("roleId") Long roleId);

    boolean existsByDashboardIdAndTargetUserId(Long dashboardId, Long targetUserId);

    boolean existsByDashboardIdAndTargetRoleId(Long dashboardId, Long targetRoleId);
}
