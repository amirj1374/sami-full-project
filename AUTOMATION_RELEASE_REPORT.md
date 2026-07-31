# Automation Release Report

## Release identity

- Release branch: `codex/final-sami-release`
- Automation completion commit: `b41432d1ba1e637aeb9981f6f6b74e2e6a5cd72e`
- Automation merge commit: `7e4fdc313fff0d997add8c0d676e0a3a82860822`
- Validated release code SHA: `7e4fdc313fff0d997add8c0d676e0a3a82860822`
- Merge conflicts: none
- Validation date: 2026-08-01

The release branch contains the complete Automation feature history, including tenant isolation, shared scheduler integration, production operations, frontend delivery, permissions, localization, migration, and focused tests.

## Tests executed

### Backend

- Command: `mvn -B test` in a fresh Docker volume populated from the release worktree.
- Java compilation: 849 production source files and 22 test source files.
- Result: **168 tests passed; 0 failures; 0 errors; 0 skipped**.
- Automation coverage included tenant isolation, audit tenant correctness, execution policy, scheduler handlers, failure retry/resolve operations, event correctness, and CSV encoding.
- Flyway startup validation: all 29 migrations validated; schema version 29 was current.

### Frontend

- `npm run type-check`: passed.
- `npm run build`: passed with Vite 8.1.5; 977 modules transformed.
- English/Persian localization parity: 1,281 keys in each locale; no missing keys in either direction.

## Browser verification

The production Docker images were exercised through the isolated full-stack validation environment.

- Authentication and protected `/automation` navigation: passed.
- Automation list, status summary, filters, execution count, and action controls: passed.
- Monitoring dialog: passed; displayed one successful execution, no running/failed executions, and no open failures.
- Import, export, execution report, monitoring, and create controls were present according to the authenticated administrator permissions.
- Desktop viewport (1440 x 900): passed without horizontal page overflow.
- Browser used: the Codex in-app Chromium browser.

## Mobile and localization verification

- Mobile viewport: 390 x 844.
- English LTR: passed; document `scrollWidth` equaled `clientWidth` (380 px), with no visible main-content overflow.
- Persian RTL: passed; document `scrollWidth` equaled `clientWidth` (380 px), with no visible main-content overflow.
- The five primary Automation actions reflowed into two-column rows with a full-width create action.
- The monitoring dialog occupied the mobile viewport without horizontal overflow.
- Persian labels, RTL document direction, filters, metrics, empty state, and action controls rendered correctly.

## Docker verification

Both images were built from the validated release merge with Docker Buildx and loaded locally.

| Image | Image ID | OS/architecture | Embedded revision | Health check |
| --- | --- | --- | --- | --- |
| `sami-backend:automation-release` | `sha256:095e8c227f29381702853cae458af754b60290740c6f7744111c20e2b6bcc25a` | `linux/amd64` | `7e4fdc313fff0d997add8c0d676e0a3a82860822` | `/actuator/health` |
| `sami-frontend:automation-release` | `sha256:8612b809ffe0b63d8dbdf21c86b60a2e679ddabe8a9861abe3c0df3b26880db5` | `linux/amd64` | `7e4fdc313fff0d997add8c0d676e0a3a82860822` | nginx root health check |

- Backend container health: healthy.
- Frontend container health: healthy.
- PostgreSQL container health: healthy.
- Production nginx `/api` proxy: verified through authenticated browser use.
- Shared scheduler startup registered both `automation.rule` and `automation.failure-retry` handlers.

## Known limitations

- Automation domain events use the repository's current in-process Spring event model; durable outbox delivery is not provided.
- Action timeouts cancel through Java interruption. A provider that ignores interruption may continue internal work until it returns.
- The notification action uses the existing provider seam. Durable external notification delivery depends on a registered Notification Center/communication provider and is not implemented directly in Automation.
- Configuration import is limited to 500 rules per request, and configuration/report exports are capped at 10,000 rows as explicit operational safeguards.
- Browser verification covered Chromium. Safari, Firefox, and physical-device browser testing were not run in this local gate.

No known limitation above blocks the completed Automation workflow or its release integration.
