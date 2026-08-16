package com.sami.app.user.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_experience_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserExperiencePreference extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mobile_navigation_codes", nullable = false)
    @Builder.Default
    private List<String> mobileNavigationCodes = new ArrayList<>();

    @Column(name = "demo_notifications_enabled", nullable = false)
    private boolean demoNotificationsEnabled;

    @Column(name = "keyboard_shortcuts_enabled", nullable = false)
    @Builder.Default
    private boolean keyboardShortcutsEnabled = true;
}
