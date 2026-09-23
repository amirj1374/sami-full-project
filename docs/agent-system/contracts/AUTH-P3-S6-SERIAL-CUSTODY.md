# Contract: Phase 3 Step 6 Serial Custody

- **Contract ID:** `CONTRACT-P3-S6-SERIAL-CUSTODY`
- **Authorization ID:** `AUTH-P3-S6-SERIAL-CUSTODY`
- **Parent Phase/Wave/Feature:** Phase 3 / Inventory / Step 6
- **Objective:** Complete Variant-aware Serial/IMEI custody and integrity.
- **Business Outcome:** Preserve legacy Product-only serials while enforcing
  tenant, Product, Variant, custody, lifecycle, HAMTA, and concurrency integrity.
- **Scope:** Existing V71 work, serial receipt/reservation/release/issue,
  supported transfer/return paths, HAMTA preservation, PostgreSQL acceptance,
  regression, Guardian review, and authorized completion evidence.
- **Out of Scope:** Step 7; broad Inventory redesign; new return subsystem;
  business-rule changes; release/deployment; unrelated dirty files.
- **Authoritative Sources:** `docs/SAMI_ERP_BUSINESS_RULES.md`,
  `docs/SAMI_ERP_ARCHITECTURE_CONSTITUTION.md`, `docs/IMPLEMENTATION_ROADMAP.md`,
  `docs/agent-system/GOVERNANCE.md`, current source, migrations, and tests.
- **Applicable Acceptance Criteria:** V71 fresh and V50 upgrade; legacy
  Product-only compatibility; Variant identity/isolation; transfer and rollback;
  issue and double-issue safety; supported returns; HAMTA preservation; real
  PostgreSQL concurrency; Step 4/5 regression; final fresh certification.
- **Dependencies:** V66–V70 baseline; Inventory public operations; PostgreSQL
  16; Flyway; existing TenantContext and PostgreSQL row locking.
- **Inputs / Existing Interfaces:** Inventory ledger/workflow services,
  `inventory_serial_units`, `inventory_transfer_items`, HAMTA activation relation,
  and `InventorySerialVariantPostgresAcceptanceIT`.
- **Expected Outputs:** Green acceptance and migration evidence, focused/full
  regression results, Guardian result, updated state, and permitted Git closure.
- **Allowed Change Area:** Step 6 backend, V71, directly related tests and
  execution evidence.
- **Forbidden Changes:** Step 7, release/deploy, unrelated frontend/docs,
  weakening constraints, historical rewrite, JVM-global locking, invented rules.
- **Primary Agent:** Lead / Orchestrator
- **Supporting Agents:** Backend; Data & Integrity; QA & Scenario; Guardian
- **Dependency / Parallelism Plan:** First isolate PostgreSQL and validate V71.
  QA may inspect scenarios in parallel; schema and lifecycle changes serialize
  around V71 and shared Inventory services. Regression follows acceptance.
- **Validation Required:** `mvn -B -DskipTests test-compile`; fresh V1→V71;
  supported V50→V71; Step 4, Step 5, Step 6 acceptance; focused tests; full
  backend tests; final fresh combined PostgreSQL run.
- **Guardian Required:** Yes, after technical validation is green.
- **Handoff Requirements:** Use the handoff fields in the run artifact and
  `AGENT-MODEL.md`; evidence must include exact commands and test totals.
- **Blocking TBDs:** None known for the authorized scope.
- **Known Starting Failures:** Prior expanded external run used a non-empty
  database and failed before Flyway initialization; treat as environment setup,
  not a proven production defect.
- **Completion Conditions:** All applicable acceptance/regression/certification
  gates pass; Guardian permits completion; unrelated changes preserved; state,
  run, and Git evidence complete.
- **Status:** `COMPLETED`
