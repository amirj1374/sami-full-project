# Project overview

## Purpose and users

SAMI ERP is a modular ERP monorepo aimed at businesses selling mobile phones
and technology products. Confirmed application concepts include staff users,
roles and permissions, customers, suppliers, products, purchases, dashboards,
files, automation, calendars, scheduled jobs, licensing/tenancy, metadata,
knowledge, communications, and a customer-portal foundation.

Primary users are operational staff, managers, administrators, and—where the
portal becomes operational—customers.

## Current maturity

The repository is an actively developed modular monolith, not a completed
general-purpose ERP. The following have executable backend and frontend paths
for meaningful scope: authentication, RBAC administration, users, customers,
suppliers, products, purchasing, and dashboards.

Several substantial backend modules have no corresponding production UI:
files, automation, scheduling, licensing, metadata, data quality, knowledge,
and communication. Portal authentication has domain/service/security
foundations but no verified public controller flow.

Inventory/warehouse, sales/orders, pricing/promotions, payments, and accounting
do not have confirmed bounded-context owners. Purchase receiving stores
received quantities and IMEI/serial records, but that is not a stock ledger.

## Architectural characteristics

- Java 21 Spring Boot modular-monolith backend, package-by-domain.
- Vue 3 TypeScript SPA with Vuetify, Pinia, Vue Router, i18n, and typed Axios clients.
- PostgreSQL schema owned exclusively by Flyway.
- JWT staff authentication and database-driven RBAC.
- In-process Spring domain events; communication messages use a durable table
  with an outbox-like dispatch shape.
- Tenant/company/branch structures exist, but tenant resolution is transitional.
- Production topology is PostgreSQL + Spring Boot + nginx-hosted SPA.

## Explicit non-capabilities

Do not assume the repository currently provides a canonical inventory ledger,
sales/order fulfillment, price engine, payment orchestration, double-entry
accounting, production-grade external messaging providers, durable general
event delivery, or complete observability.
