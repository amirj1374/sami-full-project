# New Windows workstation setup

This checklist prepares a Windows development machine without storing secrets.
Repository-required versions come from `pom.xml`, `package-lock.json`, Docker
files and Compose configuration.

## Required tools

| Tool | Requirement | Recommended official source |
|---|---|---|
| Git | Current supported Git for Windows | https://git-scm.com/download/win |
| Node.js/npm | Node compatible with Vite (`^20.19.0` or `>=22.12.0`); production uses Node 22 | https://nodejs.org/ |
| Java | JDK 21 | Microsoft Build of OpenJDK, Eclipse Temurin or another supported JDK 21 distribution |
| Maven | Maven 3.9+ | https://maven.apache.org/download.cgi |
| Docker | Docker Desktop using Linux containers | https://docs.docker.com/desktop/setup/install/windows-install/ |
| Compose/Buildx | Docker Desktop plugins | Installed and updated with Docker Desktop |
| OpenSSH Client | Windows optional capability | Windows Settings → Optional Features → OpenSSH Client |
| IntelliJ IDEA | Current version with Java 21 support | https://www.jetbrains.com/idea/download/ |

Prefer installer or operating-system package-manager methods from the official
vendor. Do not copy JDK/Maven binaries from an unknown workstation and do not
change repository dependencies to accommodate an older local runtime.

## Java, Maven and IntelliJ

1. Install a JDK 21 distribution.
2. Set `JAVA_HOME` to the JDK 21 directory and put `%JAVA_HOME%\bin` before old Java shims on `PATH`.
3. Extract/install Maven 3.9+ and add its `bin` directory to `PATH`.
4. In IntelliJ, set Project SDK and Maven importer/runner JRE to the same JDK 21.
5. Import `sami-backend/pom.xml`; do not add a Maven wrapper or alter the POM merely for workstation setup.

## Docker and PostgreSQL strategy

The recommended development database is PostgreSQL 16 from
`sami-backend/docker-compose.yml`; it avoids machine-specific database
installation drift. Docker Desktop must use Linux containers with Compose v2
and Buildx available.

If Docker cannot be used, install PostgreSQL 16 separately, create an isolated
local database, and supply `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` through
local environment configuration. Never point development startup or Flyway at
an unidentified/shared database.

## Repository-local configuration

1. Copy `sami-backend/.env.example` to the ignored `sami-backend/.env` only when using Compose.
2. Use `sami-frontend/.env.example` as the frontend variable catalogue; defaults work for normal local development.
3. Replace placeholder secrets locally. Never commit `.env`, private keys, deployment artifacts or `scripts/release-config.ps1`.
4. Run `npm ci` in `sami-frontend`; the lockfile is authoritative.

See `PROJECT_SETUP_AND_DEPLOYMENT.md` for the startup sequence and
`docs/15-testing-and-quality.md` for canonical validation commands.

## Verification commands

Open a new PowerShell session after installation:

```powershell
java -version
mvn -version
docker info
docker compose version
docker buildx version
ssh -V
node -v
npm -v
git --version
```

Expected minimums are Java 21, Maven 3.9+, a functioning Linux Docker engine,
Compose v2, Buildx, OpenSSH Client and a Vite-compatible Node release. Confirm
that `mvn -version` reports Java 21 rather than an older system Java.

## Baseline validation

After configuration:

```powershell
cd sami-frontend
npm ci
npm test
npm run type-check
npm run build

cd ../sami-backend
mvn clean verify
docker compose config
docker compose up --build
```

Then verify `http://localhost:8080/actuator/health` and
`http://localhost:7474`. Do not run deployment automation until the repository
is clean, synchronized on `development`, SSH key authentication is approved and
the deployment guide has been read.
