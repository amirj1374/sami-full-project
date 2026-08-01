package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerAddress;
import com.sami.app.crm.domain.CustomerContact;
import com.sami.app.crm.domain.CustomerRelation;
import com.sami.app.crm.dto.CustomerDetailResponse;
import com.sami.app.crm.repository.CustomerEventRepository;
import com.sami.app.crm.repository.CustomerNoteRepository;
import com.sami.app.crm.repository.CustomerRelationRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerStatusRepository;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Merges a duplicated customer (source) into the surviving record (target).
 *
 * <p>Everything moves to the target: contacts (deduplicated by kind+value,
 * defaults of the target win), addresses (deduplicated by line+city), tags,
 * notes and the full timeline. Scalar identity fields fill gaps on the target
 * but never overwrite it. The source row is NOT removed: it keeps its code,
 * gets {@code mergedInto} + the deleted status, and every reference from any
 * module keeps resolving — transactions are never lost.
 */
@Service
@RequiredArgsConstructor
public class CustomerMergeService {

    private final CustomerRepository customerRepository;
    private final CustomerStatusRepository statusRepository;
    private final CustomerNoteRepository noteRepository;
    private final CustomerEventRepository eventRepository;
    private final CustomerRelationRepository relationRepository;
    private final CustomerEventService events;
    private final TenantContext tenantContext;

