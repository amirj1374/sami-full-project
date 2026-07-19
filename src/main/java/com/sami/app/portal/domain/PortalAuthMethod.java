package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.HashMap;
import java.util.Map;

/** Configurable authentication method; {@code handlerKey} resolves the bean. */
@Entity @Table(name = "portal_auth_methods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAuthMethod extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "handler_key", nullable = false, length = 64) private String handlerKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) @Builder.Default
    private Map<String, Object> config = new HashMap<>();
    @Column(nullable = false) private boolean enabled;
    @Column(name = "is_primary_method", nullable = false) private boolean isPrimaryMethod;
    /** Needs an OtpDeliveryChannel; refused at runtime when none is registered. */
    @Column(name = "requires_delivery", nullable = false) private boolean requiresDelivery;
    @Column(name = "is_second_factor", nullable = false) private boolean isSecondFactor;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
