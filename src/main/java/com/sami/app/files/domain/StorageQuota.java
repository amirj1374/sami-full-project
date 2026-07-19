package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A storage ceiling for a configurable scope. {@code scopeRef} is the id of the
 * company/branch or the module code, depending on {@code scopeKind}; null means
 * the whole tenant.
 */
@Entity
@Table(name = "storage_quotas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StorageQuota extends BaseEntity {

    /** TENANT | COMPANY | BRANCH | MODULE | PROVIDER */
    @Column(name = "scope_kind", nullable = false, length = 32)
    private String scopeKind;

    @Column(name = "scope_ref", length = 128)
    private String scopeRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private StorageProviderConfig provider;

    @Column(name = "max_bytes")
    private Long maxBytes;

    @Column(name = "max_files")
    private Long maxFiles;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
