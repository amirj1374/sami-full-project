# Open decisions

| Decision | Affected items |
|---|---|
| Tenant/company/branch/store boundaries and cross-scope users | [CRIT-001](critical/CRIT-001-tenant-isolation-and-organization-scope.md), HIGH-002–009 |
| Browser token, CSRF, portal/staff boundary and MFA | [CRIT-002](critical/CRIT-002-production-authentication-and-secret-hardening.md), MED-003 |
| Product variant dimensions, SKU and IMEI/serial ownership | [HIGH-002](high/HIGH-002-product-variant-and-serialized-product-model.md), HIGH-003/005/007 |
| Warehouse/store/location and inventory valuation | [HIGH-003](high/HIGH-003-inventory-warehouse-architecture.md) |
| Price source, tax/currency/rounding and promotion stacking | [HIGH-005](high/HIGH-005-pricing-architecture.md) |
| Sales/order/invoice lifecycle and partial fulfillment | [HIGH-007](high/HIGH-007-sales-orders-architecture.md), HIGH-009 |
| Payment providers, refunds, accounting scope and fiscal policy | [HIGH-009](high/HIGH-009-invoicing-payments-returns-accounting-boundary.md) |
| Durable-event SLA, ordering, replay and broker need | [HIGH-010](high/HIGH-010-durable-events-and-outbox.md) |
| Canonical file owner, production storage, retention and DR | [HIGH-011](high/HIGH-011-file-platform-consolidation-and-production-storage.md) |
| Scheduler delivery guarantee and timeout semantics | [HIGH-012](high/HIGH-012-scheduler-reliability.md) |
| Email/SMS/other providers, sender IDs, cost and residency | [HIGH-013](high/HIGH-013-communication-and-otp-providers.md) |
| SLO, RPO/RTO, retention, hosting and on-call ownership | [HIGH-014](high/HIGH-014-production-observability-backup-and-release-gate.md) |
| Portal deployment and exposed customer self-service | [MED-003](medium/MED-003-customer-portal-completion.md) |
| General reports, KPI definitions and settings hierarchy | [MED-004](medium/MED-004-general-reporting-settings-and-commercial-analytics.md) |
