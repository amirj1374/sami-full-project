# SAMI ERP implementation backlog

Current as of 2026-08-08. Completed implementation is cataloged in `docs/05-module-catalog.md`; historical reports are evidence, not scheduling authority.

## P0 — release validation

### Validate V42 end-of-day revision on production-like infrastructure

- Run fresh PostgreSQL 16 Flyway V1→V42 and upgrade-path/schema validation.
- Run backend integration tests and linux/amd64 backend/frontend builds.
- Validate production Compose, healthchecks, nginx `/api`, login and build provenance.
- Browser/mobile smoke Customer Purchase, Data Quality, Legacy Asan, HAMTA, Market Sync and Vazirmatn/PWA in Persian RTL and English LTR.
- Do not deploy as part of validation. Exact commands: `docs/HANDOFF_NEXT_SESSION.md`.

## P1 — approved capability completion

### Complete Market Sync external connectivity

- Obtain authorized structured contracts for the intended Rond sources, including authentication, pagination, schema and rate limits.
- Implement/configure the concrete source adapter without scraping undocumented HTML.
- Approve and implement the real website publication connector; verify idempotent publish/update/unpublish, retries and failure recovery.
- Run live source, database, inventory/sale and publication integration tests before marking production-ready.

### Complete Files/Media

- Resolve managed versus legacy storage ownership; finish routed upload/download/version/folder/delete workflows and storage/security validation.

### Complete Appointments/Resources

- Reconcile booking/customer/resource contracts, complete management workflows, then enable through a forward migration and full release gate.

### Complete general reporting

- Inventory existing module reports and approve one cross-module reporting owner before adding new analytics contracts.

## P2 — architecture approval required

### Real Web Push

- Review the persistence, VAPID secret, after-commit delivery and retry proposal in `FULL_NEXT_PHASE_IMPLEMENTATION_REPORT.md`.
- Do not implement until architecture/security approval is explicit.

### Customer-purchase accounting integration

- Add settlement posting/offset behavior only after canonical Accounting publishes an approved posting/reversal contract.

## P3 — future modules

- Repairs, Warranty, Installments and canonical Accounting remain planned. Each requires an approved domain/ownership contract before implementation.