    @Transactional
    public CustomerDetailResponse merge(Long sourceId, Long targetId) {
        if (sourceId.equals(targetId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "A customer cannot be merged into itself");
        }
        Customer source = findOrThrow(sourceId);
        Customer target = findOrThrow(targetId);
        if (source.getMergedInto() != null || target.getMergedInto() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Already-merged records cannot participate in another merge");
        }

        mergeContacts(source, target);
        mergeAddresses(source, target);
        target.getTags().addAll(source.getTags());
        fillScalarGaps(source, target);
        mergePreferences(source, target);
        mergeRelations(source, target);

        // Notes and timeline move wholesale — history is preserved and unified.
        Long tenantId = tenantContext.requireTenantId();
        noteRepository.moveAll(tenantId, sourceId, targetId);
        eventRepository.moveAll(tenantId, sourceId, targetId);

        // The source stays: flagged as merged, soft-deleted, out of every listing.
        source.setMergedInto(target);
        source.setStatus(visibleStatuses().stream().filter(com.sami.app.crm.domain.CustomerStatus::isDeletedState).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No deleted status is configured")));
        source.setDeletedAt(Instant.now());
        source.getTags().clear();

        events.record(targetId, CustomerEventService.MERGED_IN,
                "Merged in customer " + source.getCustomerCode(),
                Map.of("sourceId", sourceId, "sourceCode", source.getCustomerCode()));
        events.record(sourceId, CustomerEventService.MERGED_AWAY,
                "Merged into customer " + target.getCustomerCode(),
                Map.of("targetId", targetId, "targetCode", target.getCustomerCode()));

        return CustomerDetailResponse.from(
                customerRepository.findWithDetailsByIdAndTenantId(targetId, tenantId).orElseThrow());
    }

    private void mergeContacts(Customer source, Customer target) {
        Set<String> existing = target.getContacts().stream()
                .map(c -> c.getKind() + ":" + c.getValue().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (CustomerContact contact : source.getContacts()) {
            String key = contact.getKind() + ":" + contact.getValue().toLowerCase(Locale.ROOT);
            if (existing.contains(key)) {
                continue;
            }
            // Target's defaults win; incoming contacts arrive as non-default.
            target.getContacts().add(CustomerContact.builder()
                    .customer(target)
                    .kind(contact.getKind())
                    .value(contact.getValue())
                    .label(contact.getLabel())
                    .isDefault(false)
                    .build());
            existing.add(key);
        }
        source.getContacts().clear();
    }

    private void mergeAddresses(Customer source, Customer target) {
        Set<String> existing = target.getAddresses().stream()
                .map(a -> (a.getLine() + "|" + (a.getCity() == null ? "" : a.getCity()))
                        .toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (CustomerAddress address : source.getAddresses()) {
            String key = (address.getLine() + "|"
                    + (address.getCity() == null ? "" : address.getCity())).toLowerCase(Locale.ROOT);
            if (existing.contains(key)) {
                continue;
            }
            target.getAddresses().add(CustomerAddress.builder()
                    .customer(target)
                    .label(address.getLabel())
                    .line(address.getLine())
                    .city(address.getCity())
                    .province(address.getProvince())
                    .postalCode(address.getPostalCode())
                    .isDefault(false)
                    .build());
            existing.add(key);
        }
        source.getAddresses().clear();
    }

    /** Identity fields fill gaps on the target only — the survivor's data wins. */
    private void fillScalarGaps(Customer source, Customer target) {
        if (target.getFirstName() == null) {
            target.setFirstName(source.getFirstName());
        }
        if (target.getLastName() == null) {
            target.setLastName(source.getLastName());
        }
        if (target.getNationalCode() == null) {
            target.setNationalCode(source.getNationalCode());
        }
        if (target.getPassportNumber() == null) {
            target.setPassportNumber(source.getPassportNumber());
        }
        if (target.getBirthDate() == null) {
            target.setBirthDate(source.getBirthDate());
        }
        if (target.getGender() == null) {
            target.setGender(source.getGender());
        }
        if (target.getOccupation() == null) {
            target.setOccupation(source.getOccupation());
        }
        if (target.getCompanyName() == null) {
            target.setCompanyName(source.getCompanyName());
        }
        if (target.getTaxNumber() == null) {
            target.setTaxNumber(source.getTaxNumber());
        }
        if (target.getSource() == null) {
            target.setSource(source.getSource());
        }
    }

    /** Preference values fill gaps on the target; the survivor's values win. */
    private void mergePreferences(Customer source, Customer target) {
        source.getPreferences().forEach((key, value) ->
                target.getPreferences().putIfAbsent(key, value));
    }

    /**
     * Re-parents relations from the source to the target, dropping any that
     * would become self-relations or duplicates.
     */
    private void mergeRelations(Customer source, Customer target) {
        for (CustomerRelation relation
                : relationRepository.findByTenantIdAndCustomerIdOrTenantIdAndRelatedCustomerId(
                        tenantContext.requireTenantId(), source.getId(), tenantContext.requireTenantId(), source.getId())) {
            if (relation.getCustomer().getId().equals(source.getId())) {
                relation.setCustomer(target);
            }
            if (relation.getRelatedCustomer().getId().equals(source.getId())) {
                relation.setRelatedCustomer(target);
            }
            boolean selfRelation = relation.getCustomer().getId()
                    .equals(relation.getRelatedCustomer().getId());
            boolean duplicate = !selfRelation
                    && relationRepository.existsByTenantIdAndCustomerIdAndRelatedCustomerIdAndRelationTypeIdAndIdNot(
                            tenantContext.requireTenantId(),
                            relation.getCustomer().getId(),
                            relation.getRelatedCustomer().getId(),
                            relation.getRelationType().getId(),
                            relation.getId());
            if (selfRelation) {
                relationRepository.delete(relation);
            }
            // Duplicates keep the earliest row; DB uniqueness is the backstop.
            if (duplicate) {
                relationRepository.delete(relation);
            }
        }
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findWithDetailsByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    private java.util.List<com.sami.app.crm.domain.CustomerStatus> visibleStatuses() {
        java.util.LinkedHashMap<String, com.sami.app.crm.domain.CustomerStatus> resolved = new java.util.LinkedHashMap<>();
        statusRepository.findVisible(tenantContext.requireTenantId()).forEach(status -> {
            String key = status.getCode().toLowerCase(java.util.Locale.ROOT);
            if (status.getTenantId() == null) resolved.putIfAbsent(key, status); else resolved.put(key, status);
        });
        return java.util.List.copyOf(resolved.values());
    }
}
