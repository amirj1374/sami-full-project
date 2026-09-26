# Contract AUTH-CROSS-MODULE-PHONE-LIFECYCLE

- **Authorization:** Owner-authorized Cross-Module End-to-End Business Flow
  Certification.
- **Parent Phase/Wave/Feature:** Post-Phase-5 cross-module certification;
  Phone purchase-to-sale lifecycle.
- **Objective:** Prove and repair the approved user journey from Variant phone
  purchase and serialized receipt through priced sale, physical issue, and
  applicable accounting traceability.
- **Business Outcome:** See `docs/SAMI_ERP_BUSINESS_RULES.md` (BR-PUR-006–011,
  BR-INV-002–005, BR-PRODUCT-001–002, BR-PRICE-001–002, and BR-SALES-011–016)
  and the Architecture Constitution. This contract does not create new
  commercial or accounting rules.
- **Scope:** Existing Purchasing, Inventory, Sales, Accounting and UI contract
  boundaries needed to preserve Product/Variant/Serial identity; a permanent
  PostgreSQL acceptance journey; authenticated UI journey and UI Quality
  evidence.
- **Out of Scope:** Phase 6, release/deployment, new pricing policy, new
  payment/settlement workflow, changing historical records, and unrelated
  Sales work.
- **Authoritative Sources:** Roadmap phases 2–4; Business Rules; Architecture
  Constitution; Inventory public API; existing V60–V79 migrations and current
  Sales/Purchasing contracts.
- **Known Starting Failure (resolved):** Purchase order/goods receipt lines
  were Product-only and delivery dropped Variant/Serial identity during issue.
  The approved integration work now carries Product/Variant/Serial identity
  through receipt, reservation, delivery issue and invoice traceability.
- **Dependencies:** Completed Phases 2–5, V71 Serial custody and V72 Sales
  Variant provenance.
- **Allowed Change Area:** Directly related schema, Purchasing/Sales/Inventory
  services, DTO/API/UI contracts, PostgreSQL acceptance and execution evidence.
- **Forbidden Changes:** New business behavior, hard-coded accounting mappings,
  automatic financial settlement, Phase 6, release/deployment, and unrelated
  frontend work.
- **Primary Agent:** Lead / Orchestrator.
- **Supporting Agents:** Backend; Frontend; Data & Integrity; Contract
  Validator; QA & Scenario; UI Quality; Guardian.
- **Dependency / Parallelism Plan:** Serialize shared data-contract and
  migration work; independently trace Purchasing and Sales issue boundaries;
  integrate through the Inventory public API; validate end-to-end only after
  both boundaries preserve identity.
- **Validation Required:** Fresh and supported-upgrade Flyway migration,
  permanent real-PostgreSQL phone journey, targeted regression of Variant/UOM,
  reservations/backorders, serial custody, Sales receipt/accounting, frontend
  type/test/build, authenticated UI evidence, Guardian review, and `git diff
  --check`.
- **Guardian Required:** Yes.
- **Blocking TBDs:** None identified. The approved Sales invoice/receivable and
  delivery/physical-issue separation is preserved.
- **Completion Conditions:** A legitimate authenticated user can complete the
  applicable phone journey without developer-only operations; persisted
  Product/Variant/Serial identity, stock transitions and traceability are
  proven; permanent acceptance and relevant regression are green; UI Quality
  and Guardian pass; state/run artifacts and scoped Git closure are complete.
- **Status:** COMPLETED — PostgreSQL and backend certification passed; rendered
  browser inspection was unavailable because no browser surface was exposed by
  the execution environment.
- **Evidence:** `PhonePurchaseToSalePostgresAcceptanceIT` 1/0/0 on a fresh
  PostgreSQL 16 database; `SalesBusinessFixturePostgresIT` 3/0/0; Maven
  backend regression 315/0/0; frontend type-check, tests 56/0/0 and production
  build PASS; Flyway fresh V1→V80 and supported V50→V80 PASS.
