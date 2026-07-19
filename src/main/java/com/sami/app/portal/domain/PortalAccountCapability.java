package com.sami.app.portal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/** Grant of a capability to a portal account. */
@Entity
@Table(name = "portal_account_capabilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAccountCapability {

    @EmbeddedId private Key id;

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @EqualsAndHashCode
    public static class Key implements Serializable {
        @Column(name = "account_id", nullable = false) private Long accountId;
        @Column(name = "capability_id", nullable = false) private Long capabilityId;
    }
}
