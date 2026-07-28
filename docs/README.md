# SAMI ERP technical documentation

This directory is the canonical, evidence-based technical knowledge base for
SAMI ERP. Current source and configuration override documentation if they
disagree. Status labels are defined in
[Module catalog](05-module-catalog.md): **Confirmed**, **Partially implemented**,
**Planned**, **Open decision**, **Historical**, and **Deprecated**.

## Reading paths

| Audience | Recommended order |
|---|---|
| New developer | [Overview](00-project-overview.md) → [Getting started](01-getting-started.md) → [Repository map](02-repository-map.md) → [Contribution guide](20-contribution-guide.md) |
| Backend developer | [Architecture](03-system-architecture.md) → [Backend](14-backend-architecture.md) → [Database](07-database-and-migrations.md) → [API](08-api-and-integrations.md) → [Security](18-security.md) |
| Frontend developer | [Frontend](13-frontend-architecture.md) → [API](08-api-and-integrations.md) → [Auth/RBAC](09-authentication-and-authorization.md) → [Testing](15-testing-and-quality.md) |
| QA engineer | [Workflows](workflows/README.md) → [Testing](15-testing-and-quality.md) → [Risks](23-known-risks-and-technical-debt.md) |
| DevOps/operator | [Configuration](16-configuration-and-environments.md) → [Operations](17-deployment-and-operations.md) → [Runbooks](runbooks/README.md) → [Observability](19-observability.md) |
| Product/domain analyst | [Overview](00-project-overview.md) → [Domain model](04-domain-model.md) → [Modules](modules/README.md) → [Open decisions](24-roadmap-and-open-decisions.md) |
| AI coding agent | [AGENTS.md](../AGENTS.md) → [AI guide](21-ai-agent-guide.md) → [Repository map](02-repository-map.md) → task-specific module/workflow documentation |

## Index

- [Project overview](00-project-overview.md)
- [Getting started](01-getting-started.md)
- [Repository map](02-repository-map.md)
- [System architecture](03-system-architecture.md)
- [Domain model](04-domain-model.md)
- [Module catalog](05-module-catalog.md) and [module notes](modules/README.md)
- [Business workflows](06-business-workflows.md) and [workflow details](workflows/README.md)
- [Database and Flyway](07-database-and-migrations.md)
- [API and integrations](08-api-and-integrations.md)
- [Authentication and authorization](09-authentication-and-authorization.md)
- [Tenancy and organization](10-multi-tenancy-and-organization.md)
- [Events, automation, and scheduling](11-events-automation-and-scheduling.md)
- [Files and communications](12-files-and-communications.md)
- [Frontend architecture](13-frontend-architecture.md)
- [Backend architecture](14-backend-architecture.md)
- [Testing and quality](15-testing-and-quality.md)
- [Configuration](16-configuration-and-environments.md)
- [Deployment and operations](17-deployment-and-operations.md)
- [Security](18-security.md)
- [Observability](19-observability.md)
- [Contribution guide](20-contribution-guide.md)
- [AI agent guide](21-ai-agent-guide.md)
- [Glossary](22-glossary.md)
- [Risks and debt](23-known-risks-and-technical-debt.md)
- [Roadmap and open decisions](24-roadmap-and-open-decisions.md)
- [Licensing product decisions](decisions/licensing-product-decisions.md)
- [ADRs](adr/README.md)
- [Runbooks](runbooks/README.md)

## Maintenance

Update the smallest owning document with every contract or architectural
change. Link to source paths instead of copying implementation. Mark facts that
cannot be verified and never promote scaffolding to implemented status.
