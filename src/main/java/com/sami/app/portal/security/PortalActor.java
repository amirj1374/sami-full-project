package com.sami.app.portal.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resolves the current {@link PortalPrincipal}, mirroring {@code CurrentActor}
 * for the staff realm.
 *
 * <p>{@link #requireCustomerId()} is the single point through which a customer's
 * identity enters a query. Business data is never scoped by a customer id taken
 * from a path or query parameter.
 */
public final class PortalActor {

    private PortalActor() {
    }

    public static Optional<PortalPrincipal> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PortalPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Long accountId() {
        return get().map(PortalPrincipal::accountId).orElse(null);
    }

    public static Long requireCustomerId() {
        return get().map(PortalPrincipal::customerId)
                .orElseThrow(() -> new IllegalStateException("No authenticated portal customer"));
    }
}
