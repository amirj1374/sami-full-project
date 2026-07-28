package com.sami.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The single place authorization decisions are made.
 *
 * <p>Referenced from method security as {@code @PreAuthorize("@authz.has('users:view')")}.
 * A principal passes a check when its role carries the super-admin bypass flag or
 * its authorities (the role's permission codes) contain the requested code.
 * Anonymous or non-{@link SecurityUser} principals always fail.
 */
@Component("authz")
public class Authz {

    /** @return true if the current principal may perform the given permission code. */
    public boolean has(String code) {
        SecurityUser principal = currentUser();
        if (principal == null) {
            return false;
        }
        if (principal.isSuperAdmin()) {
            return true;
        }
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(code::equals);
    }

    /** @return true if the current principal may perform any of the given codes. */
    public boolean hasAny(String... codes) {
        SecurityUser principal = currentUser();
        if (principal == null) {
            return false;
        }
        if (principal.isSuperAdmin()) {
            return true;
        }
        for (String code : codes) {
            if (principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(code::equals)) {
                return true;
            }
        }
        return false;
    }

    private SecurityUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser principal)) {
            return null;
        }
        return principal;
    }
}
