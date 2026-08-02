package com.sami.app.user.repository;

import com.sami.app.user.domain.UserExperiencePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserExperiencePreferenceRepository extends JpaRepository<UserExperiencePreference, Long> {
    Optional<UserExperiencePreference> findByTenantIdAndUserId(Long tenantId, Long userId);
    boolean existsByTenantIdAndUserIdAndDemoNotificationsEnabledTrue(Long tenantId, Long userId);
}
