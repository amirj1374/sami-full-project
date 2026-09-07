package com.sami.app.sales.delivery;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery,Long> {
    @EntityGraph(attributePaths="lines") Optional<Delivery> findByIdAndTenantId(Long id,Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="lines")
    @Query("select d from Delivery d where d.id = :id and d.tenantId = :tenantId")
    Optional<Delivery> findByIdAndTenantIdForUpdate(@Param("id") Long id,@Param("tenantId") Long tenantId);
    @EntityGraph(attributePaths="lines") List<Delivery> findByTenantIdAndCompanyIdAndBranchIdOrderByCreatedAtDesc(Long tenantId,Long companyId,Long branchId);
    @EntityGraph(attributePaths="lines") List<Delivery> findByOrderIdAndTenantIdAndStatus(Long orderId,Long tenantId,DeliveryStatus status);
    @Query("select distinct d from Delivery d join fetch d.lines l where l.id=?1 and d.tenantId=?2") Optional<Delivery> findByDeliveryLineIdAndTenantId(Long deliveryLineId,Long tenantId);
    @Query("select coalesce(sum(l.deliveryQuantity),0) from DeliveryLine l where l.tenantId=?1 and l.orderLine.id=?2 and l.delivery.status='CONFIRMED'") java.math.BigDecimal deliveredQuantity(Long tenantId,Long orderLineId);
    @Query(value="select nextval('sales_delivery_number_seq')",nativeQuery=true) long nextNumber();
}
