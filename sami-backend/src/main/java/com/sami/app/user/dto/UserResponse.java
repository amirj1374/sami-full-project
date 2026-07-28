package com.sami.app.user.dto;

import com.sami.app.authz.dto.RoleSummaryResponse;
import com.sami.app.user.domain.User;

import java.time.Instant;

/**
 * Public representation of a {@link User}. Never exposes the password hash.
 *
 * <p>{@code version} backs optimistic-locking checks: clients send it back on
 * update and concurrent modifications fail with a conflict instead of silently
 * overwriting each other.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        RoleSummaryResponse role,
        UserStatusResponse status,
        Instant archivedAt,
        Long archivedBy,
        Instant deletedAt,
        Long deletedBy
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVersion(),
                RoleSummaryResponse.from(user.getRole()),
                UserStatusResponse.from(user.getStatus()),
                user.getArchivedAt(),
                user.getArchivedBy(),
                user.getDeletedAt(),
                user.getDeletedBy()
        );
    }
}
