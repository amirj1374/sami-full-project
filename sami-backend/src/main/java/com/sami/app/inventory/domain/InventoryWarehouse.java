package com.sami.app.inventory.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Canonical stock-holding warehouse. The existing {@code pur_warehouses}
 * table is retained so historical purchase references remain valid.
 */
@Entity
@Table(name = "pur_warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryWarehouse extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "warehouse_type", nullable = false, length = 32)
    @Builder.Default
    private String warehouseType = "STANDARD";

    @Column(name = "allows_negative_stock", nullable = false)
    private boolean allowsNegativeStock;

    @Column(name = "is_default", nullable = false)
    private boolean defaultWarehouse;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
