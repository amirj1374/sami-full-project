# SAMI ERP deployment automation implementation report

## Outcome

The repository now contains a complete Windows PowerShell workflow for building,
verifying, exporting, uploading, deploying, rolling back, and locally cleaning
SAMI application releases. Routine remote authentication is non-interactive
OpenSSH public-key authentication. No server password, private-key content,
runtime secret, or `.env` content was requested, read, stored, printed, logged,
or committed during implementation.

- Branch: `development`
- Final executable implementation commit:
  `2f832b2018a0cb007eb1022ded5ef02b238763e4`
- Initial automation commit:
  `969c0b8dd9a1184c220ffa66d1de816cd0771d0e`
- Docker 29/atomic replacement correction:
  `6fbe8f9c868728c1acb31d382c2448f95449ff2b`
- Push/deployment/VPS connection: not performed

This report is intentionally a separate documentation commit after the final
executable implementation SHA. The exact branch HEAD containing this report is
available with `git rev-parse HEAD`; the ignored `release-manifest.json` created
after that commit is the authority for the exact deployable artifact revision.

## Files created

| File | Purpose |
|---|---|
| `scripts/deploy.ps1` | Strict orchestration for Build, Export, Upload, Deploy, Full, Rollback, Cleanup, and DryRun |
| `scripts/verify-deployment.ps1` | Non-mutating HTTP checks for root, health, login, Sales, Automation, Licensing, and nginx `/api` proxy |
| `scripts/release-config.example.ps1` | Reviewed non-secret configuration hashtable example |
| `scripts/tests/deployment-automation.tests.ps1` | Dependency-free PowerShell safety/contract test harness |
| `DEPLOYMENT_AUTOMATION_GUIDE.md` | Canonical setup, key authentication, use, rollback, cleanup, and troubleshooting runbook |
| `DEPLOYMENT_AUTOMATION_IMPLEMENTATION_REPORT.md` | Implementation and validation evidence |

## Files updated

| File | Change |
|---|---|
| `.gitignore` | Ignores root `.env`, all generated deployment artifacts/logs, and local release config files |
| `sami-backend/docker-compose.prod.yml` | Adds configurable explicit backend/frontend image tags required for verified prebuilt TAR deployment; existing `--build` behavior remains available |
| `PROJECT_SETUP_AND_DEPLOYMENT.md` | Links to the canonical automation guide without duplicating it |
| `docs/17-deployment-and-operations.md` | Records the automated application-image release owner and remaining operational gaps |
| `.agents/SAMI_PROJECT_CONTEXT.md` | Refreshes the deployment context and canonical automation pattern |

No Java, Vue, TypeScript, database migration, runtime secret, test fixture,
permission, tenancy, business logic, Dockerfile, or infrastructure service was
changed.

## Supported commands

```powershell
.\scripts\deploy.ps1 -Mode Build
.\scripts\deploy.ps1 -Mode Export
.\scripts\deploy.ps1 -Mode Upload
.\scripts\deploy.ps1 -Mode Deploy
.\scripts\deploy.ps1 -Mode Full
.\scripts\deploy.ps1 -Mode Rollback
.\scripts\deploy.ps1 -Mode Cleanup
.\scripts\deploy.ps1 -Mode Full -DryRun
.\scripts\verify-deployment.ps1 -BaseUrl http://SERVER
```

The default is local-only `Build`. Important optional switches include
`-IdentityFile`, `-AllowInteractiveAuth`, `-NoCache`, `-SkipUpload`,
`-AllowDirtyWorkingTree`, `-ConfirmCleanup`, and `-RetentionDays`.

## SSH authentication controls

- OpenSSH automatically uses standard `~/.ssh` keys and/or `ssh-agent`.
- `-IdentityFile` selects a specific OpenSSH key without reading or logging it.
- Routine runs set `BatchMode=yes`, disable password and keyboard-interactive
  methods, and prefer public-key authentication.
- A missing non-interactive key path fails before upload/deployment with a clear
  remediation message.
- `-AllowInteractiveAuth` only allows OpenSSH's own supervised prompt for first
  setup/emergency use. The script has no password parameter and never passes a
  password on a command line.
- Host keys use `StrictHostKeyChecking=accept-new`; changed host keys fail.

## Release safety controls

- Requires exact branch `development` and a clean worktree unless an explicit
  reviewed exception is supplied.
- Requires a Linux `amd64` Docker daemon and buildx.
- Builds both images with exact branch, full commit, UTC timestamp where
  supported, version, and `/api` frontend metadata.
- Handles Docker buildx OCI provenance indexes by verifying the explicit
  `linux/amd64` application manifest while preserving the tagged index ID and
  available digest.
- Verifies OCI revision/version labels and the frontend's embedded commit.
- Refuses Export when image metadata differs from current HEAD.
- Saves TARs to temporary files and uses same-volume atomic replace/move before
  publishing the manifest.
- Records SHA256 values in `release-manifest.json` and recalculates them before
  Upload.
