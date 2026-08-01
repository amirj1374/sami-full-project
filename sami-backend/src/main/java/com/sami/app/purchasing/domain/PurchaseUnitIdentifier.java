package com.sami.app.purchasing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One identifier of one serialized received unit (IMEI, serial, …).
 * {@code (identifierType, value)} is DB-unique: a duplicate serial can never
 * enter the system, even under concurrent receiving.
 */
@Entity
@Table(name = "purchase_unit_identifiers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseUnitIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_item_id", nullable = false)
    private PurchaseReceiptItem receiptItem;

    /** 1-based position of the unit within its receipt line. */
    @Column(name = "unit_index", nullable = false)
    private int unitIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identifier_type_id", nullable = false)
    private PurIdentifierType identifierType;

    @Column(nullable = false, length = 128)
    private String value;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
