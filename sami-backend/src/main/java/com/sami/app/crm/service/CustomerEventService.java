package com.sami.app.crm.service;

import com.sami.app.crm.domain.CustomerEvent;
import com.sami.app.crm.dto.CustomerEventResponse;
import com.sami.app.crm.event.CustomerDomainEvent;
import com.sami.app.crm.repository.CustomerEventRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.common.exception.ResourceNotFoundException;
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
 * The customer timeline — and the CRM's integration surface.
 *
 * <p>Other modules (sales, repairs, installments, payments, support…) call
 * {@link #record} with their own event type and {@code sourceModule} to append
 * to a customer's 360° history. The CRM depends on none of them; they depend
 * only on this service. Event types are an open set by design.
 */
@Service
@RequiredArgsConstructor
public class CustomerEventService {
    private final TenantContext tenantContext;

    /** CRM-recorded event types. Other modules define their own constants. */
    public static final String CREATED = "CREATED";
    public static final String UPDATED = "UPDATED";
    public static final String CONTACT_CHANGED = "CONTACT_CHANGED";
    public static final String ADDRESS_CHANGED = "ADDRESS_CHANGED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";
    public static final String TAGS_CHANGED = "TAGS_CHANGED";
    public static final String ARCHIVED = "ARCHIVED";
    public static final String RESTORED = "RESTORED";
    public static final String DELETED = "DELETED";
    public static final String NOTE_ADDED = "NOTE_ADDED";
    public static final String MERGED_IN = "MERGED_IN";
    public static final String MERGED_AWAY = "MERGED_AWAY";
    public static final String AVATAR_UPDATED = "AVATAR_UPDATED";
    public static final String BLACKLISTED = "BLACKLISTED";
    public static final String BLACKLIST_LIFTED = "BLACKLIST_LIFTED";
    public static final String RELATION_ADDED = "RELATION_ADDED";
    public static final String RELATION_REMOVED = "RELATION_REMOVED";
    public static final String PREFERENCES_UPDATED = "PREFERENCES_UPDATED";
    public static final String IMPORTED = "IMPORTED";

    private final CustomerEventRepository eventRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Appends one CRM event within the calling transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long customerId, String eventType, String title, Map<String, Object> detail) {
        record(customerId, eventType, title, detail, "crm");
    }

    /**
     * Appends one event on behalf of any module. Public entry point for the rest
     * of the ERP; must be called inside a transaction so the business action and
     * its history entry commit atomically.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long customerId, String eventType, String title,
                       Map<String, Object> detail, String sourceModule) {
        Long tenantId = tenantContext.requireTenantId();
        customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        CustomerEvent event = eventRepository.save(CustomerEvent.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .eventType(eventType)
                .title(title)
                .detail(detail == null || detail.isEmpty() ? null : detail)
                .sourceModule(sourceModule)
                .actorId(CurrentActor.id())
                .actorEmail(CurrentActor.email())
                .build());
        // Also published as an in-process business event: subscribers use
        // @EventListener (in-transaction) or @TransactionalEventListener
        // (after commit) without any dependency on CRM internals.
        eventPublisher.publishEvent(new CustomerDomainEvent(
                tenantId, customerId, eventType, title, event.getDetail(), sourceModule,
                event.getOccurredAt()));
    }

    @Transactional(readOnly = true)
    public Page<CustomerEventResponse> timeline(Long customerId, String eventType, Pageable pageable) {
        Long tenantId = tenantContext.requireTenantId();
        customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        Page<CustomerEvent> page = (eventType == null || eventType.isBlank())
                ? eventRepository.findByTenantIdAndCustomerIdOrderByOccurredAtDesc(tenantId, customerId, pageable)
                : eventRepository.findByTenantIdAndCustomerIdAndEventTypeOrderByOccurredAtDesc(
                        tenantId, customerId, eventType, pageable);
        return page.map(CustomerEventResponse::from);
    }
}
