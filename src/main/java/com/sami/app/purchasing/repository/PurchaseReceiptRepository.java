package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseReceipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link PurchaseReceipt}. */
public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {

    @EntityGraph(attributePaths = {"items", "items.purchaseItem",
            "items.identifiers", "items.identifiers.identifierType"})
    List<PurchaseReceipt> findByPurchaseIdOrderByCreatedAtDesc(Long purchaseId);
}
