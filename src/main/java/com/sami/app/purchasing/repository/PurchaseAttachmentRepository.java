package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.PurchaseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data-access for {@link PurchaseAttachment}. */
public interface PurchaseAttachmentRepository extends JpaRepository<PurchaseAttachment, Long> {

    List<PurchaseAttachment> findByPurchaseIdOrderByCreatedAtDesc(Long purchaseId);
}
