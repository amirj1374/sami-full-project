package com.sami.app.purchasing.repository;

import com.sami.app.purchasing.domain.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/** Data-access for {@link Purchase}. */
public interface PurchaseRepository
        extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @EntityGraph(attributePaths = {"type", "status", "supplier", "sellerCustomer", "warehouse",
            "items", "items.product", "cancelReason"})
    Optional<Purchase> findWithDetailsByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"type", "status", "supplier", "sellerCustomer", "warehouse",
            "items", "items.product", "cancelReason"})
    @Query("select p from Purchase p where p.id = :id and p.tenantId = :tenantId")
    Optional<Purchase> findForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Override
    @EntityGraph(attributePaths = {"type", "status", "supplier", "sellerCustomer", "warehouse"})
    Page<Purchase> findAll(Specification<Purchase> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"type", "status", "supplier", "sellerCustomer", "warehouse"})
    List<Purchase> findAll(Specification<Purchase> spec, Sort sort);

    long countByTenantIdAndStatusId(Long tenantId, Long statusId);

    long countByTenantIdAndTypeId(Long tenantId, Long typeId);

    long countByTenantIdAndWarehouseId(Long tenantId, Long warehouseId);

    boolean existsByTenantIdAndImportKey(Long tenantId, String importKey);

    /** Next value of the purchase-number sequence (numbers are DB-unique). */
    @Query(value = "SELECT nextval('pur_number_seq')", nativeQuery = true)
    long nextNumber();
}
