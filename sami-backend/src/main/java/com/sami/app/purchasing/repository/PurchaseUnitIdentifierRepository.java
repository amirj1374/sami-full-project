package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseUnitIdentifier;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/** Data-access for {@link PurchaseUnitIdentifier}; backs duplicate checks. */
public interface PurchaseUnitIdentifierRepository
        extends JpaRepository<PurchaseUnitIdentifier, Long> {

    boolean existsByTenantIdAndIdentifierTypeIdAndValue(Long tenantId, Long identifierTypeId, String value);

    @EntityGraph(attributePaths = {"receiptItem", "identifierType"})
    List<PurchaseUnitIdentifier> findByReceiptItemReceiptIdInAndTenantIdOrderByReceiptItemIdAscUnitIndexAscIdAsc(
            Collection<Long> receiptIds, Long tenantId);
}
