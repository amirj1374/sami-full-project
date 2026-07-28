package com.sami.app.supplier.repository;

import com.sami.app.supplier.domain.Supplier;
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

/** Data-access for {@link Supplier}. */
public interface SupplierRepository
        extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    @EntityGraph(attributePaths = {"type", "status", "paymentTerm", "tags", "categories",
            "channels", "addresses", "contacts", "bankAccounts"})
    Optional<Supplier> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"type", "status", "paymentTerm", "tags", "categories"})
    Page<Supplier> findAll(Specification<Supplier> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"type", "status", "paymentTerm", "channels"})
    List<Supplier> findAll(Specification<Supplier> spec, Sort sort);

    long countByStatusId(Long statusId);

    long countByTypeId(Long typeId);

    long countByPaymentTermId(Long paymentTermId);

    /** Next value of the supplier-code sequence (codes are DB-unique). */
    @Query(value = "SELECT nextval('sup_code_seq')", nativeQuery = true)
    long nextCodeNumber();
}
