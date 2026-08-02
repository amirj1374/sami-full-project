package com.sami.app.user.service;

import com.sami.app.authz.service.MenuService;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.user.domain.UserExperiencePreference;
import com.sami.app.user.dto.UserExperiencePreferenceRequest;
import com.sami.app.user.dto.UserExperiencePreferenceResponse;
import com.sami.app.user.repository.UserExperiencePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserExperiencePreferenceService {

    private final UserExperiencePreferenceRepository repository;
    private final MenuService menuService;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public UserExperiencePreferenceResponse get(Long userId) {
        Long tenantId = tenantContext.requireTenantId();
        return repository.findByTenantIdAndUserId(tenantId, userId)
                .map(UserExperiencePreferenceResponse::from)
                .orElseGet(UserExperiencePreferenceResponse::defaults);
    }

    @Transactional
    public UserExperiencePreferenceResponse update(Long userId, UserExperiencePreferenceRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        LinkedHashSet<String> unique = new LinkedHashSet<>(request.mobileNavigationCodes());
        if (unique.size() != request.mobileNavigationCodes().size()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Mobile navigation items must be unique");
        }
        Set<String> permitted = new HashSet<>(menuService.menuForCurrentUser().stream()
                .filter(item -> item.navigable() && !item.showPlaceholder())
                .map(item -> item.code())
                .toList());
        if (!permitted.containsAll(unique)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Mobile navigation contains an unavailable or unauthorized module");
        }

        UserExperiencePreference preference = repository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> UserExperiencePreference.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .build());
        preference.setMobileNavigationCodes(unique.stream().toList());
        preference.setDemoNotificationsEnabled(request.demoNotificationsEnabled());
        return UserExperiencePreferenceResponse.from(repository.save(preference));
    }
}
