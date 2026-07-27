package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Channel lifecycle state. {@code allowsSending} is the operative flag. */
@Entity @Table(name = "comm_channel_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommChannelStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 16) private String color;
    @Column(name = "allows_sending", nullable = false) private boolean allowsSending;
    @Column(name = "is_test_mode", nullable = false) private boolean isTestMode;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
