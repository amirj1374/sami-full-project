# Asan Accounting Migration & Reconciliation — implementation report

## Scope

This revision extends the existing Legacy Asan Import & Comparison Center. It
does not introduce a competing import module and does not write canonical
SAMI data. It adds tenant-scoped migration groups, direct Asan accounting XLSX
staging, evidence-based report detection, reconciliation controls, explained
differences, and a fail-closed acceptance boundary.

## Attached-source analysis (sanitized)

All ten supplied workbooks were inspected directly. The handoff establishes
IRR currency, frozen-source timing, independent preservation of all persons
and all 18 warehouses, Journal as the accounting test source, Trial Balance as
control evidence, and the documented IRR 699,837,000 bank timing difference.

| Source category | Detected result | Evidence outcome |
| --- | --- | --- |
| Daily journal | `ACCOUNTING_JOURNAL` | High confidence; repeated report headings present and must be skipped |
| Trial balance (two identical exports) | `TRIAL_BALANCE` | High confidence; opening/period/closing debit and credit fields present |
| Product operation export | `PRODUCT_MOVEMENT` | High confidence; product, invoice, movement and monetary fields present |
| General/subsidiary/detailed ledgers | `ACCOUNT_LEDGER` | Low confidence because their row signatures are equivalent; explicit confirmation required |
| Cheque exports (two layouts) | `CHEQUE_RECEIVABLE`/`CHEQUE_PAYABLE` | High confidence; their column layouts differ and are not treated as identical |
| Handoff workbook | migration rules | Parsed as business/acceptance instruction, not import data |

Physical worksheet-row counts observed during the read-only source inspection:
daily journal 33,796; detailed ledger 28,589; subsidiary ledger 28,592;
general ledger 28,563; product operation 22,375; cheque exports 587 and 14;
trial-balance exports 33 each; handoff 50. Parser row classification excludes
repeated headers, totals, footers, and blank rows from staged data.

No individual names, account numbers, telephone numbers, cheque numbers, or
financial rows are copied into this report, Git, logs, or fixtures.

## Changes

- `V46__asan_accounting_migration_reconciliation.sql` adds migration groups,
  evidence type/provenance, reconciliation exceptions, acceptance checks, and
  additive admin-only reconciliation/acceptance permissions.
- `AsanAccountingExcelAdapter` stages individual XLSX reports with raw source
  provenance, deterministic normalization, report detection, row
  classification, and `BigDecimal` money parsing.
- Existing RAR and manifest-ZIP adapter behavior remains available. ZIP
  recognition is restricted to `.zip`, allowing `.xlsx` to be handled by its
  dedicated adapter.
- The existing Legacy Import API now supports migration groups, XLSX upload,
  manual confirmation of ambiguous ledger level, reconciliation, and exception
  review.
- The Legacy Import UI now creates/selects groups, uploads several evidence
  files into a group, shows report confidence, stages reports, and presents
  reconciliation/acceptance evidence in Persian and English.

## Reconciliation and acceptance result

The implementation calculates journal debit/credit totals from staged rows,
trial-balance and cheque evidence counts, and stores acceptance checks. The
known bank timing amount is stored as a visible `EXPLAINED_DIFFERENCE` from the
handoff—not silently matched or journalized. Final Import stays blocked until
an approved canonical accounting/bank/cheque owner and promotion contract are
implemented.

## Privacy and safety

All processing resolves tenant scope from `TenantContext`; frontend state and
request content cannot choose a tenant. Direct XLSX input is bounded, parsed
without formula execution, and stored via existing opaque file storage.
Source files remain outside the repository. No canonical business write API was
added.

## Validation status

- Frontend localization parity and source-contract tests: passed.
- Focused parser regression tests: passed (4 tests).
- Fresh PostgreSQL 16 Flyway V1–V46, Spring/Hibernate startup, backend and
  frontend health checks: passed in disposable Compose validation.
- Trial Balance staging persisted 32 rows in `legacy_records`; canonical ERP
  tables were not mutated.
- **Open P0 defect:** the real 33,796-row Daily Journal exhausts heap in the
  Apache POI/XSSF `DataFormatter` path during staging. The transaction rolls
  back and leaves the batch `READY`, so no partial staging records are kept.
  A streaming XLSX reader is required before migration acceptance testing.
- Do not treat this revision as release-ready or migration-acceptance-ready.
