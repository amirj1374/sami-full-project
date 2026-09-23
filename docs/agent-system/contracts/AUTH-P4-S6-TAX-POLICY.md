# Contract AUTH-P4-S6-TAX-POLICY

- **Authorization:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Add tenant/company/branch-scoped, effective-dated tax policy configuration without inventing statutory rates.
- **Scope:** additive V76 schema and service resolution; rate, exemption and withholding policy types.
- **Out of scope:** statutory provider integrations, seeded rates, document recalculation, Phase 5.
- **Status:** COMPLETED — implementation and migration validation passed.
- **Validation:** Maven test compilation PASS; fresh V76 SQL application PASS; no statutory values seeded; UI gate not applicable because no user-facing UI was introduced.
- **Guardian:** PASS — additive configuration only; no business rates or provider behavior invented.
