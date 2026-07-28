package com.sami.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resolves the currently authenticated {@link SecurityUser} from the security
 * context. Empty for anonymous or system-initiated work (schedulers, bootstrap).
 */
public final class CurrentActor {

    private CurrentActor() {
    }

    public static Optional<SecurityUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Long id() {
        return get().map(SecurityUser::getId).orElse(null);
    }

    public static String email() {
        return get().map(SecurityUser::getUsername).orElse(null);
    }
}
