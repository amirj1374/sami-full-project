package com.sami.app.sales.delivery;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface DeliveryRepository extends JpaRepository<Delivery,Long> {
    @EntityGraph(attributePaths="lines") Optional<Delivery> findByIdAndTenantId(Long id,Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="lines") Optional<Delivery> findByIdAndTenantIdForUpdate(Long id,Long tenantId);
    @EntityGraph(attributePaths="lines") List<Delivery> findByTenantIdAndCompanyIdAndBranchIdOrderByCreatedAtDesc(Long tenantId,Long companyId,Long branchId);
    @Query("select coalesce(sum(l.deliveryQuantity),0) from DeliveryLine l where l.tenantId=?1 and l.orderLine.id=?2 and l.delivery.status='CONFIRMED'") java.math.BigDecimal deliveredQuantity(Long tenantId,Long orderLineId);
    @Query(value="select nextval('sales_delivery_number_seq')",nativeQuery=true) long nextNumber();
}
