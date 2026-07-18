package com.sami.app.user.dto;

import com.sami.app.user.domain.UserStatus;

/** Public representation of a {@link UserStatus}. */
public record UserStatusResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean allowsLogin,
        boolean hiddenByDefault,
        boolean isDefault,
        boolean isArchivedState,
        boolean isDeletedState,
        boolean isSystem,
        int displayOrder
) {

    public static UserStatusResponse from(UserStatus status) {
        return new UserStatusResponse(
                status.getId(),
                status.getCode(),
                status.getName(),
                status.getDescription(),
                status.isAllowsLogin(),
                status.isHiddenByDefault(),
                status.isDefault(),
                status.isArchivedState(),
                status.isDeletedState(),
                status.isSystem(),
                status.getDisplayOrder()
        );
    }
}
