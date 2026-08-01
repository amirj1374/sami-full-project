# SAMI ERP deployment automation

This runbook is the canonical guide for building verified SAMI images on
Windows and deploying those exact images to the Linux VPS. The default command
is local-only `Build`; nothing connects to the server unless `Upload`, `Deploy`,
`Full`, or `Rollback` is selected.

## Safety model

- The release source must be the exact `development` branch with a clean
  working tree. `-AllowDirtyWorkingTree` is an explicit, logged exception.
- Images are built for `linux/amd64`, labeled with branch, full commit, and
  application version, and rejected when their metadata differs from HEAD.
- TARs are written under temporary names and published only after both image
  exports and SHA256 calculations succeed.
- SCP uploads use `.uploading` names. Remote names become final only after all
  three files arrive.
- Remote SHA256 values must match the local manifest before `docker load`.
- Deployment records the previous application image IDs and non-secret Compose
  state under `/root/sami-deployment-history/<UTC timestamp>/`.
- Only `backend` and `frontend` are force-recreated. The database container and
  named volumes are never removed. The script never runs `down -v`.
- A failed health/smoke gate automatically retags and restores the previous
  backend/frontend images when both still exist.
- Logs never intentionally contain passwords, JWT secrets, `.env` contents, or
  private-key contents. Remote failure output passes through secret redaction.

## Prerequisites

On the Windows build computer:

- Windows PowerShell 5.1 or later
- Git
- Docker Desktop using Linux containers
- Docker buildx
- Windows OpenSSH `ssh.exe` and `scp.exe`
- enough free space for two images and two TAR exports

On the VPS:

- Linux amd64 with Docker Engine and the Docker Compose plugin
- the tracked monorepo at `/root/sami-full-project`
- production Compose directory `/root/sami-full-project/sami-backend`
- a protected `/root/sami-full-project/sami-backend/.env`
- port 80 available for the frontend and the configured SSH port reachable

The server `.env` remains the only owner of database, JWT, bootstrap, and other
runtime secrets. The automation neither uploads nor reads it.

## SSH key setup on Windows

Normal deployment is deliberately non-interactive. OpenSSH may use keys from
the standard `$HOME\.ssh` directory or keys loaded into `ssh-agent`. No password
parameter exists, and passwords must never be put in commands or config files.

### 1. Generate a dedicated Ed25519 key

Run these exact commands in Windows PowerShell. Choose a strong key passphrase
when prompted; the passphrase is held by you/ssh-agent, not the deployment
script.

```powershell
$sshDirectory = Join-Path $HOME '.ssh'
$samiKey = Join-Path $sshDirectory 'sami_vps_ed25519'
New-Item -ItemType Directory -Path $sshDirectory -Force | Out-Null
ssh-keygen -t ed25519 -a 100 -f $samiKey -C 'sami-deployment'
```

This creates the private key `~/.ssh/sami_vps_ed25519` and public key
`~/.ssh/sami_vps_ed25519.pub`. Never copy the private key to the VPS.

### 2. Install the public key on the VPS

The following first-time command sends only the public key. OpenSSH may prompt
interactively for the server account during this one supervised setup command;
the credential is not placed in the command line or a file.

```powershell
Get-Content -LiteralPath "$samiKey.pub" |
  ssh -p 9011 root@87.248.131.157 "umask 077; mkdir -p ~/.ssh; cat >> ~/.ssh/authorized_keys; chmod 700 ~/.ssh; chmod 600 ~/.ssh/authorized_keys"
```

Run the install command once for a newly generated key. On the server, remove
duplicate public-key lines manually if it was repeated.

### 3. Test non-interactive key access

```powershell
ssh -p 9011 -i $samiKey `
  -o BatchMode=yes `
  -o PasswordAuthentication=no `
  -o KbdInteractiveAuthentication=no `
  root@87.248.131.157 'echo key-auth-ok'
```

The output must be `key-auth-ok` without a server-password prompt. Full
deployment fails before upload when this non-interactive test cannot succeed.

### 4. Optional Windows ssh-agent setup

Run the `Set-Service` command in an elevated PowerShell window if the service is
disabled, then load the key in your normal session:

```powershell
Set-Service -Name ssh-agent -StartupType Automatic
Start-Service -Name ssh-agent
ssh-add $samiKey
ssh-add -l
```

When the agent has the key, `-IdentityFile` is optional. To select the key
directly:

```powershell
.\scripts\deploy.ps1 -Mode Full -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

`-AllowInteractiveAuth` is an explicit exception for supervised first setup or
emergency access. It only lets OpenSSH prompt normally; the script still has no
password argument, never records the prompt response, and never creates a
credential file. Omit it for every routine/automated deployment.

## Configuration

Defaults match the current SAMI test environment:

| Setting | Default |
|---|---|
| SSH host | `87.248.131.157` |
| SSH port/user | `9011` / `root` |
| remote artifacts | `/root` |
| remote Compose directory | `/root/sami-full-project/sami-backend` |
| Compose/environment files | `docker-compose.prod.yml` / `.env` |
| backend/frontend images | `sami-backend:test` / `sami-frontend:test` |
| frontend API base | `/api` |

Copy the example and edit only non-secret settings:

```powershell
Copy-Item .\scripts\release-config.example.ps1 .\scripts\release-config.ps1
.\scripts\deploy.ps1 -Mode Build -ConfigFile .\scripts\release-config.ps1
```

`scripts/release-config.ps1` is ignored by Git. The file returns a PowerShell
hashtable and is executed by the script, so use only a locally reviewed file.
Do not put application secrets, server passwords, or key contents in it.
Explicit command-line parameters override config-file entries.

## First-time validation

From the monorepo root on clean `development`:

```powershell
git status --short --branch
docker version
docker buildx version
Get-Help .\scripts\deploy.ps1 -Full
.\scripts\deploy.ps1 -Mode Full -DryRun -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

