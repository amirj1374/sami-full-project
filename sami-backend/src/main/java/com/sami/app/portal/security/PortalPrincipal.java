package com.sami.app.portal.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * The authenticated customer.
 *
 * <p>Deliberately NOT {@code SecurityUser}. A customer is a CRM {@code customers}
 * row, not a staff {@code users} row, and must never hold an RBAC role. Two
 * independent safeguards keep the realms apart:
 *
 * <ol>
 *   <li>{@code Authz.has(...)} returns false for any principal that is not a
 *       {@code SecurityUser}, so a portal principal can never satisfy a staff
 *       {@code @PreAuthorize} check even if one were reached.</li>
 *   <li>Capabilities are exposed as authorities prefixed {@code PORTAL_}, so they
 *       cannot collide with a staff permission code such as {@code users:view}.</li>
 * </ol>
 */
public record PortalPrincipal(Long accountId,
                              Long customerId,
                              Long tenantId,
                              String username,
                              String language,
                              String timezone,
                              Set<String> capabilities,
                              Long sessionId) {

    public static final String AUTHORITY_PREFIX = "PORTAL_";

    public Collection<? extends GrantedAuthority> authorities() {
        return capabilities.stream()
                .map(c -> new SimpleGrantedAuthority(AUTHORITY_PREFIX + c))
                .toList();
    }

    public boolean can(String capability) {
        return capabilities.contains(capability);
    }
}
