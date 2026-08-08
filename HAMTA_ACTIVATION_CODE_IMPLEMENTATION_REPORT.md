# CR-001 — HAMTA Activation Code Management

Date: 2026-08-08  
Branch implemented: `codex/feature-hamta-activation-code`

## Delivered

- Flyway `V41`: tenant policy, product eligibility, one code per canonical serial
  unit, one-time delivery evidence, audit table, module and six permissions.
- Eligible used-phone purchase receipt gate with per-unit IMEI/code capture.
- Inventory-owned canonical custody and immutable post-delivery code.
- Permission-separated lookup, correction, sale invoice, delivery and report APIs.
- Product, Purchasing and HAMTA UI in English/Persian with responsive containment.
- Existing browser Print / Save PDF contract receives the canonical code without
  introducing a second persisted invoice snapshot.

## Validation evidence

- `mvn test`: PASS, including HAMTA migration/security/purchase contracts.
- `npm test`: PASS (11/11), including localization parity and HAMTA contract.
- `npm run type-check`: PASS. `npm run build`: PASS.
- Fixture-backed browser inspection: Persian RTL PASS at 360, 375, 390, 412,
  430 and 1280 px after containing report scrolling inside the table; no page
  horizontal overflow. English LTR has localization/type/build evidence, but a
  direct browser LTR fixture navigation was blocked by browser URL policy and is
  not claimed as visual evidence.
- DOCX text was extracted and reconciled. LibreOffice was absent, so page render
  verification was unavailable.

## Environment-blocked release gates

`BLOCKED_BY_WORKSTATION_TOOLING`: fresh PostgreSQL Flyway V1→V41, PostgreSQL
integration validation, Linux/amd64 backend and frontend images, production
Compose, nginx proxy and container healthchecks. No deployment was attempted.

Ready for customer UI testing with fixtures/mocks; not production release-ready
until the blocked infrastructure gates and a live bilingual smoke pass succeed.
