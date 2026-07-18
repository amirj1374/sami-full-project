package com.sami.app.crm.domain;

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

/**
 * Duplicate-detection policy as data: which identifiers participate in the
 * check is toggled here at runtime, not compiled in.
 */
@Entity
@Table(name = "crm_duplicate_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicateRule extends BaseEntity {

    public enum Identifier { PHONE, EMAIL, NATIONAL_CODE, PASSPORT }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private Identifier identifier;

    @Column(nullable = false)
    private boolean enabled;
}
