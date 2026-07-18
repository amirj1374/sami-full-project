package com.sami.app.supplier.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An unlimited, configurable supplier tag (Preferred, Fast Delivery, …). */
@Entity
@Table(name = "sup_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupTag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 32)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
