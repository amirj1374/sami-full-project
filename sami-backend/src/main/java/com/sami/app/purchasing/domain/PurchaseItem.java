package com.sami.app.purchasing.domain;

import com.sami.app.common.domain.BaseEntity;
import com.sami.app.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One purchase line. {@code receivedQuantity}/{@code returnedQuantity} are
 * running totals maintained by the receiving/return services — bounds are
 * enforced in the service AND by DB check constraints (no over-receiving, no
 * over-returning, ever). Tax is a future column, not a redesign.
 */
@Entity
@Table(name = "purchase_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String unit = "piece";

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(name = "requires_serial", nullable = false)
    private boolean requiresSerial;

    @Column(name = "requires_imei", nullable = false)
    private boolean requiresImei;

    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "returned_quantity", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal returnedQuantity = BigDecimal.ZERO;

    public BigDecimal lineTotal() {
        return quantity.multiply(unitPrice).subtract(discount);
    }

    public BigDecimal remainingQuantity() {
        return quantity.subtract(receivedQuantity);
    }
}
