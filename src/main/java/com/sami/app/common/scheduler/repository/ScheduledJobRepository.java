package com.sami.app.common.scheduler.repository;

import com.sami.app.common.scheduler.domain.ScheduledJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {

    @EntityGraph(attributePaths = {"status"})
    Optional<ScheduledJob> findByCode(String code);

    @Override
    @EntityGraph(attributePaths = {"status"})
    Optional<ScheduledJob> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"status"})
    List<ScheduledJob> findAll();

    /** Due work, oldest first, limited so a burst after downtime stays bounded. */
    @EntityGraph(attributePaths = {"status"})
    @Query("SELECT j FROM ScheduledJob j WHERE j.status.allowsRun = TRUE "
            + "AND j.nextRunAt IS NOT NULL AND j.nextRunAt <= :now ORDER BY j.nextRunAt ASC")
    List<ScheduledJob> findDue(@Param("now") Instant now, Pageable pageable);

    @EntityGraph(attributePaths = {"status"})
    List<ScheduledJob> findAllByRunOnStartupTrue();

    /**
     * Compare-and-swap claim. Advancing {@code next_run_at} only if it still holds
     * the value we read means two application instances polling simultaneously
     * cannot both run the same occurrence — the row update is the lock.
     *
     * @return 1 if this instance won the claim, 0 if another already took it
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ScheduledJob j SET j.nextRunAt = :nextRunAt "
            + "WHERE j.id = :id AND j.nextRunAt = :expectedRunAt")
    int claim(@Param("id") Long id,
              @Param("expectedRunAt") Instant expectedRunAt,
              @Param("nextRunAt") Instant nextRunAt);
}
