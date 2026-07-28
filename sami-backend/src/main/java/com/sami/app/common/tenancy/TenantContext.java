package com.sami.app.common.tenancy;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.security.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

/**
 * Trusted tenant authority for the current HTTP request.
 *
 * <p>The context derives ownership exclusively from the authenticated
 * server-side principal. Tenant identifiers supplied by clients are never an
 * authority source. Tenant-scoped operations must call {@link #requireTenantId()}
 * and include that value in every repository lookup and mutation.
 */
@Component
@RequestScope
public class TenantContext {

    /**
     * Returns the tenant carried by the authenticated database-backed principal.
     * Anonymous, non-staff and incomplete principals have no tenant context.
     */
    public Optional<Long> currentTenantId() {
        return principal().map(SecurityUser::getTenantId).filter(id -> id != null);
    }

    /**
     * Resolves the trusted tenant or fails closed.
     *
     * @throws ApiException with 401 when no authenticated staff principal exists,
     *                      or 403 when that principal has no tenant ownership
     */
    public Long requireTenantId() {
        SecurityUser principal = principal()
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        if (principal.getTenantId() == null) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Authenticated account has no tenant context");
        }
        return principal.getTenantId();
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
