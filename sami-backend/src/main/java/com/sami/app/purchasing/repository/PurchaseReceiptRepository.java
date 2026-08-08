package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseReceipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link PurchaseReceipt}. */
public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {

    /**
     * Loads the receipt and its lines for the history read model. Identifiers
     * are deliberately loaded by {@code PurchaseUnitIdentifierRepository} in
     * a second query: fetching both collection-valued associations here makes
     * Hibernate attempt to join-fetch two bags.
     */
    @EntityGraph(attributePaths = {"items", "items.purchaseItem"})
    List<PurchaseReceipt> findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(Long purchaseId, Long tenantId);
}