`-DryRun` checks the local repository/tools and prints every planned phase. It
does not build/export, connect to SSH, upload, load images, recreate containers,
clean files, or modify the VPS.

## Commands

Build both local images (the safe default):

```powershell
.\scripts\deploy.ps1
# equivalent:
.\scripts\deploy.ps1 -Mode Build
```

Force a clean layer build:

```powershell
.\scripts\deploy.ps1 -Mode Build -NoCache
```

Export already verified current-HEAD images and create the manifest:

```powershell
.\scripts\deploy.ps1 -Mode Export
```

Upload existing verified artifacts without deployment:

```powershell
.\scripts\deploy.ps1 -Mode Upload -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

Deploy artifacts already present both locally and remotely:

```powershell
.\scripts\deploy.ps1 -Mode Deploy -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

Build, export, upload, checksum, deploy, and verify end to end:

```powershell
.\scripts\deploy.ps1 -Mode Full -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

Skip SCP only when the exact manifest/TARs have already been safely uploaded:

```powershell
.\scripts\deploy.ps1 -Mode Full -SkipUpload -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

## Manifest and checksum verification

Local outputs:

```text
deployment-artifacts/
  sami-backend-test.tar
  sami-frontend-test.tar
  release-manifest.json
  logs/
    deployment-<UTC timestamp>-<mode>.log
```

The JSON manifest records branch, full/short commit, UTC build timestamp, tags,
image IDs, available digests, filenames, and SHA256 values. Inspect or recheck:

```powershell
Get-Content .\deployment-artifacts\release-manifest.json
Get-FileHash .\deployment-artifacts\sami-backend-test.tar -Algorithm SHA256
Get-FileHash .\deployment-artifacts\sami-frontend-test.tar -Algorithm SHA256
```

Upload uses `/root/<filename>.uploading`, then renames all files to their final
names. Deploy calculates `sha256sum` remotely and aborts before `docker load` if
either value differs.

## Remote deployment history and rollback

Each deploy creates:

```text
/root/sami-deployment-history/<UTC timestamp>/
  previous-images.env
  pre-deployment-compose-ps.txt
  effective-compose-no-secrets.yml
  compose-services.txt
  compose-images.txt
  compose-source.yml
  release-manifest.json
```

The effective Compose snapshot uses `config --no-interpolate`, so it preserves
variable expressions instead of secret values; `.env` contents are never copied.
A failed deployment automatically attempts application-image rollback. To
explicitly use the newest valid entry:

```powershell
.\scripts\deploy.ps1 -Mode Rollback -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
```

Rollback requires both old image IDs to still exist on the VPS. It retags and
recreates backend/frontend only. Flyway/database changes are never reversed;
releases must therefore keep migrations backward-compatible with the preceding
application image.

## Cleanup

Cleanup is local, narrow, and preview-first. It may select old non-current
artifacts/logs and non-running `sami-backend:build-check-*` or
`sami-frontend:build-check-*` tags. It never removes the current release
artifacts/tags, running images, volumes, rollback history, source, or worktrees.

```powershell
.\scripts\deploy.ps1 -Mode Cleanup -DryRun -RetentionDays 30
.\scripts\deploy.ps1 -Mode Cleanup -RetentionDays 30
# Only after reviewing the printed candidate list:
.\scripts\deploy.ps1 -Mode Cleanup -RetentionDays 30 -ConfirmCleanup
```

No cleanup occurs without `-ConfirmCleanup`.

## Verification and troubleshooting

Run HTTP smoke checks independently:

```powershell
.\scripts\verify-deployment.ps1 -BaseUrl http://87.248.131.157
```

The checks cover `/`, `/health`, `/auth/login`, `/sales`, `/automations`,
`/licensing`, and an unauthenticated `/api` request that proves nginx reaches
Spring rather than returning a proxy/SPA response. They do not log in or mutate
business data.

Common failures:

- **Wrong branch/dirty worktree:** switch to and synchronize `development`, then
  commit or stash reviewed work. Avoid bypassing the gate for release builds.
- **Docker unavailable:** start Docker Desktop in Linux-container mode and check
  `docker version` plus `docker buildx version`.
- **Image metadata mismatch:** run `-Mode Build`; Export refuses an image not
  labeled with the current commit/version.
- **Key authentication failed:** repeat the non-interactive SSH test, check
  `ssh-add -l`, permissions on `~/.ssh/authorized_keys`, and `-IdentityFile`.
- **Checksum mismatch:** do not deploy. Remove only the remote partial/final TAR
  concerned and repeat Upload from verified local artifacts.
- **Compose validation failed:** verify the tracked server Compose revision and
  protected `.env`; never print the `.env` while diagnosing.
- **Unhealthy backend/frontend:** the script prints sanitized status/log tails
  and attempts application-image rollback. Inspect the saved history record.
- **First deployment rollback unavailable:** no previous application image can
  exist yet; correct the failure and redeploy without touching data volumes.

## Security notes and limitations

- Routine `Full` deployment is non-interactive after key setup. Key passphrases
  should be managed by `ssh-agent`; private keys remain local.
- Host-key policy is `accept-new`: the first key is recorded automatically, but
  a changed server host key fails. For high-assurance use, verify the initial
  host-key fingerprint out of band before the first run.
- HTTP route checks prove reachability and proxy behavior, not authenticated
  authorization or business workflows. Use the release gate separately.
- TLS termination is not introduced by this workflow. Production customer data
  requires the separately approved HTTPS/domain hardening.
- Application-image rollback does not reverse Flyway migrations or restore
  database/file data. Database backup/restore remains an independent controlled
  operation.
