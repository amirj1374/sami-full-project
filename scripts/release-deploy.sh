#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"

if [[ "$#" -ne 1 ]]; then
  echo "Usage: $0 <semver> (for example: 0.4.0)" >&2
  exit 2
fi

VERSION="$1"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
  echo "Usage: $0 <semver> (for example: 0.4.0)" >&2
  exit 2
fi

DEPLOY_SCRIPT="$ROOT_DIR/scripts/deploy.ps1"
if [[ ! -f "$DEPLOY_SCRIPT" ]]; then
  echo "Release deployment script is missing: $DEPLOY_SCRIPT" >&2
  exit 1
fi

if command -v pwsh.exe >/dev/null 2>&1; then
  POWERSHELL="pwsh.exe"
elif command -v powershell.exe >/dev/null 2>&1; then
  POWERSHELL="powershell.exe"
else
  echo "PowerShell is required (Windows PowerShell or PowerShell 7)." >&2
  exit 1
fi

ARGS=(-NoProfile -ExecutionPolicy Bypass -File "$DEPLOY_SCRIPT" -Mode Full -ApplicationVersion "$VERSION")
if [[ -f "$ROOT_DIR/scripts/release-config.ps1" ]]; then
  ARGS+=( -ConfigFile "$ROOT_DIR/scripts/release-config.ps1" )
fi

exec "$POWERSHELL" "${ARGS[@]}"
