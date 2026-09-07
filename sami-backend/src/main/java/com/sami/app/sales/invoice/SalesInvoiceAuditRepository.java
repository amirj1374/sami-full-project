package com.sami.app.sales.invoice;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoiceAuditRepository extends JpaRepository<SalesInvoiceAudit,Long> { List<SalesInvoiceAudit> findByInvoiceIdAndTenantIdOrderByOccurredAtDesc(Long invoiceId,Long tenantId); }
