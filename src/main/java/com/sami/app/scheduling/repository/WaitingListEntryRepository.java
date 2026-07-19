package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.WaitingListEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, Long> {

    /**
     * The promotion queue: highest priority first, then longest waiting.
     *
     * <p>Ordering by {@code createdAt} ascending within a priority is what
     * makes the queue fair — without it, promotion order would be arbitrary
     * and a customer could be overtaken indefinitely.
     */
    @EntityGraph(attributePaths = {"appointmentType"})
    @Query("""
            SELECT w FROM WaitingListEntry w
            WHERE w.isActive = TRUE
              AND w.promotedAt IS NULL
              AND w.cancelledAt IS NULL
              AND w.appointmentType.id = :appointmentTypeId
              AND (:branchId IS NULL OR w.branchId IS NULL OR w.branchId = :branchId)
              AND w.desiredFrom <= :windowEnd
              AND w.desiredTo >= :windowStart
            ORDER BY w.priority DESC, w.createdAt ASC
            """)
    List<WaitingListEntry> findQueue(@Param("appointmentTypeId") Long appointmentTypeId,
                                     @Param("branchId") Long branchId,
                                     @Param("windowStart") Instant windowStart,
                                     @Param("windowEnd") Instant windowEnd);

    @EntityGraph(attributePaths = {"appointmentType"})
    List<WaitingListEntry> findByIsActiveTrueAndPromotedAtIsNullOrderByPriorityDescCreatedAtAsc();

    @EntityGraph(attributePaths = {"appointmentType"})
    List<WaitingListEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** Entries whose acceptance window has lapsed; drives the expiry sweep. */
    @Query("""
            SELECT w FROM WaitingListEntry w
            WHERE w.isActive = TRUE
              AND w.promotedAt IS NULL
              AND w.expiresAt IS NOT NULL
              AND w.expiresAt <= :now
            """)
    List<WaitingListEntry> findExpired(@Param("now") Instant now);
}
