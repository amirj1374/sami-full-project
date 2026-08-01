package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseReturn;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link PurchaseReturn}. */
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    @EntityGraph(attributePaths = {"items", "items.purchaseItem"})
    List<PurchaseReturn> findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(Long purchaseId, Long tenantId);
}
