package com.sami.app.sales.order;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderAuditRepository extends JpaRepository<SalesOrderAudit,Long> {
    List<SalesOrderAudit> findByOrderIdAndTenantIdOrderByOccurredAtDesc(Long orderId, Long tenantId);
}
