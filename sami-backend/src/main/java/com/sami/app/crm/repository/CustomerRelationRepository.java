package com.sami.app.crm.repository;

import com.sami.app.crm.domain.CustomerRelation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data-access for {@link CustomerRelation}. */
public interface CustomerRelationRepository extends JpaRepository<CustomerRelation, Long> {

    /** Relations in BOTH directions: where the customer is owner or target. */
    @EntityGraph(attributePaths = {"customer", "relatedCustomer", "relationType"})
    @Query("""
            SELECT r FROM CustomerRelation r
            WHERE r.tenantId = :tenantId
              AND (r.customer.id = :customerId OR r.relatedCustomer.id = :customerId)
            ORDER BY r.id
            """)
    List<CustomerRelation> findAllInvolving(@Param("tenantId") Long tenantId,
                                             @Param("customerId") Long customerId);

    boolean existsByTenantIdAndCustomerIdAndRelatedCustomerIdAndRelationTypeId(
            Long tenantId, Long customerId, Long relatedCustomerId, Long relationTypeId);

    /** Duplicate check that excludes the row being re-parented (merge path). */
    boolean existsByTenantIdAndCustomerIdAndRelatedCustomerIdAndRelationTypeIdAndIdNot(
            Long tenantId, Long customerId, Long relatedCustomerId, Long relationTypeId, Long id);

    List<CustomerRelation> findByTenantIdAndCustomerIdOrTenantIdAndRelatedCustomerId(
            Long tenantId, Long customerId, Long tenantIdAgain, Long relatedCustomerId);

    java.util.Optional<CustomerRelation> findByIdAndTenantId(Long id, Long tenantId);
}
