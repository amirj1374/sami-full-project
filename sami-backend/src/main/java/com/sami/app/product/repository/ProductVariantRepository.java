package com.sami.app.product.repository;

import com.sami.app.product.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByTenantIdAndProductIdOrderById(Long tenantId, Long productId);
    Optional<ProductVariant> findByTenantIdAndId(Long tenantId, Long id);
    boolean existsByTenantIdAndProductIdAndVariantCode(Long tenantId, Long productId, String code);
    boolean existsByTenantIdAndSku(Long tenantId, String sku);
}
