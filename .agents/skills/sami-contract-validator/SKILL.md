---
name: sami-contract-validator
description: Validate and synchronize contracts between SAMI ERP backend and frontend. Use when REST APIs, DTOs, TypeScript models, authentication, lifecycle fields, filters, pagination, errors, permissions, mocks, or cross-module integrations change or need reconciliation.
---

# SAMI Contract Validator

Analyze first and remain read-only unless the user explicitly requests fixes. When fixing mismatches, treat the verified backend contract as authoritative while preserving compatibility where practical.

## Discover both sides

1. Read applicable `AGENTS.md`; record repository roots, branches and worktree state.
2. Inspect backend controllers, request/response DTOs, Jackson configuration, validation, exception envelopes, security and OpenAPI/tests.
3. Inspect frontend API clients, TypeScript types, schemas, stores, composables, views and mock handlers.
4. Map each frontend operation to an executable backend endpoint. Do not infer implementation from routes, types, placeholders or migrations.

## Contract matrix

Compare:

- API base/version, paths and HTTP methods;
- path/query parameters, request bodies and content types;
- pagination envelope, indexes, totals and sort/filter syntax;
- response fields, naming, nested shapes and nullability;
- enum/status codes and lifecycle behavior;
- dates, times, zones, decimals, identifiers and file payloads;
- Jakarta/Zod/form validation limits and optional-value normalization;
- success/error envelopes, status codes and field errors;
- authentication refresh/logout behavior and permission requirements;
- optimistic-lock/version fields and concurrency responses.

Detect Java record/bean/Jackson naming mismatches, frontend-only assumptions, backend fields intentionally or accidentally ignored, hardcoded frontend lifecycle rules, API version incompatibility and mocks that drift from real APIs.

Verify English/Persian paths, labels and error-message behavior without treating localization as server authorization.

## Findings and fixes

Classify mismatches as blocking, high, medium or low. Cite exact paths and tight line ranges. Distinguish verified mismatch, likely risk and unverified contract.

When authorized to fix:

- state the proposed compatibility strategy first;
- prefer additive backend changes or compatible frontend consumption;
- do not expose sensitive fields merely to satisfy a frontend assumption;
- update types, schemas, clients, mocks, tests and localization together;
- do not duplicate backend business rules in the frontend.

## Verification

Run relevant backend contract/security tests, frontend type-check/tests/build, OpenAPI checks and browser/network verification when available. Record exact commands and failures. Never report unavailable checks as passed.

## Final report

- contract matrix;
- confirmed matches;
- mismatches with severity and exact evidence;
- compatibility impact;
- recommended/fixed order;
- files changed when authorized;
- backend/frontend/mock verification results;
- remaining assumptions and unverified areas.
