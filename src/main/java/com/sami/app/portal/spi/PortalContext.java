package com.sami.app.portal.spi;

import java.util.Map;
import java.util.Set;

/**
 * Who is asking, and for what.
 *
 * <p>{@code customerId} is resolved from the authenticated session — never from
 * a request parameter — so a provider cannot be tricked into returning another
 * customer's data by a crafted URL.
 */
public record PortalContext(Long accountId,
                            Long customerId,
                            Long tenantId,
                            String language,
                            String timezone,
                            Set<String> capabilities,
                            Map<String, Object> widgetConfig) {

    public boolean can(String capability) {
        return capabilities.contains(capability);
    }
}
