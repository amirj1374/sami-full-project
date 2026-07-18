package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Data-access for {@link Purchase}. */
public interface PurchaseRepository
        extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @EntityGraph(attributePaths = {"type", "status", "supplier", "warehouse",
            "items", "items.product", "cancelReason"})
    Optional<Purchase> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"type", "status", "supplier", "warehouse"})
    Page<Purchase> findAll(Specification<Purchase> spec, Pageable pageable);

    long countByStatusId(Long statusId);

    long countByTypeId(Long typeId);

    long countByWarehouseId(Long warehouseId);

    /** Next value of the purchase-number sequence (numbers are DB-unique). */
    @Query(value = "SELECT nextval('pur_number_seq')", nativeQuery = true)
    long nextNumber();
}
