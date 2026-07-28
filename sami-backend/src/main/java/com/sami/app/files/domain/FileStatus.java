package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configurable file lifecycle state. Behaviour is read from FLAGS, never from
 * the code — {@code allowsDownload} is what stops a quarantined file being
 * served, so adding a new state never touches a service.
 */
@Entity
@Table(name = "file_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileStatus extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_available_state", nullable = false)
    private boolean isAvailableState;

    @Column(name = "is_processing_state", nullable = false)
    private boolean isProcessingState;

    @Column(name = "is_locked_state", nullable = false)
    private boolean isLockedState;

    @Column(name = "is_archived_state", nullable = false)
    private boolean isArchivedState;

    @Column(name = "is_deleted_state", nullable = false)
    private boolean isDeletedState;

    @Column(name = "is_quarantined_state", nullable = false)
    private boolean isQuarantinedState;

    @Column(name = "allows_download", nullable = false)
    private boolean allowsDownload;

    @Column(name = "allows_new_version", nullable = false)
    private boolean allowsNewVersion;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "tenant_id")
    private Long tenantId;
}
