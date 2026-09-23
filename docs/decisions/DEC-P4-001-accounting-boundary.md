# DEC-P4-001 — Phase 4 Accounting Boundary Decisions

- **Decision ID:** `DEC-P4-001`
- **Status:** APPROVED
- **Date:** 2026-09-23
- **Scope:** Phase 4 Accounting — fiscal periods and tax configuration
- **Authority:** Owner / Product authority
- **Supersedes:** None
- **Superseded By:** None
- **Affected Requirements:** `HIGH-009`; BR-ACC-001–004; Phase 4 execution plan

## Decision

SAMI supports configurable fiscal periods. Authorized financial/accounting users
may define periods with `OPEN` or `CLOSED` status. Posting into a closed period
is forbidden. Reopening requires Manager authorization and a mandatory reason;
close/reopen actions record actor and timestamp. Reopening does not rewrite
historical accounting records.

Tax behavior is configuration-driven. Authorized financial/accounting users may
configure tax rates, exemptions and applicable withholding rules. Changes use
effective dates/versioned history, and historical financial documents preserve
the tax result applicable when they became authoritative.

Official/legal document-provider formats and external statutory integrations are
not Accounting-core blockers unless separately approved.

## Implementation and validation impact

Schema, permissions, locking, APIs, calculation, UI, audit and validation design
remain delegated to responsible Agents within the approved architecture. Phase 4
acceptance must verify closed-period rejection, authorized audited reopen, and
effective-dated preservation of historical tax results.
