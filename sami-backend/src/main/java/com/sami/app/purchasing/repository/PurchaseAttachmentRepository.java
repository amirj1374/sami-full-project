package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for {@link PurchaseAttachment}. */
public interface PurchaseAttachmentRepository extends JpaRepository<PurchaseAttachment, Long> {

    List<PurchaseAttachment> findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(Long purchaseId, Long tenantId);

    Optional<PurchaseAttachment> findByIdAndPurchaseIdAndTenantId(Long id, Long purchaseId, Long tenantId);
}
