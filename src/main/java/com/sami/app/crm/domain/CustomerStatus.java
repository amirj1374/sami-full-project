package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable customer status. Behavior lives in flags, never in names:
 * {@code isBlocking} is the flag transaction modules consult before doing
 * business with a customer (Blocked and Blacklisted carry it out of the box);
 * the default/archived/deleted structural flags mirror the user-status pattern.
 */
@Entity
@Table(name = "customer_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerStatus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_blocking", nullable = false)
    private boolean isBlocking;

    @Column(name = "hidden_by_default", nullable = false)
    private boolean hiddenByDefault;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_archived_state", nullable = false)
    private boolean isArchivedState;

    @Column(name = "is_deleted_state", nullable = false)
    private boolean isDeletedState;

    /** The (unique) status the blacklist operation transitions customers to. */
    @Column(name = "is_blacklist_state", nullable = false)
    private boolean isBlacklistState;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
