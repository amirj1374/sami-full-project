package com.sami.app.supplier.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Which supplier identifiers must be unique — policy as data, toggleable. */
@Entity
@Table(name = "sup_duplicate_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupDuplicateRule extends BaseEntity {

    public enum Identifier { TAX_NUMBER, NATIONAL_ID, ECONOMIC_CODE, REGISTRATION_NUMBER }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private Identifier identifier;

    @Column(nullable = false)
    private boolean enabled;
}
