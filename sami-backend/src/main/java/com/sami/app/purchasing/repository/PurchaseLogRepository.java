package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-access for the append-only {@link PurchaseLog}. */
public interface PurchaseLogRepository extends JpaRepository<PurchaseLog, Long> {

    Page<PurchaseLog> findByPurchaseIdAndTenantIdOrderByOccurredAtDesc(
            Long purchaseId, Long tenantId, Pageable pageable);
}
