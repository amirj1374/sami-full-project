package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/** An attachment referencing the files module by UUID. No bytes here. */
@Entity @Table(name = "portal_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAttachment extends BaseEntity {
    @Column(name = "request_id") private Long requestId;
    @Column(name = "message_id") private Long messageId;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "file_uuid", nullable = false) private UUID fileUuid;
    @Column(length = 255) private String caption;
    @Column(name = "uploaded_by_kind", nullable = false, length = 16) @Builder.Default
    private String uploadedByKind = "CUSTOMER";
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
