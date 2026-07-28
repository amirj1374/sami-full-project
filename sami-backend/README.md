# SAMI ERP — Backend

Production-ready REST API for the SAMI ERP, built with **Java 21** and
**Spring Boot 3.5**. Layered architecture, DTO pattern, database-driven
permission RBAC, JWT auth, Flyway migrations, and OpenAPI docs.

The companion SPA lives in a separate repository:
[sami-frontend](https://github.com/amirj1374/sami-frontend).

---

## Tech stack

| Area | Technology |
|------|------------|
| Language / runtime | Java 21 (LTS) |
| Framework | Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator) |
| Persistence | Hibernate / Spring Data JPA, PostgreSQL |
| Migrations | Flyway (`V1` … `V9`) |
| Auth | JWT (access + refresh), BCrypt, database-driven permission RBAC |
| Docs | OpenAPI / Swagger UI (springdoc) |
| Build | Maven |
| Boilerplate | Lombok |

---

## Requirements

- **Docker** + Docker Compose (recommended path — no local JDK needed), **or**
- **JDK 21** + **Maven 3.9+** + a running **PostgreSQL 16** (local path).

---

## Quick start (Docker — recommended)

Spins up PostgreSQL and the API together; Flyway runs all migrations and a
default admin is seeded on first boot.

```bash
cp .env.example .env      # adjust secrets if you like
docker compose up --build
```

- **API base:** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health

Stop with `Ctrl+C`; remove volumes with `docker compose down -v`.

---

## Quick start (local, without Docker)

1. **Start PostgreSQL** and create the database:
   ```bash
   createdb sami        # or: docker run --name sami-db -e POSTGRES_DB=sami \
                        #     -e POSTGRES_USER=sami -e POSTGRES_PASSWORD=sami \
                        #     -p 5432:5432 -d postgres:16-alpine
   ```
2. **Export the connection settings** (or rely on the defaults in
   `application.yml`, which already point at `localhost:5432/sami`, user/pass
   `sami`/`sami`):
   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/sami
   export DB_USERNAME=sami
   export DB_PASSWORD=sami
   export JWT_SECRET=a-long-random-dev-secret-at-least-32-bytes
   ```
3. **Run** (Flyway migrates on startup):
   ```bash
   ./mvnw spring-boot:run        # or: mvn spring-boot:run
   ```

> No Maven installed? Use the wrapper `./mvnw` (Linux/macOS) / `mvnw.cmd`
> (Windows) if present, otherwise install Maven 3.9+.

---

## First login

On first boot (empty `users` table) a super-admin is seeded from the
`BOOTSTRAP_ADMIN_*` variables. Defaults:

- **Email:** `admin@sami.local`
- **Password:** `Admin123!`

Authenticate and grab a token:

```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sami.local","password":"Admin123!"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` on protected
endpoints, or paste it into Swagger UI's **Authorize** dialog.

> Change these credentials for anything beyond local development.

---

## Configuration

All settings come from environment variables (see `.env.example`); the most
important ones:

| Variable | Default | Purpose |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (verbose, SQL echo) or `prod` (quiet) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | localhost / `sami` / `sami` | PostgreSQL connection |
| `JWT_SECRET` | dev placeholder | HMAC secret (≥ 32 bytes) — **set a strong value** |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | 900 / 1209600 | Token lifetimes (seconds) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed frontend origin(s) |
| `BOOTSTRAP_ADMIN_EMAIL` / `_PASSWORD` | `admin@sami.local` / `Admin123!` | Seed admin |
| `STORAGE_PROVIDER` / `STORAGE_BASE_PATH` | `local` / `./data/uploads` | File storage (avatars, documents) |

Schema is owned by **Flyway** (`ddl-auto: validate`); Hibernate never alters
tables. Migrations live in `src/main/resources/db/migration`.

---

## Modules & API

RESTful API under `/api/v1`, every response wrapped in a standard envelope
(`{ success, data, error, timestamp }`), with pagination, filtering and sorting
on list endpoints.

| Module | Base path | Highlights |
|--------|-----------|------------|
| Auth | `/auth` | register, login, refresh (rotating), logout, change/forgot/reset password |
| Users | `/users`, `/user-statuses`, `/profile-fields` | lifecycle, configurable statuses, custom fields, avatar, audit |
| Authorization | `/roles`, `/permissions`, `/modules`, `/menu` | permission-based RBAC, dynamic menu |
| Products | `/products` | sample CRUD |
| Customers (CRM) | `/customers`, `/crm/…` | 360° profiles, timeline, dedup, merge, blacklist, segments |
| Suppliers | `/suppliers`, `/supplier-config/…` | registry, contacts, banking, weighted evaluation, documents |
| Purchasing | `/purchases`, `/purchasing/…` | drafts, approval workflow, receiving (IMEI/serial), returns |

Full interactive reference: **Swagger UI** at `/swagger-ui.html`.

Security model: JWT carries identity only; permissions are loaded from the
database per request (role → permissions), so permission changes take effect
immediately. Every endpoint declares its required permission via `@authz.has(...)`.

---

## Testing & build

```bash
mvn test            # unit tests
mvn clean package   # build the executable jar into target/
```

---

## Project layout

```
src/main/java/com/sami/app/
├── common/       # ApiResponse envelope, exceptions, storage port, dynamic fields
├── config/       # security, OpenAPI, properties, bootstrap admin
├── security/     # JWT filter/service, @authz bean, SecurityUser
├── auth/         # authentication flows + refresh/reset tokens
├── user/         # user management (statuses, profile fields, audit)
├── authz/        # roles, permissions, modules, menu
├── product/      # sample product catalogue
├── crm/          # customer management
├── supplier/     # supplier management
└── purchasing/   # purchasing lifecycle
src/main/resources/
├── application*.yml
└── db/migration/ # Flyway V1..V9
```

---

## License

Proprietary — © SAMI ERP.
