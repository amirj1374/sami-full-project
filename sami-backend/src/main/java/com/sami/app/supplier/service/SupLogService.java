package com.sami.app.supplier.service;

import com.sami.app.security.CurrentActor;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.supplier.domain.SupLog;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.dto.SupplierDtos.LogResponse;
import com.sami.app.supplier.event.SupplierDomainEvent;
import com.sami.app.supplier.repository.SupLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Append-only supplier history; every entry is republished as a
 * {@link SupplierDomainEvent} for other modules.
 */
@Service
@RequiredArgsConstructor
public class SupLogService {
    private final TenantContext tenantContext;

    public static final String CREATED = "CREATED";
    public static final String UPDATED = "UPDATED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";
    public static final String RATED = "RATED";
    public static final String ARCHIVED = "ARCHIVED";
    public static final String RESTORED = "RESTORED";
    public static final String DELETED = "DELETED";
    public static final String DOCUMENT_ADDED = "DOCUMENT_ADDED";

    private final SupLogRepository logRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Supplier supplier, String action, String title, Map<String, Object> detail) {
        SupLog log = logRepository.save(SupLog.builder()
                .supplierId(supplier.getId())
                .action(action)
                .title(title)
                .detail(detail == null || detail.isEmpty() ? null : detail)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .build());
        eventPublisher.publishEvent(new SupplierDomainEvent(
                tenantContext.requireTenantId(), supplier.getId(), supplier.getSupplierCode(), action,
                log.getDetail(), log.getOccurredAt()));
    }

    @Transactional(readOnly = true)
    public Page<LogResponse> history(Long supplierId, Pageable pageable) {
        return logRepository.findBySupplierIdOrderByOccurredAtDesc(supplierId, pageable)
                .map(LogResponse::from);
    }
}
