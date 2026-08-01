package com.sami.app.crm.repository;

import com.sami.app.crm.domain.Segment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for stored {@link Segment}s. */
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    List<Segment> findAllByTenantIdOrderByNameAsc(Long tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(Long tenantId, String name, Long id);

    java.util.Optional<Segment> findByIdAndTenantId(Long id, Long tenantId);
}
