package com.sami.app.automation.engine;

import com.sami.app.automation.spi.AutomationContext;
import com.sami.app.crm.event.CustomerDomainEvent;
import com.sami.app.dashboard.event.DashboardDomainEvent;
import com.sami.app.purchasing.event.PurchaseDomainEvent;
import com.sami.app.supplier.event.SupplierDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The only place business modules connect to automation. It subscribes to the
 * domain events the business modules already publish and normalizes each into an
 * {@link AutomationContext} for the engine — so business modules stay ignorant of
 * automation entirely (they just publish events).
 *
 * <p>Listeners run {@code AFTER_COMMIT}, so automations only ever act on
 * committed data. Trigger keys are canonical {@code module.entity.ACTION}
 * strings; rules bind exact keys or wildcards.
 */
@Component
@RequiredArgsConstructor
public class DomainEventBridge {

    private final AutomationEngine engine;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCustomerEvent(CustomerDomainEvent event) {
        engine.dispatch(new AutomationContext(
                "crm.customer." + event.eventType(), "crm", "customer", event.customerId(),
                data(event.detail(), "title", event.title(), "sourceModule", event.sourceModule()),
                event.tenantId(), null, null, null, event.occurredAt(), 0));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseEvent(PurchaseDomainEvent event) {
        engine.dispatch(new AutomationContext(
                "purchasing.purchase." + event.action(), "purchasing", "purchase", event.purchaseId(),
                data(event.detail(), "purchaseNumber", event.purchaseNumber()),
                event.tenantId(), null, null, null, event.occurredAt(), 0));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSupplierEvent(SupplierDomainEvent event) {
        engine.dispatch(new AutomationContext(
                "supplier.supplier." + event.action(), "supplier", "supplier", event.supplierId(),
                data(event.detail(), "supplierCode", event.supplierCode()),
                event.tenantId(), null, null, null, event.occurredAt(), 0));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDashboardEvent(DashboardDomainEvent event) {
        engine.dispatch(new AutomationContext(
                "dashboard." + event.entityType() + "." + event.eventType(), "dashboard",
                event.entityType(), event.entityId(),
                data(event.payload()), event.tenantId(), event.companyId(), event.branchId(), event.userId(),
                event.occurredAt(), 0));
    }

    private Map<String, Object> data(Map<String, Object> base, Object... extra) {
        Map<String, Object> map = new HashMap<>(base == null ? Map.of() : base);
        for (int i = 0; i + 1 < extra.length; i += 2) {
            if (extra[i + 1] != null) {
                map.put(String.valueOf(extra[i]), extra[i + 1]);
            }
        }
        return map;
    }
}
