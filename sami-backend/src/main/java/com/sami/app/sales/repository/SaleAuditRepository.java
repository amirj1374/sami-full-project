package com.sami.app.sales.repository;
import com.sami.app.sales.domain.SaleAuditHistory; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SaleAuditRepository extends JpaRepository<SaleAuditHistory,Long>{ List<SaleAuditHistory> findBySaleIdAndTenantIdOrderByOccurredAtDesc(Long saleId,Long tenantId); }
