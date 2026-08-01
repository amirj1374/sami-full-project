package com.sami.app.sales.repository;

import com.sami.app.sales.domain.LostSale;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostSaleRepository extends JpaRepository<LostSale, Long> {
    Page<LostSale> findByTenantIdOrderByOccurredAtDesc(Long tenantId, Pageable pageable);
}
