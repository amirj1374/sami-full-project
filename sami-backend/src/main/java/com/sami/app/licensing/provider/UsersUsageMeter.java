package com.sami.app.licensing.provider;

import com.sami.app.licensing.spi.UsageMeterProvider;
import com.sami.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reference {@link UsageMeterProvider}: reports the seat count for the
 * {@code max-users} limit. Shows the intended direction of coupling — the meter
 * lives next to the data it counts and the licensing core stays ignorant of user
 * storage. Other modules add their own meters (storage, API calls, transactions)
 * the same way, with no core change.
 */
@Component
@RequiredArgsConstructor
public class UsersUsageMeter implements UsageMeterProvider {

    private final UserRepository userRepository;

    @Override
    public String limitType() {
        return "max-users";
    }

    @Override
    public long currentUsage(Long tenantId) {
        // Single-tenant today: user rows are not yet tenant-scoped, so the whole
        // installation is the tenant. When users gain a tenant_id this narrows
        // to a per-tenant count without touching the licensing core.
        return userRepository.count();
    }
}
