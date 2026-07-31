package com.sami.app.sales.repository;
import com.sami.app.sales.domain.Sale; import com.sami.app.sales.domain.SaleStatus; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface SaleRepository extends JpaRepository<Sale,Long>,JpaSpecificationExecutor<Sale> {
 @EntityGraph(attributePaths={"items","payments","services"}) Optional<Sale> findByIdAndTenantId(Long id,Long tenantId);
 Optional<Sale> findByTenantIdAndIdempotencyKey(Long tenantId,String key);
 Page<Sale> findByTenantId(Long tenantId,Pageable pageable);
 long countByTenantIdAndStatus(Long tenantId,SaleStatus status);
}
