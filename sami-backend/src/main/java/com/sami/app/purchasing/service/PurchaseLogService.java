package com.sami.app.purchasing.service;

import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseLog;
import com.sami.app.purchasing.dto.PurchaseDtos.LogResponse;
import com.sami.app.purchasing.event.PurchaseDomainEvent;
import com.sami.app.purchasing.repository.PurchaseLogRepository;
import com.sami.app.security.CurrentActor;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Append-only purchase history. Every entry is also published as a
 * {@link PurchaseDomainEvent} — the integration channel for inventory,
 * accounting and payment modules.
 */
@Service
@RequiredArgsConstructor
public class PurchaseLogService {
    private final TenantContext tenantContext;

    public static final String CREATED = "CREATED";
    public static final String UPDATED = "UPDATED";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVED = "APPROVED";
    public static final String SETTLEMENT_UPDATED = "SETTLEMENT_UPDATED";
    public static final String REJECTED = "REJECTED";
    public static final String RECEIVED = "RECEIVED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String RETURNED = "RETURNED";
    public static final String ATTACHMENT_ADDED = "ATTACHMENT_ADDED";

    private final PurchaseLogRepository logRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Purchase purchase, String action, String title, Map<String, Object> detail) {
        PurchaseLog log = logRepository.save(PurchaseLog.builder()
                .tenantId(purchase.getTenantId())
                .purchaseId(purchase.getId())
                .action(action)
                .title(title)
                .detail(detail == null || detail.isEmpty() ? null : detail)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .build());
        eventPublisher.publishEvent(new PurchaseDomainEvent(
                purchase.getTenantId(), purchase.getId(), purchase.getPurchaseNumber(), action,
                log.getDetail(), log.getOccurredAt()));
    }

    @Transactional(readOnly = true)
    public Page<LogResponse> history(Long purchaseId, Pageable pageable) {
        return logRepository.findByPurchaseIdAndTenantIdOrderByOccurredAtDesc(
                        purchaseId, tenantContext.requireTenantId(), pageable)
                .map(LogResponse::from);
    }
}
