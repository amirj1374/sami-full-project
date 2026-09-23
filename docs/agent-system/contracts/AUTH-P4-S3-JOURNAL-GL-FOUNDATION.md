# Contract AUTH-P4-S3-JOURNAL-GL-FOUNDATION

- **Contract ID:** `AUTH-P4-S3-JOURNAL-GL-FOUNDATION`
- **Authorization ID:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Add the minimal canonical immutable Journal/GL persistence
  foundation and database-enforced balance/idempotency invariants.
- **Business Outcome:** Accounting can persist balanced, tenant-scoped journal
  facts without rewriting posted history or duplicating an idempotent posting.
- **Scope:** Additive V74 migration for journal headers, lines, posting and
  idempotency references, immutable rows, and deferred balanced-entry checks.
- **Out of Scope:** Domain posting rules, tax/provider behavior, returns,
  reports, UI, statutory integrations, Phase 5, release/deploy.
- **Authoritative Sources:** Phase 4 execution plan, BR-ACC-002/003/004,
  `DEC-P4-001-accounting-boundary.md`, architecture constitution.
- **Primary Agent:** Data & Integrity / Backend
- **Supporting Agents:** QA & Scenario, Guardian
- **Dependencies:** P4-S2 accounting foundation.
- **Validation Required:** SQL review, PostgreSQL migration application,
  balanced-entry and idempotency acceptance before P4-S3 closure.
- **Guardian Required:** Yes
- **Completion Conditions:** V74 applies without checksum/schema errors;
  journal rows are immutable, scoped references are unique, and posted entries
  require non-zero balanced debit/credit lines.
- **Status:** COMPLETED
- **Completion Evidence:** V74 applied on a genuinely empty PostgreSQL 16.15
  database and on a supported V72 fixture; the Backend posting boundary was
  added with tenant/scope validation, open-period locking, balanced whole-Toman
  validation, account validation, and idempotent posting lookup. Maven
  `test-compile` completed successfully.
