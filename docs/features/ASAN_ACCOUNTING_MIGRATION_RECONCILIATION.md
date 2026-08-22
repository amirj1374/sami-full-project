# Asan accounting migration, reconciliation, and acceptance

## Safety boundary

This capability extends the existing Legacy Asan Import & Comparison Center.
It accepts Asan backup evidence, manifest ZIP packages, and individual Asan
XLSX reports. Every file is tenant-scoped and staged in `legacy_*` storage.
It never creates or changes canonical customers, products, warehouses,
inventory, sales, accounting, bank, or cheque records.

Final Import is intentionally fail-closed. SAMI does not yet have an approved
canonical accounting, bank, and cheque migration owner. Acceptance evidence is
therefore useful for a future approved final-import workflow, but cannot be
used to promote staging rows.

## Verified source inventory

The accounting handoff and supplied exports were inspected without copying
source values into Git. The source set contains: a migration handoff, product
operation export, daily journal, general/subsidiary/detailed account ledgers,
two structurally distinct cheque exports, and duplicate trial-balance exports.

| Verified signature | Staged report type | Confidence | Use |
| --- | --- | --- | --- |
| `نام کالا`, `کد کالا`, `شماره فاکتور` | `PRODUCT_MOVEMENT` | High | product/invoice evidence |
| `دفتر روزنامه` plus debit, credit, document, account, date | `ACCOUNTING_JOURNAL` | High | primary journal evidence |
| debit, credit, document, account, date without journal signature | `ACCOUNT_LEDGER` | Low | user must confirm general/subsidiary/detailed level |
| opening/period/closing debit and credit columns | `TRIAL_BALANCE` | High | control and balance evidence |
| cheque number, amount, due date, cheque status | `CHEQUE_RECEIVABLE` or `CHEQUE_PAYABLE` | High | cheque evidence |

The three account-level exports have an equivalent row signature. They are not
silently classified from file names. The user must select General, Subsidiary,
or Detailed ledger when the reported confidence is low.

## Source precedence and handoff rules

- Daily Journal is the accounting-entry test source.
- General, subsidiary, and detailed ledgers support mapping, hierarchy, and
  reconciliation; they must not independently produce duplicate entries.
- Trial Balance is balance-control evidence.
- All 18 legacy warehouses, including zero-stock warehouses, are preserved as
  evidence and must not be collapsed by name.
- Matching persons by name is prohibited. Legacy code and other deterministic
  identifiers are the only automatic evidence.
- The documented IRR 699,837,000 bank extraction timing difference is inserted
  visibly as an `EXPLAINED_DIFFERENCE`, never as a match or balancing entry.

## Data dictionary and normalization

Each `legacy_records` row retains batch, uploaded file, sheet, source row,
raw record, normalized record, source identifier, and legacy key. The parser
maps verified source headers to normalized names including `documentNumber`,
`accountCode`, `accountName`, `debit`, `credit`, `date`, `invoiceNumber`,
`productCode`, `chequeNumber`, `sayadIdentifier`, cheque dates/status, and
trial-balance opening/period/closing amounts.

Normalization uses NFKC; Arabic Yeh/Kaf to Persian; Persian/Arabic digits to
Latin digits; whitespace cleanup; code canonicalization; and safe Persian-date
separator normalization. It does not replace raw evidence. Monetary fields use
`BigDecimal`; floating-point values are not used for money.

## Parsing and upload controls

The XLSX adapter uses Apache POI's SAX event model without evaluating formulas.
It bounds file size, expanded workbook content and worksheet count, rejects
malformed workbooks, preserves row classification, and skips repeated headers,
totals, footers, and blank rows. Analysis retains no source rows; staging
replays the workbook into configured JDBC chunks and fails the transaction if
any streamed dataset key or row count differs from the preceding analysis.
The existing archive traversal, hash, size, extraction, and provenance controls
remain in effect. Duplicate file hashes are still rejected per tenant.

## Reconciliation and acceptance

A migration group links multiple independently uploaded evidence batches.
Reconciliation calculates staged journal rows, debit, credit, their difference,
trial-balance rows, cheque rows, acceptance checks, and visible exceptions.
Exceptions use: `MATCHED`, `DIFFERENT`, `ONLY_IN_LEGACY`, `ONLY_IN_SAMI`,
`AMBIGUOUS`, `UNMAPPED`, `EXPLAINED_DIFFERENCE`, or `IGNORED`; their review
state is `NEEDS_INVESTIGATION`, `EXPLAINED`, `ACCEPTED`, or `REJECTED`.

The acceptance checklist currently verifies staged journal debit/credit,
trial-balance presence, cheque presence, and the final-import owner boundary.
The last item stays pending until an approved canonical accounting migration
contract exists. No comparison result is allowed to manipulate source or SAMI
financial values.

## Security, scope, and audit

All queries resolve scope from `TenantContext`; request bodies do not contain a
trusted tenant identity. Existing import/view/analyze/execute/compare/review/
delete permissions remain. `legacy-import:reconcile` and
`legacy-import:accept` are server-enforced and seeded only for admin roles.
Upload, analysis, staging, report-type confirmation, reconciliation, and
exception review are recorded as batch audit actions without source-row PII.

## Limitations

The attached exports establish report parsing and evidence reconciliation, not
an approved canonical accounting model. Product, warehouse, inventory,
customer, bank, and cheque promotion require their respective approved public
owners and a future final-import authorization. Source files and unsanitized
rows must never be committed, logged, or used as test fixtures.

The generated 34,000-row streaming regression passes with a 256 MB test heap.
Acceptance still requires the supplied 33,796-row Daily Journal to be staged
against isolated PostgreSQL and reconciled to its source totals; that file is
not available on every development workstation.

When the source files cannot be shared with development, the customer may use
the isolated validation candidate described in
[`../release/ASAN_CUSTOMER_VALIDATION_GUIDE_FA.md`](../release/ASAN_CUSTOMER_VALIDATION_GUIDE_FA.md).
Its downloadable JSON contains aggregate validation evidence and build
provenance, never raw rows or source filenames. This workflow validates staging
only and does not enable canonical promotion.
