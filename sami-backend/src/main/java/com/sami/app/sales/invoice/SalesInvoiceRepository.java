package com.sami.app.sales.invoice;

import java.math.BigDecimal;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice,Long> {
    @EntityGraph(attributePaths="lines") Optional<SalesInvoice> findByIdAndTenantId(Long id,Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="lines")
    @Query("select i from SalesInvoice i where i.id = :id and i.tenantId = :tenantId")
    Optional<SalesInvoice> findByIdAndTenantIdForUpdate(@Param("id") Long id,@Param("tenantId") Long tenantId);
    @EntityGraph(attributePaths="lines") List<SalesInvoice> findByTenantIdAndCompanyIdAndBranchIdOrderByCreatedAtDesc(Long tenantId,Long companyId,Long branchId);
    @Query("select coalesce(sum(l.quantity),0) from SalesInvoiceLine l where l.tenantId=?1 and l.deliveryLineId=?2 and l.invoice.status='ISSUED'") BigDecimal issuedQuantity(Long tenantId,Long deliveryLineId);
    @Query(value="select nextval('sales_invoice_number_seq')",nativeQuery=true) long nextNumber();
}
