package com.sami.app.common.tenancy;

import com.sami.app.licensing.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplies the tenant id for code written before {@code TenantContext} exists.
 *
 * <p><b>Why this is needed.</b> V17 gave every Tier T table a column DEFAULT of
 * {@code tenancy_default_tenant_id()}, on the assumption that inserts omitting
 * the column would pick it up. That holds for raw SQL, but NOT for Hibernate: an
 * entity that maps {@code tenantId} always includes the column in its INSERT, and
 * an explicit NULL overrides a column DEFAULT rather than triggering it. Every
 * new Tier T entity therefore failed its NOT NULL constraint on first write —
 * which only surfaced when the application was actually run.
 *
 * <p>This is a bridge, not the design. When tenancy enforcement lands, callers
 * switch from {@code tenantDefaults.current()} to {@code TenantContext.require()}
 * and this class is deleted along with the V17 scaffold.
 *
 * <p>The id is resolved once and cached: it cannot change for the lifetime of a
 * single-tenant installation, and resolving it per insert would add a query to
 * every write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantDefaults {

    private static final String DEFAULT_TENANT_CODE = "DEFAULT";

    private final TenantRepository tenantRepository;

    private volatile Long cachedId;

    /**
     * @return the id of the DEFAULT tenant
     * @throws IllegalStateException if it is missing — failing loudly here is far
     *                               better than writing a row with no tenant
     */
    @Transactional(readOnly = true)
    public Long current() {
        Long id = cachedId;
        if (id != null) {
            return id;
        }
        synchronized (this) {
            if (cachedId == null) {
                cachedId = tenantRepository.findAll().stream()
                        .filter(t -> DEFAULT_TENANT_CODE.equals(t.getCode()))
                        .map(t -> t.getId())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No tenant with code '" + DEFAULT_TENANT_CODE + "' exists. "
                                        + "V16 should have created it."));
                log.debug("Resolved DEFAULT tenant id = {}", cachedId);
            }
            return cachedId;
        }
    }
}
