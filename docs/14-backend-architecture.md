# Backend architecture

The backend is a Java 21 Spring Boot 3.5.3 modular monolith organized by domain
under `com.sami.app`.

## Layer responsibilities

- `web`: REST controllers and transport concerns.
- `dto`: request/response contracts; do not expose entities directly.
- `service`: transactions, invariants, lifecycle and authorization-sensitive work.
- `repository`: JPA access and specifications.
- `domain`: entities, value/status types and domain state.
- `event`: module event contracts.
- `spi`/`provider`: extension contracts and implementations.
- `common`: genuinely shared infrastructure, not a miscellaneous domain layer.

Controllers validate inputs and delegate. Services define transaction
boundaries. Repositories must apply tenant scope. Central exceptions produce
consistent API errors. Sensitive operations use method security even when a
controller is already authenticated.

## Adding a capability safely

1. Identify its bounded-context owner and comparable module.
2. define DTOs, lifecycle invariants, permissions and tenant scope.
3. Add a new forward migration; never let Hibernate create schema.
4. Implement entity/repository/service/controller in that order.
5. Publish minimal stable events after durable state changes.
6. Keep provider-specific code behind an SPI.
7. Add unit, PostgreSQL integration, authorization and contract tests.
8. Synchronize frontend types/client/mocks/locales.

Avoid cross-module repository access. Prefer explicit public services or
integration events, and document any unavoidable dependency.
