package com.sami.app.common.tenancy;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.security.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Trusted tenant authority for the current request or server-owned background execution.
 *
 * <p>The context derives ownership exclusively from the authenticated
 * server-side principal. Background work may establish the persisted tenant
 * carried by a scheduled job. Tenant identifiers supplied by clients are never
 * an authority source. Tenant-scoped operations must call
 * {@link #requireTenantId()} and include that value in every repository lookup
 * and mutation.
 */
@Component
public class TenantContext {

    private static final ThreadLocal<Long> BACKGROUND_TENANT = new ThreadLocal<>();

    /**
     * Returns the tenant carried by the authenticated database-backed principal.
     * Anonymous, non-staff and incomplete principals have no tenant context.
     */
    public Optional<Long> currentTenantId() {
        Optional<Long> authenticated = principal().map(SecurityUser::getTenantId).filter(id -> id != null);
        return authenticated.isPresent() ? authenticated : Optional.ofNullable(BACKGROUND_TENANT.get());
    }

    /**
     * Resolves the trusted tenant or fails closed.
     *
     * @throws ApiException with 401 when no authenticated staff principal exists,
     *                      or 403 when that principal has no tenant ownership
     */
    public Long requireTenantId() {
        Optional<SecurityUser> principal = principal();
        if (principal.isPresent() && principal.get().getTenantId() == null) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Authenticated account has no tenant context");
        }
        return currentTenantId().orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
    }

    /**
     * Requires an entity or command to belong to the current trusted tenant.
     * Platform roles do not bypass this check implicitly.
     */
    public void requireAccessTo(Long tenantId) {
        Long current = requireTenantId();
        if (tenantId == null || !current.equals(tenantId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Cross-tenant access is not permitted");
        }
    }

    /**
     * Indicates eligibility for a separately permissioned platform operation.
     * Callers must still enforce that dedicated permission and choose their
     * cross-tenant scope explicitly.
     */
    public boolean isPlatformActor() {
        return principal().map(SecurityUser::isPlatformActor).orElse(false);
    }

    /**
     * Runs server-owned background work in an explicit trusted tenant scope.
     * Callers must obtain the tenant from persisted server state such as a
     * scheduled job; request values must never be passed here.
     */
    public <T> T callAsTenant(Long tenantId, Supplier<T> operation) {
        if (tenantId == null) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Background operation has no tenant context");
        }
        Optional<SecurityUser> actor = principal();
        if (actor.isPresent() && (actor.get().getTenantId() == null
                || !tenantId.equals(actor.get().getTenantId()))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Cross-tenant context override is not permitted");
        }
        Long previous = BACKGROUND_TENANT.get();
        if (previous != null && !previous.equals(tenantId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Nested cross-tenant context is not permitted");
        }
        BACKGROUND_TENANT.set(tenantId);
        try {
            return operation.get();
        } finally {
            if (previous == null) {
                BACKGROUND_TENANT.remove();
            } else {
                BACKGROUND_TENANT.set(previous);
            }
        }
    }

    private Optional<SecurityUser> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
