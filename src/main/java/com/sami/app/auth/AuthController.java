package com.sami.app.auth;

import com.sami.app.auth.dto.AuthResponse;
import com.sami.app.auth.dto.ChangePasswordRequest;
import com.sami.app.auth.dto.ForgotPasswordRequest;
import com.sami.app.auth.dto.LoginRequest;
import com.sami.app.auth.dto.LogoutRequest;
import com.sami.app.auth.dto.RefreshRequest;
import com.sami.app.auth.dto.RegisterRequest;
import com.sami.app.auth.dto.ResetPasswordRequest;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 *
 * <p>{@code /api/v1/auth/**} is permitAll at the URL level, so the one endpoint
 * that does need a logged-in caller (change-password) enforces it with method
 * security instead.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive tokens")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair (rotates the refresh token)")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change the current user's password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal SecurityUser principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ApiResponse.ok();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password-reset email")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // Always 200, whether or not the email exists (no account enumeration).
        authService.forgotPassword(request);
        return ApiResponse.ok();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset the password with an emailed token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }
}
