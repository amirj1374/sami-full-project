# Contract AUTH-P4-S7-ACCOUNTING-REPORTS-UI

- **Authorization:** `AUTH-P4-PHASE4-ACCOUNTING`
- **Objective:** Expose read-only canonical journal/GL evidence through SAMI's existing API/UI conventions.
- **Scope:** tenant-scoped journal listing and balanced debit/credit summary; existing authorization and localization conventions; no posting or policy changes.
- **Out of scope:** new accounting rules, statutory reports, tax calculation, workflow/permissions invention, Phase 5.
- **Agents:** Backend, Frontend, Contract Validator, QA, UI Quality, Guardian.
- **Acceptance:** API tenant isolation and reconciliation tests; rendered desktop/tablet/mobile UI evidence for loading, empty, populated, error and read-only states; Persian/English and RTL/LTR checks; full regression.
- **Status:** COMPLETED — implementation, API validation and rendered UI gate passed with recorded viewport limitation.
