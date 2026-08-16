package com.sami.app.user.dto;

import com.sami.app.user.domain.UserExperiencePreference;

import java.util.List;

public record UserExperiencePreferenceResponse(
        List<String> mobileNavigationCodes,
        boolean demoNotificationsEnabled,
        boolean keyboardShortcutsEnabled,
        boolean mobileNavigationConfigured,
        long version
) {
    public static UserExperiencePreferenceResponse defaults() {
        return new UserExperiencePreferenceResponse(List.of(), false, true, false, 0);
    }

    public static UserExperiencePreferenceResponse from(UserExperiencePreference preference) {
        return new UserExperiencePreferenceResponse(
                List.copyOf(preference.getMobileNavigationCodes()),
                preference.isDemoNotificationsEnabled(),
                preference.isKeyboardShortcutsEnabled(),
                true,
                preference.getVersion());
    }
}
