package com.sami.app.product.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Sample business entity used to demonstrate the full CRUD + filtering stack.
 *
 * <p>Kept intentionally generic (name/sku/price/stock/active) so it can be replaced
 * or extended for the real domain. {@code sku} is unique and a natural filter key.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(length = 2000)
    private String description;

    /** Stored with fixed precision to avoid floating-point money errors. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(nullable = false)
    private boolean active;
}
