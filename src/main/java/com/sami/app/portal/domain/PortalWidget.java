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

/** A dashboard widget; {@code providerKey} resolves a PortalDataProvider bean. */
@Entity @Table(name = "portal_widgets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalWidget extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "provider_key", nullable = false, length = 64) private String providerKey;
    @Column(name = "required_capability", length = 64) private String requiredCapability;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) @Builder.Default
    private Map<String, Object> config = new HashMap<>();
    @Column(nullable = false) private boolean enabled;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "tenant_id") private Long tenantId;
}
