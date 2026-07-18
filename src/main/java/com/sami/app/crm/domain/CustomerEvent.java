package com.sami.app.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * One entry in a customer's 360° timeline. Append-only and immutable.
 *
 * <p>{@code eventType} is an OPEN string set: the CRM records its own types
 * (CREATED, UPDATED, CONTACT_CHANGED, STATUS_CHANGED, …) and future modules
 * append theirs (PURCHASE, REPAIR_REQUEST, INSTALLMENT, PAYMENT, COMPLAINT, …)
 * through {@code CustomerEventService} with their {@code sourceModule} — no CRM
 * change needed. This table IS the integration surface.
 */
@Entity
@Table(name = "customer_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail")
    private Map<String, Object> detail;

    @Column(name = "source_module", nullable = false, length = 64)
    @Builder.Default
    private String sourceModule = "crm";

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