- Uploads all files as `.uploading`; only after all succeed are final remote
  names replaced.
- Compares remote `sha256sum` output with the local manifest before image load.
- Stores previous image IDs, container status, source Compose, unexpanded
  effective Compose, service/image lists, and manifest in timestamped history.
- Never copies `.env` or interpolated secret values into history/logs.
- Uses loaded explicit image tags and `--no-build` for application recreation.
- Preserves PostgreSQL and every named volume; never invokes `down -v`.
- Recreates backend first, waits for health, then recreates frontend and verifies
  root, health, login, Sales, Automation, Licensing, and a protected API read.
- Failure output is redacted and triggers automatic application-image rollback
  when both prior image IDs remain available.
- Cleanup is local, narrow, preview-first, and requires `-ConfirmCleanup`.

## Rollback design

Each deployment writes a non-secret history entry under
`/root/sami-deployment-history/<UTC timestamp>/`. Automatic and explicit
rollback validate that both previous image IDs still exist, retag those images
with the configured production tags, recreate backend/frontend only, and rerun
health verification. Rollback never removes, recreates, or restores PostgreSQL
or named volumes.

Flyway/database rollback is deliberately outside this script. A deployed schema
must remain compatible with the preceding application image, consistent with
the repository's forward-migration policy.

## Validation performed

| Check | Result |
|---|---|
| PowerShell parser: orchestrator, verifier, config, tests | PASS, zero parse errors |
| Comment-based `Get-Help` | PASS |
| Development preflight | PASS |
| Wrong-branch rejection | PASS |
| Dirty-worktree rejection | PASS |
| Unavailable Docker handling | PASS |
| Missing-artifact handling before SSH | PASS |
| Invalid Mode rejection | PASS |
| Full DryRun without SSH/VPS connection | PASS |
| Cleanup preview/no-confirm behavior | PASS |
| Focused automation harness | PASS, 10/10 |
| Generated remote Bash bodies (`bash -n`) | PASS, four templates |
| Documentation/backlog validator | PASS |
| Production Compose `config --quiet` with temporary non-secret placeholders | PASS |
| Compose image resolution | PASS: PostgreSQL plus exact backend/frontend release tags |
| Real backend Docker Build (`linux/amd64`) | PASS |
| Real frontend Docker Build (`linux/amd64`, type-check + Vite build) | PASS, 1013 modules |
| Image architecture, tags, OCI labels, embedded frontend revision | PASS |
| Real atomic Export and JSON manifest | PASS |
| TAR full listing | PASS: backend 19 entries, frontend 21 entries |
| Manifest SHA256 recalculation | PASS for both TARs |
| Backend `mvn -B clean verify` in correctly mounted monorepo container | PASS: 196 tests, 0 failures/errors/skips |
| Frontend `npm test` | PASS: 7 tests |
| Frontend `npm run type-check` | PASS |
| Frontend `npm run build` | PASS, 1013 modules |
| Local healthy-stack HTTP verifier | PASS: 7/7, including nginx API proxy returning expected 401 |
| `git diff --check` | PASS |

The first independent Maven invocation mounted only `sami-backend` and therefore
could not satisfy an existing contract test that reads sibling
`sami-frontend/vite.config.ts`. This was a validation-command error, not a
source failure. The corrected monorepo-root mount passed all 196 tests.

During real script validation, Docker 29's provenance index and Windows
`File.Replace` behavior exposed two implementation defects. Both were corrected
and the affected Build/Export/test gates were rerun successfully.

## Generated artifact evidence

The ignored `deployment-artifacts/` directory contains:

- `sami-backend-test.tar`
- `sami-frontend-test.tar`
- `release-manifest.json`
- timestamped logs

The manifest is regenerated after the report commit so its branch/commit, image
IDs, platform image IDs, build timestamp, filenames, and SHA256 values match the
final deployable branch HEAD. Generated TARs/logs/config are intentionally not
committed.

## Known limitations

- No SSH connectivity check, SCP upload, remote checksum, Docker load, service
  recreation, automatic rollback, or explicit Rollback was executed against the
  VPS during implementation, as required by the no-deploy boundary.
- The remote host needs a modern Docker Compose plugin supporting
  `config --quiet` and `config --no-interpolate`.
- The first deployment cannot roll back when no previous application images
  exist.
- Rollback is application-image-only; it does not reverse Flyway or restore
  database/file data.
- HTTP verification is unauthenticated reachability/proxy validation, not a
  substitute for authenticated role/tenant business-workflow QA.
- This workflow does not add TLS termination, automated database backup/restore,
  infrastructure-as-code, monitoring, or disaster recovery.
- `accept-new` provides trust-on-first-use. Verify the initial server host-key
  fingerprint out of band for high-assurance setup.

## Exact next deployment command

After completing the SSH key setup and confirming non-interactive access:

```powershell
.\scripts\deploy.ps1 -Mode Full -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

Recommended final no-change rehearsal first:

```powershell
.\scripts\deploy.ps1 -Mode Full -DryRun -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

No push or deployment was performed.
