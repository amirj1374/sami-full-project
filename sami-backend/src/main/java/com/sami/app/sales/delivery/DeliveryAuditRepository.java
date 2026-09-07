package com.sami.app.sales.delivery;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAuditRepository extends JpaRepository<DeliveryAudit,Long> { List<DeliveryAudit> findByDeliveryIdAndTenantIdOrderByOccurredAtDesc(Long deliveryId,Long tenantId); }
