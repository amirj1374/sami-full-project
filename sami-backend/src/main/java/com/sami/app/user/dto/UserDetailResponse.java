package com.sami.app.user.dto;

import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserProfile;

/** A user together with their full profile — the admin detail/edit payload. */
public record UserDetailResponse(
        UserResponse user,
        UserProfileResponse profile
) {

    public static UserDetailResponse from(User user) {
        UserProfile profile = user.getProfile();
        return new UserDetailResponse(
                UserResponse.from(user),
                profile != null ? UserProfileResponse.from(profile) : null
        );
    }
}
