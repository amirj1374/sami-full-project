package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.crm.domain.BlacklistEntry;
import com.sami.app.crm.domain.BlacklistReason;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerStatus;
import com.sami.app.crm.dto.BlacklistDtos.BlacklistEntryResponse;
import com.sami.app.crm.dto.BlacklistDtos.BlacklistRequest;
import com.sami.app.crm.dto.CustomerResponse;
import com.sami.app.crm.repository.BlacklistEntryRepository;
import com.sami.app.crm.repository.BlacklistReasonRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerStatusRepository;
import com.sami.app.security.CurrentActor;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Customer blacklist: configurable reasons, auditable episodes, and the
 * BLACKLISTED business event other modules react to. Blacklisted customers
 * stay searchable (the blacklist status is not hidden); whether to refuse a
 * sale/installment/repair is each business module's call, made by consulting
 * {@code status.isBlocking} or subscribing to the event.
 */
@Service
@RequiredArgsConstructor
public class CustomerBlacklistService {

    private final CustomerRepository customerRepository;
    private final CustomerStatusRepository statusRepository;
    private final BlacklistReasonRepository reasonRepository;
    private final BlacklistEntryRepository entryRepository;
    private final CustomerEventService events;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<com.sami.app.crm.dto.BlacklistDtos.BlacklistReasonResponse> reasons() {
        java.util.LinkedHashMap<String, BlacklistReason> resolved = new java.util.LinkedHashMap<>();
        reasonRepository.findVisible(tenantContext.requireTenantId()).forEach(reason -> {
            String key = reason.getCode().toLowerCase(java.util.Locale.ROOT);
            if (reason.getTenantId() == null) resolved.putIfAbsent(key, reason); else resolved.put(key, reason);
        });
        return resolved.values().stream()
                .map(com.sami.app.crm.dto.BlacklistDtos.BlacklistReasonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BlacklistEntryResponse> history(Long customerId) {
        findOrThrow(customerId);
        return entryRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
                        tenantContext.requireTenantId(), customerId).stream()
                .map(BlacklistEntryResponse::from)
                .toList();
    }

    @Transactional
    public CustomerResponse blacklist(Long customerId, BlacklistRequest request) {
        Customer customer = findOrThrow(customerId);
        if (customer.getStatus().isBlacklistState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The customer is already blacklisted");
        }
        Long tenantId = tenantContext.requireTenantId();
        BlacklistReason reason = reasonRepository.findVisible(tenantId).stream()
                .filter(candidate -> candidate.getId().equals(request.reasonId())).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Blacklist reason",
                        request.reasonId()));
        CustomerStatus blacklistStatus = visibleStatuses().stream()
                .filter(CustomerStatus::isBlacklistState).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No blacklist status is configured"));

        String from = customer.getStatus().getName();
        customer.setStatus(blacklistStatus);
        entryRepository.save(BlacklistEntry.builder()
                .tenantId(tenantId)
                .customer(customer)
                .reason(reason)
                .note(request.note())
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build());

        events.record(customerId, CustomerEventService.BLACKLISTED,
                "Blacklisted: " + reason.getName(),
                Map.of("reason", reason.getName(), "from", from));
        return CustomerResponse.from(customer);
    }

    /** Lifts the open episode and returns the customer to the default status. */
    @Transactional
    public CustomerResponse lift(Long customerId) {
        Customer customer = findOrThrow(customerId);
        if (!customer.getStatus().isBlacklistState()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The customer is not blacklisted");
        }
        CustomerStatus target = visibleStatuses().stream().filter(CustomerStatus::isDefault).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No default customer status is configured"));

        customer.setStatus(target);
        entryRepository.findFirstByTenantIdAndCustomerIdAndLiftedAtIsNullOrderByCreatedAtDesc(
                        tenantContext.requireTenantId(), customerId)
                .ifPresent(entry -> {
                    entry.setLiftedAt(Instant.now());
                    entry.setLiftedBy(CurrentActor.id());
                });

        events.record(customerId, CustomerEventService.BLACKLIST_LIFTED,
                "Blacklist lifted", null);
        return CustomerResponse.from(customer);
    }

    private Customer findOrThrow(Long id) {
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
        if (customer.getMergedInto() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "This record was merged away");
        }
        return customer;
    }

    private List<CustomerStatus> visibleStatuses() {
        java.util.LinkedHashMap<String, CustomerStatus> resolved = new java.util.LinkedHashMap<>();
        statusRepository.findVisible(tenantContext.requireTenantId()).forEach(status -> {
            String key = status.getCode().toLowerCase(java.util.Locale.ROOT);
            if (status.getTenantId() == null) resolved.putIfAbsent(key, status); else resolved.put(key, status);
        });
        return List.copyOf(resolved.values());
    }
}
