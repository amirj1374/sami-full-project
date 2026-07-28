package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Reference to a stored file (V18) attached to a message. No bytes here. */
@Entity @Table(name = "comm_message_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommMessageAttachment extends BaseEntity {
    @Column(name = "message_id", nullable = false) private Long messageId;
    @Column(name = "file_uuid", nullable = false) private UUID fileUuid;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "content_type", length = 128) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
}
