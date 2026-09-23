# Contract: Phase 3 Step 8 Cross-Module Variant/UOM Integration

- **Contract ID:** `CONTRACT-P3-S8-CROSS-MODULE-VARIANT-UOM`
- **Authorization ID:** `AUTH-P3-S8-CROSS-MODULE-VARIANT-UOM`
- **Parent Phase/Wave/Feature:** Phase 3 / Inventory / P3-S8
- **Objective:** Complete approved Variant/UOM propagation across existing
  Purchasing, Sales delivery/reservation, Inventory public APIs and applicable
  frontend contracts where repository evidence shows a missing boundary.
- **Business Outcome:** Existing cross-module inventory journeys preserve exact
  Product/Variant identity and quantity provenance without changing approved
  commercial behavior.
- **Scope:** Additive nullable cross-module identity/provenance fields and
  propagation, compatible API/types/tests, and PostgreSQL journey evidence.
- **Out of Scope:** New Product rules, price-list ownership, Phase 3 final
  certification (P3-S9), Phase 4, release/deployment, unrelated Sales UI work.
- **Authoritative Sources:** Approved Phase 3 roadmap, business rules,
  architecture constitution, V66–V71 contracts and current public Inventory
  API.
- **Acceptance Criteria:** Variant identity is validated at cross-module
  boundaries; receipt/reservation/delivery preserve it; UOM quantities retain
  entered/base provenance where supported; Product-only compatibility remains;
  tenant isolation and persisted read-back are proven.
- **Dependencies:** P3-S4–S7 and existing Sales/Purchasing public services.
- **Allowed Change Area:** Directly related backend schema/services/API,
  frontend contract/types/tests, migrations and acceptance evidence.
- **Forbidden Changes:** New business behavior, price-list redesign,
  historical rewrite, broad Inventory redesign, P3-S9, Phase 4, release.
- **Primary Agent:** Lead / Orchestrator
- **Supporting Agents:** Backend; Frontend; Contract Validator; Data &
  Integrity; QA; Guardian
- **Validation Required:** compile, migration validation, cross-module
  PostgreSQL acceptance, frontend type/test/build checks where affected, prior
  Step 4/5/7 regression and full backend regression.
- **Guardian Required:** Yes.
- **Completion Conditions:** Applicable integration gaps implemented or
  documented as unsupported, acceptance/regression green, Guardian PASS, state
  and run updated, scoped commit/push complete.
- **Status:** `COMPLETED`
