package com.sami.app.automation.spi;

import java.time.Instant;
import java.util.Map;

/**
 * The normalized firing context passed from a trigger through condition
 * evaluation into every action. Business modules never see this type — the
 * event bridge builds it from published domain events (or the manual-run API).
 *
 * @param triggerType canonical trigger key, e.g. {@code crm.customer.CREATED}
 * @param module      originating module slug, e.g. {@code crm}
 * @param entityType  affected entity, e.g. {@code customer}
 * @param entityId    affected entity id (may be null)
 * @param data        flattened event payload; the field source for conditions/actions
 * @param tenantId    trusted tenant scope; required for tenant-owned execution
 * @param companyId   tenant scope (nullable, forward-compat)
 * @param branchId    branch scope (nullable, forward-compat)
 * @param actorId     the user who caused the event (nullable for system events)
 * @param occurredAt  when the source event happened
 * @param depth       recursion depth guard (0 for the originating event)
 */
public record AutomationContext(
        String triggerType,
        String module,
        String entityType,
        Long entityId,
        Map<String, Object> data,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long actorId,
        Instant occurredAt,
        int depth
) {

    /** A shallow copy with an incremented recursion depth (used when re-dispatching). */
    public AutomationContext deeper() {
        return new AutomationContext(triggerType, module, entityType, entityId, data,
                tenantId, companyId, branchId, actorId, occurredAt, depth + 1);
    }
}
