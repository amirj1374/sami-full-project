package com.sami.app.user.dto;

/**
 * Optional query filters for the admin user listing. Bound from request query
 * params; every field is nullable and contributes a predicate only when present.
 *
 * <p>{@code search} is the global search: it matches email, full name, first/last
 * name, phone number, employee code or national code, case-insensitive. Statuses
 * flagged {@code hiddenByDefault} (archived / deleted) are excluded unless
 * {@code includeHidden} is true or {@code statusId} targets one explicitly.
 */
public record UserFilter(
        String search,
        String email,
        String phone,
        String employeeCode,
        String nationalCode,
        Long roleId,
        Long statusId,
        Boolean includeHidden
) {
}
