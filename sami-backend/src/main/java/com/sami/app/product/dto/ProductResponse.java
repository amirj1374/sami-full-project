package com.sami.app.product.dto;

import com.sami.app.product.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

/** Public representation of a {@link Product}. */
public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        boolean hamtaEligible,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive(),
                product.isHamtaEligible(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
