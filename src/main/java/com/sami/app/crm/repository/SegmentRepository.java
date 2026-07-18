package com.sami.app.crm.repository;

import com.sami.app.crm.domain.Segment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for stored {@link Segment}s. */
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    List<Segment> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
