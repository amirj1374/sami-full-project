@echo off
setlocal

if "%~1"=="" (
  echo Usage: release.bat ^<VERSION^>
  exit /b 2
)

if not "%~2"=="" (
  echo Error: exactly one version argument is required.
  echo Usage: release.bat ^<VERSION^>
  exit /b 2
)

set "GIT_BASH=C:\Program Files\Git\bin\bash.exe"
if not exist "%GIT_BASH%" (
  echo Error: Git Bash was not found at "%GIT_BASH%".
  exit /b 1
)

set "ROOT_DIR=%~dp0"
if not exist "%ROOT_DIR%scripts\release-deploy.sh" (
  echo Error: scripts\release-deploy.sh was not found below the launcher directory.
  exit /b 1
)

"%GIT_BASH%" -lc "cd \"$(cygpath -u '%ROOT_DIR%')\" && exec ./scripts/release-deploy.sh \"$1\" --interactive-auth" -- "%~1"
exit /b %errorlevel%
