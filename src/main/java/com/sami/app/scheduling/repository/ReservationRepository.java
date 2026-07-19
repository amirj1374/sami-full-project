package com.sami.app.scheduling.repository;

import com.sami.app.scheduling.domain.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Live reservations overlapping the window on any of the given resources.
     *
     * <p>Half-open comparison ({@code startsAt < :end AND :start < endsAt})
     * exactly matches the {@code '[)'} range used by the database exclusion
     * constraint, so this pre-check and the constraint can never disagree about
     * whether two back-to-back bookings collide.
     *
     * <p>This is an ADVISORY check used to produce good error messages and
     * alternative suggestions. It is not what guarantees correctness — the
     * exclusion constraint is. Two concurrent callers can both see "free" here
     * and only one will survive the insert, which is exactly the intended
     * behaviour.
     */
    @EntityGraph(attributePaths = {"resource", "status"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.holdsResource = TRUE
              AND r.resource.id IN :resourceIds
              AND r.startsAt < :end
              AND :start < r.endsAt
              AND (:excludeScheduleId IS NULL OR r.scheduleId IS NULL
                   OR r.scheduleId <> :excludeScheduleId)
            ORDER BY r.startsAt
            """)
    List<Reservation> findOverlapping(@Param("resourceIds") List<Long> resourceIds,
                                      @Param("start") Instant start,
                                      @Param("end") Instant end,
                                      @Param("excludeScheduleId") Long excludeScheduleId);

    /** Everything live on one resource in a window, for the day/utilisation views. */
    @EntityGraph(attributePaths = {"resource", "status"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.holdsResource = TRUE
              AND r.startsAt < :end
              AND :start < r.endsAt
            ORDER BY r.startsAt
            """)
    List<Reservation> findOnResourceBetween(@Param("resourceId") Long resourceId,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);

    @EntityGraph(attributePaths = {"resource", "status"})
    List<Reservation> findByScheduleId(Long scheduleId);

    /** Occupied minutes per resource in a window — the utilisation report. */
    @Query("""
            SELECT r.resource.id, SUM(r.durationMinutes)
            FROM Reservation r
            WHERE r.holdsResource = TRUE
              AND r.startsAt >= :start AND r.endsAt <= :end
            GROUP BY r.resource.id
            """)
    List<Object[]> sumOccupiedMinutes(@Param("start") Instant start, @Param("end") Instant end);
}
