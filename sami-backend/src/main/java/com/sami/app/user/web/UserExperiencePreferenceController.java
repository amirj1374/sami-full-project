package com.sami.app.user.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.dto.UserExperiencePreferenceRequest;
import com.sami.app.user.dto.UserExperiencePreferenceResponse;
import com.sami.app.user.service.UserExperiencePreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/preferences")
@RequiredArgsConstructor
public class UserExperiencePreferenceController {

    private final UserExperiencePreferenceService service;

    @GetMapping
    @Operation(summary = "Get the current user's application preferences")
    public ApiResponse<UserExperiencePreferenceResponse> get(@AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(service.get(principal.getId()));
    }

    @PutMapping
    @Operation(summary = "Replace the current user's application preferences")
    public ApiResponse<UserExperiencePreferenceResponse> update(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody UserExperiencePreferenceRequest request) {
        return ApiResponse.ok(service.update(principal.getId(), request));
    }
}
