package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data-access for the append-only {@link CustomerEvent} timeline. */
public interface CustomerEventRepository extends JpaRepository<CustomerEvent, Long> {

    Page<CustomerEvent> findByCustomerIdOrderByOccurredAtDesc(Long customerId, Pageable pageable);

    Page<CustomerEvent> findByCustomerIdAndEventTypeOrderByOccurredAtDesc(
            Long customerId, String eventType, Pageable pageable);

    /** Re-parents timeline events during a merge (history is never lost). */
    @Modifying
    @Query("UPDATE CustomerEvent e SET e.customerId = :targetId WHERE e.customerId = :sourceId")
    int moveAll(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
