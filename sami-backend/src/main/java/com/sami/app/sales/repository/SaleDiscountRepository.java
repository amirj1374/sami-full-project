package com.sami.app.sales.repository;
import com.sami.app.sales.domain.SaleDiscount; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SaleDiscountRepository extends JpaRepository<SaleDiscount,Long>{ List<SaleDiscount> findBySaleIdAndTenantIdOrderByRequestedAtAsc(Long saleId,Long tenantId); Optional<SaleDiscount> findByIdAndTenantId(Long id,Long tenantId); boolean existsBySaleIdAndTenantIdAndStatus(Long saleId,Long tenantId,String status); }
