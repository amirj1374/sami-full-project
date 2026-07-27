package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * A message template with declared {@code {{variables}}}.
 *
 * <p>Editing creates a NEW revision row and deactivates the old one, so a
 * sent message's {@code template_id} forever points at the exact text that
 * was rendered — the same reasoning as article versions in V20.
 */
@Entity @Table(name = "comm_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommTemplate extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_type_id")
    private CommChannelType channelType;

    @Column(name = "module_code", length = 64) private String moduleCode;
    @Column(nullable = false, length = 16) @Builder.Default private String language = "fa";
    @Column(name = "subject_template", length = 500) private String subjectTemplate;
    @Column(name = "body_template", nullable = false, columnDefinition = "text") private String bodyTemplate;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<String> variables;
    @Column(nullable = false) @Builder.Default private int revision = 1;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
