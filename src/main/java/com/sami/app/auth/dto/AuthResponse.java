package com.sami.app.auth.dto;

import com.sami.app.user.dto.CurrentUserResponse;

/**
 * Result of a successful authentication.
 *
 * @param tokenType   always "Bearer"
 * @param accessToken short-lived token sent on every request
 * @param refreshToken long-lived token used to obtain new access tokens
 * @param expiresIn   access-token lifetime in seconds
 * @param user        the authenticated user's profile including permissions
 */
public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        CurrentUserResponse user
) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn,
                                  CurrentUserResponse user) {
        return new AuthResponse("Bearer", accessToken, refreshToken, expiresIn, user);
    }
}
