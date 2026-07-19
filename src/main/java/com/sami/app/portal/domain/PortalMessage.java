package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/**
 * A message on a request or a standalone secure conversation.
 * {@code isInternal} keeps staff-only notes out of the customer's view; a
 * database CHECK stops a customer authoring one.
 */
@Entity @Table(name = "portal_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalMessage extends BaseEntity {
    @Column(name = "request_id") private Long requestId;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "parent_id") private Long parentId;
    /** CUSTOMER | STAFF | SYSTEM */
    @Column(name = "author_kind", nullable = false, length = 16) private String authorKind;
    @Column(name = "author_id") private Long authorId;
    @Column(name = "author_name", length = 160) private String authorName;
    @Column(length = 255) private String subject;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "is_internal", nullable = false) private boolean isInternal;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
