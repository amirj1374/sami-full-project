# Backlog index

## Critical

| ID | Title | Status | Type | Area | Size | Dependencies | Task |
|---|---|---|---|---|---|---|---|
| CRIT-001 | Tenant and organization isolation | needs-decision | security | tenancy | XL | — | [task](critical/CRIT-001-tenant-isolation-and-organization-scope.md) |
| CRIT-002 | Production authentication and secrets | needs-decision | security | security | L | CRIT-001 | [task](critical/CRIT-002-production-authentication-and-secret-hardening.md) |

## High

| ID | Title | Status | Type | Area | Size | Dependencies | Task |
|---|---|---|---|---|---|---|---|
| HIGH-001 | PostgreSQL/Flyway integration tests | ready | test | database | M | — | [task](high/HIGH-001-postgresql-flyway-integration-tests.md) |
| HIGH-002 | Product variants and serial identity | needs-decision | architecture | product | L | CRIT-001 | [task](high/HIGH-002-product-variant-and-serialized-product-model.md) |
| HIGH-003 | Inventory architecture | needs-decision | architecture | inventory | L | CRIT-001, HIGH-002 | [task](high/HIGH-003-inventory-warehouse-architecture.md) |
| HIGH-004 | Inventory implementation | blocked | feature | inventory | XL | HIGH-001, HIGH-002, HIGH-003, HIGH-010 | [task](high/HIGH-004-inventory-warehouse-implementation.md) |
| HIGH-005 | Pricing architecture | needs-decision | architecture | pricing | L | CRIT-001, HIGH-002 | [task](high/HIGH-005-pricing-architecture.md) |
| HIGH-006 | Pricing implementation | blocked | feature | pricing | XL | HIGH-001, HIGH-005, HIGH-010 | [task](high/HIGH-006-pricing-implementation.md) |
| HIGH-007 | Sales architecture | needs-decision | architecture | sales | L | CRIT-001, HIGH-002, HIGH-003, HIGH-005 | [task](high/HIGH-007-sales-orders-architecture.md) |
| HIGH-008 | Sales implementation | blocked | feature | sales | XL | HIGH-001, HIGH-004, HIGH-006, HIGH-007, HIGH-010 | [task](high/HIGH-008-sales-orders-implementation.md) |
| HIGH-009 | Invoicing/payments/returns/accounting boundary | needs-decision | architecture | finance | XL | CRIT-001, HIGH-003, HIGH-005, HIGH-007 | [task](high/HIGH-009-invoicing-payments-returns-accounting-boundary.md) |
| HIGH-010 | Durable events/outbox | needs-decision | architecture | integration | L | HIGH-001 | [task](high/HIGH-010-durable-events-and-outbox.md) |
| HIGH-011 | File consolidation/production storage | needs-decision | architecture | files | L | CRIT-001, HIGH-001 | [task](high/HIGH-011-file-platform-consolidation-and-production-storage.md) |
| HIGH-012 | Scheduler reliability | proposed | defect | scheduler | M | HIGH-001 | [task](high/HIGH-012-scheduler-reliability.md) |
| HIGH-013 | Communication and OTP providers | needs-decision | integration | communication | L | CRIT-002, HIGH-010 | [task](high/HIGH-013-communication-and-otp-providers.md) |
| HIGH-014 | Observability/backup/release gate | needs-decision | operations | operations | XL | CRIT-002, HIGH-001, HIGH-012 | [task](high/HIGH-014-production-observability-backup-and-release-gate.md) |

## Medium

| ID | Title | Status | Type | Area | Size | Dependencies | Task |
|---|---|---|---|---|---|---|---|
| MED-001 | Frontend/API quality infrastructure | needs-decision | test | quality | L | HIGH-001 | [task](medium/MED-001-frontend-and-api-quality-infrastructure.md) |
| MED-002 | Partial-module administration UIs | proposed | feature | frontend | XL | CRIT-001, MED-001 | [task](medium/MED-002-partial-module-administration-uis.md) |
| MED-003 | Customer Portal completion | blocked | feature | portal | XL | CRIT-001, CRIT-002, HIGH-011, HIGH-013, MED-001 | [task](medium/MED-003-customer-portal-completion.md) |
| MED-004 | General reporting/settings/analytics | needs-decision | architecture | reporting | L | HIGH-004, HIGH-006, HIGH-008, HIGH-009 | [task](medium/MED-004-general-reporting-settings-and-commercial-analytics.md) |
| MED-005 | Reproducible development environment | proposed | technical-debt | developer-experience | S | — | [task](medium/MED-005-reproducible-development-environment.md) |

## Low

| ID | Title | Status | Type | Area | Size | Dependencies | Task |
|---|---|---|---|---|---|---|---|
| LOW-001 | Automated backlog/docs validation | done | documentation | documentation | S | — | [task](completed/LOW-001-automated-backlog-and-documentation-validation.md) |
