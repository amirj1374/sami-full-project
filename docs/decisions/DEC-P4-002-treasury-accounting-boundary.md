# DEC-P4-002 — Treasury / Accounting Posting Boundary

- **Status:** APPROVED
- **Date:** 2026-09-23
- **Scope:** Phase 4 P4-S5 Treasury integration
- **Authority:** Owner decision

Treasury remains the authoritative owner of operational cash truth. Accounting
owns the Chart of Accounts, Journal Entries and General Ledger. Accounting-
relevant finalized Treasury transactions must post exactly once through the
canonical Accounting boundary. Treasury accounts participating in accounting
require explicit configurable mappings to canonical Accounting accounts;
missing mappings must never be guessed and must remain auditable/retryable.
Transfers must be balanced asset movements rather than duplicate income or
expense. Corrections and reversals use immutable traceable journal behavior.

Technical schema, ports, locking, idempotency, retry state and validation are
delegated to the responsible agents within the approved architecture.
