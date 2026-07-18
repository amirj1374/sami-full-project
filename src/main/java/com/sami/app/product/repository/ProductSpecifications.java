package com.sami.app.product.repository;

import com.sami.app.product.domain.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Reusable, composable query fragments for {@link Product} filtering.
 *
 * <p>Each method returns a {@link Specification} that is null-safe (a null argument
 * contributes no predicate), so callers can chain only the filters that were
 * actually supplied.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Case-insensitive substring match on name. */
    public static Specification<Product> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Product> active(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
