package com.sami.app.licensing.spi;

/**
 * Extension point through which a module reports how much of a metered resource
 * a tenant is currently consuming. The licensing core knows limits but never how
 * to count business data — the owning module publishes a bean per limit type
 * (users, storage, API calls, transactions …). New limit types therefore need a
 * catalogue row plus a meter bean, never a core change.
 */
public interface UsageMeterProvider {

    /** The {@code usage_limit_types.code} this meter measures. */
    String limitType();

    /**
     * Current consumption for the tenant.
     *
     * @param tenantId tenant to measure; may be null for single-tenant installs
     */
    long currentUsage(Long tenantId);
}
