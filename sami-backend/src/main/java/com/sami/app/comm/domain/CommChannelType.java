package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A kind of communication (email, SMS, WhatsApp…). Behavioural flags drive
 * recipient validation and capability checks; no service tests channel names.
 */
@Entity @Table(name = "comm_channel_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommChannelType extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(length = 64) private String icon;
    @Column(name = "requires_phone", nullable = false) private boolean requiresPhone;
    @Column(name = "requires_email", nullable = false) private boolean requiresEmail;
    @Column(name = "requires_device_token", nullable = false) private boolean requiresDeviceToken;
    @Column(name = "supports_subject", nullable = false) private boolean supportsSubject;
    @Column(name = "supports_attachments", nullable = false) private boolean supportsAttachments;
    @Column(name = "supports_conversations", nullable = false) private boolean supportsConversations;
    @Column(name = "supports_read_receipt", nullable = false) private boolean supportsReadReceipt;
    /** 0 = unlimited. */
    @Column(name = "max_body_length", nullable = false) private int maxBodyLength;
    @Column(name = "max_attachment_bytes", nullable = false) private long maxAttachmentBytes;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
