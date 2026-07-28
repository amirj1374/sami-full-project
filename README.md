# SAMI ERP

SAMI ERP is maintained as a single monorepo:

- `sami-backend` — Java 21 / Spring Boot / PostgreSQL / Flyway API
- `sami-frontend` — Vue 3 / TypeScript / Vuetify SPA
- `.agents` — repository-scoped Codex skills and reusable project context

The integration branch is `development`.

The canonical technical knowledge base is [docs/README.md](docs/README.md).
Start there for architecture, module status, workflows, operations, security,
known risks, and AI-agent guidance.

Confirmed incomplete work and open decisions are tracked in the
[project backlog](PROJECT_BACKLOG/README.md).

## Clone

```bash
git clone --branch development https://github.com/amirj1374/sami-full-project.git
cd sami-full-project
```

No submodules or additional application repositories are required.

## Local development

Backend prerequisites: JDK 21, Maven 3.9+, PostgreSQL 16.

```bash
cd sami-backend
cp .env.example .env
mvn spring-boot:run
```

Frontend prerequisites: Node.js compatible with the locked Vite version.

```bash
cd sami-frontend
npm ci
npm run dev
```

The frontend development proxy targets `http://localhost:8080` by default.

## Docker

Run the complete production-style stack from the backend directory:

```bash
cd sami-backend
cp .env.example .env
docker compose -f docker-compose.prod.yml up --build
```

Set strong production values for `POSTGRES_PASSWORD`, `JWT_SECRET`,
`PORTAL_JWT_SECRET`, and `BOOTSTRAP_ADMIN_PASSWORD`.

## Verification

```bash
cd sami-backend
mvn clean verify

cd ../sami-frontend
npm ci
npm run type-check
npm run build
```

GitHub Actions runs the same backend and frontend gates on pushes and pull
requests targeting `development`.

## Codex guidance

Read [`AGENTS.md`](AGENTS.md) first. The reusable technical snapshot is
[`.agents/SAMI_PROJECT_CONTEXT.md`](.agents/SAMI_PROJECT_CONTEXT.md).
Repository files remain authoritative when that snapshot becomes stale.
