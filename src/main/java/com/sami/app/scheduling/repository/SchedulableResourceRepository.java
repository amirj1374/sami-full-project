package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.SchedulableResource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchedulableResourceRepository extends JpaRepository<SchedulableResource, Long> {

    @EntityGraph(attributePaths = {"category", "status"})
    Optional<SchedulableResource> findByResourceCode(String resourceCode);

    @EntityGraph(attributePaths = {"category", "status"})
    Optional<SchedulableResource> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"category", "status"})
    List<SchedulableResource> findByIsActiveTrueOrderByPriorityDescDisplayOrderAsc();

    /**
     * Candidate resources for a booking, most preferred first.
     *
     * <p>Filters on {@code status.allowsBooking} rather than a status code, so
     * introducing a new blocking status needs no change here. A NULL branch
     * matches every branch — resources shared across sites.
     */
    @EntityGraph(attributePaths = {"category", "status"})
    @Query("""
            SELECT r FROM SchedulableResource r
            WHERE r.isActive = TRUE
              AND r.status.allowsBooking = TRUE
              AND (:branchId IS NULL OR r.branchId = :branchId OR r.branchId IS NULL)
              AND (:categoryId IS NULL OR r.category.id = :categoryId)
            ORDER BY r.priority DESC, r.displayOrder ASC, r.id ASC
            """)
    List<SchedulableResource> findBookable(@Param("branchId") Long branchId,
                                           @Param("categoryId") Long categoryId);

    @EntityGraph(attributePaths = {"category", "status"})
    List<SchedulableResource> findByBranchIdAndIsActiveTrue(Long branchId);

    Optional<SchedulableResource> findByUserId(Long userId);
}
