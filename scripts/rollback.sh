#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
REQUESTED_VERSION="${1:-history}"
if [[ "$REQUESTED_VERSION" != history && ! "$REQUESTED_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
  echo "Usage: $0 [previous-version]" >&2
  exit 2
fi

if command -v pwsh.exe >/dev/null 2>&1; then POWERSHELL="pwsh.exe";
elif command -v powershell.exe >/dev/null 2>&1; then POWERSHELL="powershell.exe";
else echo "PowerShell is required (Windows PowerShell or PowerShell 7)." >&2; exit 1; fi

echo "Rolling back application images from the latest verified deployment history; database and volumes are untouched."
exec "$POWERSHELL" -NoProfile -ExecutionPolicy Bypass -File "$ROOT_DIR/scripts/deploy.ps1" -Mode Rollback
