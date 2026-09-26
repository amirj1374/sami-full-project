# Repository Real Browser QA

The repository-owned harness lives in `sami-frontend/playwright.config.ts` and
`sami-frontend/tests/browser/`. It uses Playwright with desktop, tablet and
mobile projects, screenshots/video/trace on failure, console and failed-network
capture, refresh persistence checks, and environment-provided credentials.

## Entry point

From `sami-frontend`:

```powershell
npm run run-real-user-acceptance
```

Set `SAMI_E2E_BASE_URL`, `SAMI_E2E_EMAIL`, and `SAMI_E2E_PASSWORD` only through
approved local/CI environment configuration. Never commit credentials. The
normal runner is:

```powershell
npx playwright install chromium
npm run run-real-user-acceptance
```

When the host cannot install a browser, use the version-matched official
Playwright container (`mcr.microsoft.com/playwright:v1.52.0-noble`) with the
repository mounted and the frontend/backend reachable from the container.
This is a test-only dependency and does not alter production authentication.

User-facing business journeys must additionally use the
`real-user-acceptance` skill and record the full action/visible/business/
persisted/cross-screen ledger. Browser infrastructure failure is explicitly
`BLOCKED BY HARNESS`; it is never silently treated as PASS.
