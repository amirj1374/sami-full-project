package com.sami.app.crm.repository;

import com.sami.app.crm.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Data-access for {@link Customer}. */
public interface CustomerRepository
        extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    /**
     * Detail load fetches references and tags in one query. Contacts and
     * addresses remain transactionally initialized collections: fetching both
     * list-valued associations in this graph triggers Hibernate's
     * MultipleBagFetchException.
     */
    @EntityGraph(attributePaths = {"type", "status", "source", "tags"})
    Optional<Customer> findWithDetailsByIdAndTenantId(Long id, Long tenantId);

    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    /** Listing override: reference data fetched with the page (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"type", "status", "source", "tags"})
    Page<Customer> findAll(Specification<Customer> spec, Pageable pageable);

    /** Export override. */
    @Override
    @EntityGraph(attributePaths = {"type", "status", "source", "contacts"})
    List<Customer> findAll(Specification<Customer> spec, Sort sort);

    long countByTenantIdAndStatusId(Long tenantId, Long statusId);

    long countByTenantIdAndTypeId(Long tenantId, Long typeId);

    long countByTenantIdAndSourceId(Long tenantId, Long sourceId);

    /** Next value of the customer-code sequence (codes are DB-unique). */
    @Query(value = "SELECT nextval('customer_code_seq')", nativeQuery = true)
    long nextCodeNumber();

    // --- Reporting aggregates (merged-away records excluded) -----------------

    long countByTenantIdAndMergedIntoIsNull(Long tenantId);

    long countByTenantIdAndCreatedAtAfterAndMergedIntoIsNull(Long tenantId, java.time.Instant after);

    @Query("SELECT c.status.name, COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId AND c.mergedInto IS NULL GROUP BY c.status.name")
    List<Object[]> countByStatus(Long tenantId);

    @Query("SELECT c.type.name, COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId AND c.mergedInto IS NULL GROUP BY c.type.name")
    List<Object[]> countByType(Long tenantId);

    @Query("SELECT COALESCE(s.name, 'Unknown'), COUNT(c) FROM Customer c "
            + "LEFT JOIN c.source s WHERE c.tenantId = :tenantId AND c.mergedInto IS NULL GROUP BY s.name")
    List<Object[]> countBySource(Long tenantId);
}
