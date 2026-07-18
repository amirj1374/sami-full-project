package com.sami.app.product.dto;

import java.math.BigDecimal;

/**
 * Optional query filters for listing products. Bound from request query params;
 * every field is nullable and contributes a predicate only when present.
 */
public record ProductFilter(
        String name,
        Boolean active,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
