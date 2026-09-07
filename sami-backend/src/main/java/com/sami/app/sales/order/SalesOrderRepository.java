package com.sami.app.sales.order;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface SalesOrderRepository extends JpaRepository<SalesOrder,Long> {
    @EntityGraph(attributePaths="lines") Optional<SalesOrder> findByIdAndTenantId(Long id, Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="lines") Optional<SalesOrder> findByIdAndTenantIdForUpdate(Long id, Long tenantId);
    @EntityGraph(attributePaths="lines") List<SalesOrder> findByTenantIdAndCompanyIdAndBranchIdOrderByCreatedAtDesc(Long tenantId, Long companyId, Long branchId);
    @Query(value="select nextval('sales_order_number_seq')", nativeQuery=true) long nextNumber();
}
