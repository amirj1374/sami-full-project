package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>,
        JpaSpecificationExecutor<Schedule> {

    String GRAPH = "appointmentType";

    @EntityGraph(attributePaths = {"appointmentType", "status", "category"})
    Optional<Schedule> findByScheduleNumber(String scheduleNumber);

    @EntityGraph(attributePaths = {"appointmentType", "status", "category"})
    Optional<Schedule> findWithDetailsById(Long id);

    /**
     * {@code JpaSpecificationExecutor.findAll} does NOT inherit the
     * {@code @EntityGraph} declared on other methods — a lesson from V18,
     * where the omission produced a LazyInitializationException on every
     * filtered list. Overriding it here attaches the graph explicitly.
     */
    @Override
    @EntityGraph(attributePaths = {"appointmentType", "status", "category"})
    Page<Schedule> findAll(Specification<Schedule> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"appointmentType", "status"})
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.branchId = :branchId
              AND s.startsAt >= :start AND s.startsAt < :end
            ORDER BY s.startsAt
            """)
    List<Schedule> findDaySchedule(@Param("branchId") Long branchId,
                                   @Param("start") Instant start,
                                   @Param("end") Instant end);

    @EntityGraph(attributePaths = {"appointmentType", "status"})
    List<Schedule> findByCustomerIdOrderByStartsAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"appointmentType", "status"})
    List<Schedule> findBySupplierIdOrderByStartsAtDesc(Long supplierId);

    List<Schedule> findByModuleCodeAndRelatedEntityTypeAndRelatedEntityId(
            String moduleCode, String relatedEntityType, Long relatedEntityId);

    /**
     * Appointments that started, were never checked in, and are past their
     * type's no-show threshold. Drives the auto-no-show sweep.
     */
    @EntityGraph(attributePaths = {"appointmentType", "status"})
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.checkedInAt IS NULL
              AND s.status.isTerminal = FALSE
              AND s.startsAt < :cutoff
            ORDER BY s.startsAt
            """)
    List<Schedule> findCandidateNoShows(@Param("cutoff") Instant cutoff);

    @Query(value = "SELECT nextval('schedule_number_seq')", nativeQuery = true)
    Long nextScheduleSequence();
}
