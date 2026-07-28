package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerRelation;
import com.sami.app.crm.domain.RelationType;
import com.sami.app.crm.dto.RelationDtos.RelationRequest;
import com.sami.app.crm.dto.RelationDtos.RelationResponse;
import com.sami.app.crm.repository.CustomerRelationRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.RelationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Customer-to-customer relationships (family, partners, guarantor, emergency /
 * company contacts — all runtime-configurable types). Relations are visible
 * from both sides; additions and removals land on both timelines.
 */
@Service
@RequiredArgsConstructor
public class CustomerRelationService {

    private final CustomerRelationRepository relationRepository;
    private final CustomerRepository customerRepository;
    private final RelationTypeRepository relationTypeRepository;
    private final CustomerEventService events;

    @Transactional(readOnly = true)
    public List<RelationResponse> list(Long customerId) {
        requireCustomer(customerId);
        return relationRepository.findAllInvolving(customerId).stream()
                .map(r -> RelationResponse.from(r, customerId))
                .toList();
    }

    @Transactional
    public RelationResponse add(Long customerId, RelationRequest request) {
        if (customerId.equals(request.relatedCustomerId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "A customer cannot be related to themselves");
        }
        Customer customer = requireCustomer(customerId);
        Customer related = requireCustomer(request.relatedCustomerId());
        RelationType type = relationTypeRepository.findById(request.relationTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Relation type",
                        request.relationTypeId()));
        if (relationRepository.existsByCustomerIdAndRelatedCustomerIdAndRelationTypeId(
                customerId, related.getId(), type.getId())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "This relation already exists");
        }

        CustomerRelation relation = relationRepository.save(CustomerRelation.builder()
                .customer(customer)
                .relatedCustomer(related)
                .relationType(type)
                .note(request.note())
                .build());

        Map<String, Object> detail = Map.of("type", type.getName(),
                "with", related.getCustomerCode());
        events.record(customerId, CustomerEventService.RELATION_ADDED,
                "%s: %s".formatted(type.getName(), related.getDisplayName()), detail);
        events.record(related.getId(), CustomerEventService.RELATION_ADDED,
                "%s (reverse): %s".formatted(type.getName(), customer.getDisplayName()),
                Map.of("type", type.getName(), "with", customer.getCustomerCode()));

        return RelationResponse.from(relation, customerId);
    }

    @Transactional
    public void remove(Long customerId, Long relationId) {
        CustomerRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Relation", relationId));
        boolean involves = relation.getCustomer().getId().equals(customerId)
                || relation.getRelatedCustomer().getId().equals(customerId);
        if (!involves) {
            throw ResourceNotFoundException.of("Relation", relationId);
        }

        Long ownerId = relation.getCustomer().getId();
        Long otherId = relation.getRelatedCustomer().getId();
        String typeName = relation.getRelationType().getName();
        relationRepository.delete(relation);
        events.record(ownerId, CustomerEventService.RELATION_REMOVED,
                "Relation removed: " + typeName, null);
        events.record(otherId, CustomerEventService.RELATION_REMOVED,
                "Relation removed: " + typeName, null);
    }

    private Customer requireCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
        if (customer.getMergedInto() != null) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Merged records cannot hold relations");
        }
        return customer;
    }
}
