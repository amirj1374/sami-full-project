package com.sami.app.sales.order;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder,Long> {
    @EntityGraph(attributePaths="lines") Optional<SalesOrder> findByIdAndTenantId(Long id, Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="lines")
    @Query("select o from SalesOrder o where o.id = :id and o.tenantId = :tenantId")
    Optional<SalesOrder> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);
    @EntityGraph(attributePaths="lines") List<SalesOrder> findByTenantIdAndCompanyIdAndBranchIdOrderByCreatedAtDesc(Long tenantId, Long companyId, Long branchId);
    @Query(value="select nextval('sales_order_number_seq')", nativeQuery=true) long nextNumber();
}
