package com.sami.app.crm.service;

import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerContact;
import com.sami.app.crm.domain.DuplicateRule;
import com.sami.app.crm.dto.ContactDto;
import com.sami.app.crm.dto.CustomerRequest;
import com.sami.app.crm.dto.DuplicateMatchResponse;
import com.sami.app.crm.repository.CustomerContactRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerSpecifications;
import com.sami.app.crm.repository.DuplicateRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable duplicate detection. Which identifiers participate (phone,
 * email, national code, passport) is read from {@code crm_duplicate_rules} at
 * check time — toggling a rule takes effect immediately, no restart, no code.
 */
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final DuplicateRuleRepository ruleRepository;
    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;

    /**
     * Finds existing (non-merged) customers colliding with the candidate on any
     * enabled identifier. {@code excludeId} skips the customer being edited.
     */
    @Transactional(readOnly = true)
    public List<DuplicateMatchResponse> findMatches(CustomerRequest candidate, Long excludeId) {
        long exclude = excludeId == null ? -1L : excludeId;
        // matched customer id -> identifiers it collided on
        Map<Long, List<String>> matches = new LinkedHashMap<>();

        for (DuplicateRule rule : ruleRepository.findByEnabledTrue()) {
            switch (rule.getIdentifier()) {
                case PHONE -> contactValues(candidate, CustomerContact.Kind.PHONE).forEach(value ->
                        contactRepository.findOwnerIds(CustomerContact.Kind.PHONE, value, exclude)
                                .forEach(id -> add(matches, id, "PHONE " + value)));
                case EMAIL -> contactValues(candidate, CustomerContact.Kind.EMAIL).forEach(value ->
                        contactRepository.findOwnerIds(CustomerContact.Kind.EMAIL, value, exclude)
                                .forEach(id -> add(matches, id, "EMAIL " + value)));
                case NATIONAL_CODE -> fieldMatches(candidate.nationalCode(), "nationalCode", exclude)
                        .forEach(id -> add(matches, id, "NATIONAL_CODE"));
                case PASSPORT -> fieldMatches(candidate.passportNumber(), "passportNumber", exclude)
                        .forEach(id -> add(matches, id, "PASSPORT"));
            }
        }

        return matches.entrySet().stream()
                .map(entry -> customerRepository.findById(entry.getKey())
                        .map(c -> new DuplicateMatchResponse(
                                c.getId(), c.getCustomerCode(), c.getDisplayName(), entry.getValue()))
                        .orElse(null))
                .filter(m -> m != null)
                .toList();
    }

    private List<String> contactValues(CustomerRequest candidate, CustomerContact.Kind kind) {
        if (candidate.contacts() == null) {
            return List.of();
        }
        return candidate.contacts().stream()
                .filter(c -> c.kind() == kind)
                .map(ContactDto::value)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .toList();
    }

    private List<Long> fieldMatches(String value, String attribute, long excludeId) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Specification<Customer> spec = Specification.allOf(
                CustomerSpecifications.notMerged(),
                (root, query, cb) -> cb.equal(root.get(attribute), value),
                (root, query, cb) -> cb.notEqual(root.get("id"), excludeId));
        return customerRepository.findAll(spec).stream().map(Customer::getId).toList();
    }

    private void add(Map<Long, List<String>> matches, Long id, String reason) {
        matches.computeIfAbsent(id, k -> new java.util.ArrayList<>());
        if (!matches.get(id).contains(reason)) {
            matches.get(id).add(reason);
        }
    }
}
