package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Physical, virtual or smart folder.
 *
 * <p>A smart folder stores a {@code smartQuery} (the same filter shape the search
 * API accepts) and is resolved on read, so classification rules change without
 * moving a single file.
 */
@Entity
@Table(name = "file_folders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileFolder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileFolder parent;

    @Column(nullable = false, length = 160)
    private String name;

    /** Materialised path, unique per tenant — makes subtree queries a prefix match. */
    @Column(nullable = false, length = 2000)
    private String path;

    @Column(length = 500)
    private String description;

    @Column(name = "is_virtual", nullable = false)
    private boolean isVirtual;

    @Column(name = "is_smart", nullable = false)
    private boolean isSmart;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "smart_query")
    @Builder.Default
    private Map<String, Object> smartQuery = new HashMap<>();

    @Column(name = "module_code", length = 64)
    private String moduleCode;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
