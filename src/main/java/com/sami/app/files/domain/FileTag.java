package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable tag. {@code isConfidential} feeds role-based access decisions. */
@Entity
@Table(name = "file_tags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileTag extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 24)
    private String color;

    @Column(name = "is_confidential", nullable = false)
    private boolean isConfidential;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "tenant_id")
    private Long tenantId;
}
