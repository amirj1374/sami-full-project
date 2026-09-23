# Contract AUTH-P4-S5-TREASURY-POSTING

- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Integrate finalized Treasury movements with canonical
  Accounting posting while preserving Treasury's operational ownership.
- **Status:** BLOCKED — Owner/architecture decision required.
- **Evidence:** `V49__treasury_cash_management.sql` explicitly keeps Treasury
  operational-only; no approved Treasury-to-CoA account mapping exists.
- **Minimum blocker:** Decide the approved business/architecture boundary for
  mapping Treasury accounts and finalized movements to Accounting accounts.
- **Forbidden:** Reusing Treasury IDs as Accounting IDs or inventing mapping
  semantics in implementation.
