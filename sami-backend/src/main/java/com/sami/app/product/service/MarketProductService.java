package com.sami.app.product.service;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.product.domain.Product;
import com.sami.app.product.publicapi.MarketProductOperations;
import com.sami.app.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MarketProductService implements MarketProductOperations {
    private final ProductRepository products;
    private final TenantContext tenants;

    @Override @Transactional(propagation = Propagation.MANDATORY)
    public ProductState upsertSim(String code, BigDecimal price) {
        Long tenantId = tenants.requireTenantId(); String sku = "SIM-" + code;
        Product product = products.findByTenantIdAndSku(tenantId, sku).orElse(null);
        boolean created = product == null;
        if (created) product = Product.builder().tenantId(tenantId).sku(sku).name("SIM " + code)
                .description("Market-synchronized SIM card").price(price).stockQuantity(0).active(true).hamtaEligible(false).build();
        else { product.setPrice(price); product.setActive(true); }
        return new ProductState(products.saveAndFlush(product).getId(), created);
    }
}
