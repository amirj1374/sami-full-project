package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

/**
 * An explicit grant of a managed file to a customer.
 *
 * <p>This is the ONLY route by which a portal account may read a file. The
 * portal never infers access from a file's owning module or contents, so a
 * customer cannot reach a document simply because it mentions them.
 */
@Entity @Table(name = "portal_document_grants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalDocumentGrant extends BaseEntity {
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "file_uuid", nullable = false) private UUID fileUuid;
    @Column(length = 255) private String title;
    @Column(name = "source_module", length = 64) private String sourceModule;
    @Column(name = "source_entity", length = 64) private String sourceEntity;
    @Column(name = "source_record_id") private Long sourceRecordId;
    @Column(name = "granted_by") private Long grantedBy;
    @Column(name = "granted_at", nullable = false) @Builder.Default private Instant grantedAt = Instant.now();
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "download_count", nullable = false) @Builder.Default private int downloadCount = 0;
    @Column(name = "last_downloaded_at") private Instant lastDownloadedAt;
    @Column(name = "requires_signature", nullable = false) private boolean requiresSignature;
    @Column(name = "signed_at") private Instant signedAt;
    @Column(name = "signature_ref", length = 255) private String signatureRef;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isActive() {
        return revokedAt == null;
    }
}
