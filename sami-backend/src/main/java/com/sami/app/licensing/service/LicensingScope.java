package com.sami.app.licensing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Licensing-specific use of the shared trusted tenant context. Cross-tenant
 * administration is explicit and is available only to an existing platform
 * actor; controller permissions remain mandatory in addition to this check.
 */
@Component("licensingScope")
@RequiredArgsConstructor
public class LicensingScope {

    private final TenantContext tenantContext;

    public Long currentTenantId() {
        return tenantContext.requireTenantId();
    }

    public boolean platform() {
        return tenantContext.currentTenantId().isPresent() && tenantContext.isPlatformActor();
    }

    public void requirePlatform() {
        tenantContext.requireTenantId();
        if (!tenantContext.isPlatformActor()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Platform licensing permission is required for cross-tenant administration");
        }
    }

    public Long resolveTenant(Long requestedTenantId) {
        Long currentTenantId = tenantContext.requireTenantId();
        if (requestedTenantId == null || currentTenantId.equals(requestedTenantId)) {
            return currentTenantId;
        }
        if (tenantContext.isPlatformActor()) {
            return requestedTenantId;
        }
        throw new ApiException(ErrorCode.ACCESS_DENIED, "Cross-tenant licensing access is not permitted");
    }

    public void requireAccessTo(Long tenantId) {
        resolveTenant(tenantId);
    }

    /** Empty means that the platform actor may see every tenant. */
    public Optional<Long> tenantFilter() {
        Long currentTenantId = tenantContext.requireTenantId();
        return tenantContext.isPlatformActor() ? Optional.empty() : Optional.of(currentTenantId);
    }
}
