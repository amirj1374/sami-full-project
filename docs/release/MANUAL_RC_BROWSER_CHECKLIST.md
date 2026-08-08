# Manual RC browser acceptance checklist

Release SHA: `a766b10b13efc7093d5751e5ed1bed3458cdaa20`

Use a normal supported desktop/mobile browser against the deployed RC. Do not
record credentials, customer data, activation codes, or screenshots containing
them in Git. Mark each item PASS or FAIL and attach evidence outside the
repository where required.

## Authentication and navigation

- [ ] **Login** — Sign in with the provisioned administrator account. **Expected:** Dashboard loads and the permission-driven menu is visible.
- [ ] **Session** — Refresh Dashboard. **Expected:** session remains authenticated and `/api/v1/users/me` succeeds in browser network tools.

## Customer purchase / trade-in

- [ ] **Create** — Create/select a customer, select *Customer seller / trade-in*, a used HAMTA-eligible phone, IMEI/serial requirements, valuation and settlement; save draft. **Expected:** seller remains Customer after refresh.
- [ ] **Receive** — Submit/approve and receive one serialized unit. **Expected:** receipt history opens, IMEI is shown once, inventory receives the unit, and customer history records the event.

## HAMTA

- [ ] **Required-code guard** — Attempt eligible used-phone receiving without a code. **Expected:** a clear validation rejection.
- [ ] **Persistence and report** — Receive with a disposable valid code, refresh the HAMTA page, then test unfiltered and filtered reports. **Expected:** code is linked to the IMEI once; both reports work.
- [ ] **Sales/delivery** — Sell the unit and open invoice/preview, then perform delivery. **Expected:** HAMTA resolves, delivery is one-time, timestamp and employee are recorded.

## Other business modules

- [ ] **Legacy Asan** — Upload an approved sanitized archive; analyze, stage, browse datasets and compare. **Expected:** batch persists and canonical CRM/product/inventory/sales data is not automatically written.
- [ ] **Data Quality** — Open the page and run one safe action/filter. **Expected:** live data or a legitimate empty state; permission errors are handled.
- [ ] **Market Sync** — Review Overview, Sources, Pricing Profiles, Publication Rules and history. **Expected:** disabled Rond sources remain disabled and publication without a connector fails closed.
- [ ] **Sales** — Create/save/reload a representative sale and exercise the permitted payment/lifecycle action. **Expected:** detail and serial handling persist.

## Localization, responsive UI and PWA

- [ ] **Persian RTL** — Switch to Persian and repeat representative Dashboard, Purchasing, HAMTA, Asan, Data Quality, Market Sync and Sales navigation. **Expected:** `dir=rtl`, no raw keys, readable mixed IMEI/serial values.
- [ ] **English LTR** — Switch back to English. **Expected:** `dir=ltr`, labels and forms remain usable.
- [ ] **Widths** — At 360, 375, 390, 412, 430 and desktop widths, open navigation plus a long dialog/form. **Expected:** no page-level horizontal overflow; controls and sticky actions are reachable.
- [ ] **Vazirmatn** — Inspect body, input, button, table and dialog computed styles. **Expected:** Vazirmatn is applied and font assets return HTTP 200.
- [ ] **PWA** — Reload after a deployment/update and inspect manifest/service worker. **Expected:** current assets are used; no stale bundle. Installed-PWA/iOS behavior may be recorded separately.

## Sign-off

- [ ] PASS — all applicable checks above passed.
- [ ] FAIL — record exact route, action, expected result, actual result and screenshot/network evidence.
