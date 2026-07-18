package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.SupLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-access for the append-only {@link SupLog}. */
public interface SupLogRepository extends JpaRepository<SupLog, Long> {

    Page<SupLog> findBySupplierIdOrderByOccurredAtDesc(Long supplierId, Pageable pageable);
}
