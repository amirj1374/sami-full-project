package com.sami.app.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserExperiencePreferenceRequest(
        @NotNull @Size(max = 4)
        List<@Pattern(regexp = "[a-z0-9][a-z0-9-]{0,63}") String> mobileNavigationCodes,
        boolean demoNotificationsEnabled
) {
}
