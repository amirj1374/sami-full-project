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

import java.util.Map;

/**
 * A delivery technology. {@code handlerKey} resolves to a
 * {@code CommProviderHandler} bean; {@code config} carries endpoints and
 * env-var NAMES — never secret values, which stay in the environment.
 */
@Entity @Table(name = "comm_providers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommProvider extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(name = "handler_key", nullable = false, length = 64) private String handlerKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private Map<String, Object> config;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
