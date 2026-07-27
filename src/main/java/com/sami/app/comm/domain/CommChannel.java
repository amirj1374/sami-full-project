package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A concrete route: a channel type served by a provider under a policy,
 * scoped to an organisation. Two SMS channels with different providers and
 * priorities are what make provider fallback a data question.
 */
@Entity @Table(name = "comm_channels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommChannel extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_type_id", nullable = false)
    private CommChannelType channelType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private CommProvider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CommChannelStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_policy_id")
    private CommDeliveryPolicy deliveryPolicy;

    @Column(name = "company_id") private Long companyId;
    @Column(name = "branch_id") private Long branchId;
    @Column(nullable = false) private int priority;
    @Column(name = "sender_address", length = 255) private String senderAddress;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean canSend() {
        return status != null && status.isAllowsSending()
                && provider != null && provider.isActive();
    }
}
