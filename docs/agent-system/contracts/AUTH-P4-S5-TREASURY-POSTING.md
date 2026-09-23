# Contract AUTH-P4-S5-TREASURY-POSTING

- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Integrate finalized Treasury movements with canonical
  Accounting posting while preserving Treasury's operational ownership.
- **Status:** COMPLETED — Owner boundary approved and implementation validated.
- **Evidence:** `V49__treasury_cash_management.sql` explicitly keeps Treasury
  operational-only; no approved Treasury-to-CoA account mapping exists.
- **Approved decision:** `docs/decisions/DEC-P4-002-treasury-accounting-boundary.md`.
- **Forbidden:** Reusing Treasury IDs as Accounting IDs or inventing mappings
  when configuration is missing.
- **Validation:** Docker Maven test compilation PASS; `TreasuryContractTest`
  2/0/0; V75 SQL applied successfully to disposable PostgreSQL 16.
- **Completion:** Treasury operational events now produce auditable canonical
  posting outcomes with tenant-scoped mappings and idempotency protection.
