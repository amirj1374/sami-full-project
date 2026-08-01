package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A dynamic, unlimited customer tag (VIP, Wholesale, High Risk, …). */
@Entity
@Table(name = "customer_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTag extends BaseEntity {

    @Column(name = "tenant_id", updatable = false)
    private Long tenantId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 32)
    private String color;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
