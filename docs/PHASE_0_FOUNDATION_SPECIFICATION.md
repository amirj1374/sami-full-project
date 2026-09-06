# Phase 0 — Foundation Implementation Specification

**Status:** Proposed for review and approval.  
**Architecture baseline:** SAMI ERP Architecture Constitution v1.0, READY FOR FREEZE.  
**Implementation boundary:** This document is an approved-work prerequisite, not authorization to change production code or schema.

## 1. Objective

Make Tenant -> Company -> Branch, shared Contact identity, and auditable authorization real backend-enforced foundations while preserving the current tenant-scoped Company, user/RBAC, Customer, Supplier, Sales, Purchasing, Inventory, Treasury, and audit data.

## 2. Scope

Included: Organization/Company/Branch, User/Role/Permission, Company-specific role assignment, explicit Branch access, trusted active Company/Branch context, shared Contact and Customer/Supplier roles, audit foundation, and backend scope enforcement.

Excluded: Sales/Purchasing document redesign, Variant/UOM/Price List, Accounting/GL, tax, CRM intelligence, reports, Asan final promotion, and destructive migrations.

## 3. Existing Implementation Inventory

| Area | Existing Implementation | Target Architecture | Compatibility | Action |
|---|---|---|---|---|
| Tenant context | TenantContext derives tenant only from authenticated SecurityUser and fails closed. | Trusted Tenant -> Company -> Branch context. | Strong reusable tenant base. | KEEP |
| Company | Tenant-scoped Company repository/service/controller, RBAC, UI, audit table, and isolation test exist. | Company is active authorization context. | Preserve CRUD; add grants/context. | COMPLETE |
| Branch | V22 schema has mandatory company_id; no Branch entity/repository/service/controller/UI exists. | Branch belongs to one Company and is explicit access context. | Preserve V22 tables and references. | COMPLETE |
| Company/Branch integrity | Separate tenant FKs and Branch -> Company FK exist; some later services validate with direct SQL. | Tenant consistency and document scope are centrally enforced. | Existing rows stay valid. | REFACTOR |
| User / Role / Permission | User has one global Role; Role has permissions; method security uses Authz. | User may have Company-specific roles and explicit Branch grants. | Preserve permission catalog and compatible role path. | MIGRATE |
| Backend authorization | JWT reloads persisted User/Role/permissions; method security is enabled. | Permission plus Company/Branch grant is authoritative server-side. | Reuse JWT, Authz, guards. | COMPLETE |
| Frontend authorization | Pinia auth store, route metadata, permission directive/composable, Axios bearer interceptor exist. | Frontend mirrors trusted context, never enforces it. | Preserve UI permission behavior. | COMPLETE |
| Active Company / Branch | No shared store, selector, API context, request header, or backend validator found. Forms accept raw IDs. | Active context is selected from verified grants and validated server-side. | Additive API/client state required. | COMPLETE |
| Customer identity | Tenant-scoped Customer has contacts, duplicate candidates, controlled merge, timeline and merge events. | Contact is shared canonical identity with Customer role. | Preserve Customer IDs/history/FKs. | MIGRATE |
| Supplier identity | Separate Supplier model owns legal/contact/address/bank data; it retains source_customer_id. | Supplier is a role on Contact. | Preserve Supplier IDs and Purchasing references. | MIGRATE |
| Duplicate / merge | Customer merge is permission-guarded and tenant-scoped; source remains resolvable. Supplier rules are separate. | One canonical Contact; no uncertain auto-merge. | Reuse merge pattern, not owner. | REFACTOR |
| Audit | User audit has actor and old/new JSON; CRM timeline has actor/detail; organization audit logs create/update. | Foundation audit covers grants, context and Contact merge consistently. | Preserve module logs; add common Foundation record. | COMPLETE |

## 4. Target Architecture

1. TenantContext remains the trusted root.
2. Active Company and Branch are valid only if they belong to the tenant and the user has explicit positive grants.
3. A Company role grants permissions only inside that Company and its granted Branches. Missing grant means deny.
4. Contact owns real-world identity/common identity facts. Customer and Supplier are commercial roles on Contact.
5. Existing Customer/Supplier identifiers and historical references remain resolvable during migration.
6. Backend method security and trusted scope guards are authoritative; frontend checks are presentation only.

## 5. Gap Analysis

### P0 — Must be solved in Phase 0

1. **Company/Branch authorization is absent.** Branch has no application flow and no user-Company-role or user-Branch-grant records.
2. **Active context is absent.** Axios attaches only a bearer token; raw Company/Branch IDs are accepted in forms without a shared context or grant validation.
3. **Roles are global and User administration is not uniformly tenant-scoped.** User stores one role_id; RoleService and most UserService operations do not use TenantContext.
4. **Contact is absent.** Customer and Supplier duplicate core identity/contact data and cannot represent one identity with both roles safely.
5. **Foundation audit is incomplete.** No common append-only evidence covers role/grant/context changes or cross-role Contact merge with before/after/reason.

### P1 — Should be solved in Phase 0

1. Centralize repeated ad-hoc Company/Branch checks behind a reusable server scope service after characterization tests.
2. Add Branch API/UI using V22 and the organization permission family.
3. Expose migration/read compatibility and reconciliation evidence to administrators.

### P2 — Can wait

1. Field-level permissions, delegated approvals, and branch-capability workflow policy.
2. Cross-company/intercompany workflow; current scope remains one operating company.

## 6. Required Changes

### Organization and authorization

- Complete Branch application ownership over existing V22 schema and verify same-tenant Company/Branch membership.
- Add additive Company-role assignments and explicit Branch grants. Never infer grants from defaults, record existence, role name, or frontend state.
- Add trusted active-context resolution; stale or manually altered context must be denied.
- Retain global permission definitions and Authz permission checks while removing implicit global reach.
- Scope User administration with TenantContext before using it to manage grants.

### Contact and roles

- Add tenant-scoped Contact for authoritative identifiers and common communication facts.
- Add Customer and Supplier role records linked to Contact; retain role-specific policy fields.
- Preserve legacy IDs, timeline, logs, documents, Sales/Purchase/Treasury references and audit evidence through mapping/provenance and dual-read.
- Move merge ownership to Contact only after history/references can be retained and audited.

### Audit

- Add append-only Foundation audit: tenant, Company, optional Branch, actor, subject type/id, action, timestamp, reason/context, and before/after where applicable.
- Write it transactionally with grants, role assignments, context/default changes, Contact identity changes and merges.
- Preserve existing module audit tables.

## 7. Components Affected

| Component | Phase 0 treatment |
|---|---|
| common tenancy/TenantContext | Preserve tenant root; extend through a distinct organization-scope collaborator. |
| security/Authz, SecurityUser, JWT, SecurityConfig | Preserve permission mechanism; complete scope-aware inputs. |
| organization package and V22/V48 data | Complete Branch and active-context capability; retain Company CRUD/audit. |
| user and authz packages | Migrate global one-role model compatibly to Company-scoped assignments. |
| crm and supplier packages | Migrate identity ownership to Contact with dual-read/provenance. |
| Sales/Purchasing/Inventory/Treasury | Inspect/adapt scope boundary only; no later-phase workflow work. |
| frontend auth store, API HTTP, router, Organization/Customer/Supplier/Admin views | Add active context and grant-aware presentation only after backend contract exists. |

## 8. Database Changes

All changes are new forward Flyway migrations after V50. Never alter V22, V48, customer, supplier, user, role, or audit migrations already applied.

- Add Branch integrity constraints/indexes needed to prove Tenant -> Company -> Branch consistency.
- Add Company-role assignment and explicit Branch-grant tables, including tenant, lifecycle/audit fields, unique constraints and scope indexes.
- Persist active context only as a revalidated preference, never as authority.
- Add Contact, Customer-role, Supplier-role and legacy mapping/provenance tables additively.
- Add Foundation audit table/indexes that preserve evidence after deactivation or soft deletion.

## 9. Migration Strategy

### Customer / Supplier -> Contact

| Element | Strategy |
|---|---|
| Current | Tenant-scoped Customer and separate Supplier overlap identity/contact fields; Purchases can use Supplier or Customer seller. |
| Target | Contact owns identity; Customer/Supplier roles link to Contact. |
| Mapping | Create one Contact per legacy record. Match only exact approved identifiers inside a tenant; uncertain similarity creates a review candidate, never an automatic merge. |
| Preservation | Keep legacy IDs, timeline/events, supplier logs/documents/bank data, Sales/Purchase/Treasury references and audit rows with source mapping/provenance. |
| Compatibility | Dual-read role resolution; existing APIs retain legacy IDs until consumers migrate. |
| Reconciliation | Per-tenant counts/mappings; identifier/contact/history coverage; no unexplained unmapped rows before cutover. |
| Rollback | Disable new preference; retain mappings/legacy data; correct through audited compensating links, never bulk deletion. |

### Company / Branch access and roles

| Element | Strategy |
|---|---|
| Current | Company is tenant-scoped; Branch schema exists; User has one global Role; no grants/context. |
| Target | Explicit Company role plus Branch grant and server-validated active context. |
| Mapping | Seed only verified same-tenant user/company/branch assignments; no implicit cross-company grant. |
| Preservation | Retain User/Role/permission rows during compatibility period. |
| Compatibility | Global Role may remain a permission template but cannot itself authorize scoped data after Phase 0 enforcement. |
| Reconciliation | Every enabled user has valid same-tenant assignments; every Branch grant belongs to assigned Company. |
| Rollback | Feature-gated enforcement after verified grants; revoke/repair erroneous grants with Foundation audit; do not remove legacy role rows initially. |

### Audit/history

Do not consolidate or delete existing audit tables. Foundation audit links new authorization/identity actions and reconciliation verifies actor, subject, action and timestamp coverage.

## 10. API and Security Changes

- Add authenticated context discovery/select APIs that return only caller-granted Company/Branch choices.
- Define one validated backend context contract before client changes; naked IDs/headers never grant access.
- Add permission-guarded Branch and assignment/grant APIs with service-layer enforcement.
- Extend current-user responses compatibly only after grants/context are server-verified.
- Retain PreAuthorize/permission checks; frontend route guards/buttons remain presentation only.

## 11. Frontend Changes

- Add active organization store sourced from authenticated API results.
- Add selector UI that handles revoked/stale selections safely.
- Add Branch and Company assignment/grant administration behind permissions.
- Replace raw numeric Company/Branch fields incrementally with grant-aware selectors.
- Keep API clients, models, routes, Persian/English translations, RTL/LTR, and mobile layouts synchronized.

## 12. Audit Requirements

Foundation audit records actor, timestamp, tenant, Company/Branch when applicable, action, target, reason/context and changed values. Required actions: Company/Branch create/change/deactivate; Company-role assign/change/revoke; Branch grant/revoke; context default change; Contact identity correction; role add/remove; Contact role add/remove; Contact merge/compensation.

## 13. Test Strategy

Preserve TenantContextTest, CompanyTenantIsolationTest, CRM tenant tests, AuthzTest, and frontend release-contract tests. Add:

1. Branch cannot cross tenant or Company.
2. Company A user is denied Company B resource even with the same permission.
3. Company grant without Branch grant is denied Branch access.
4. Missing/stale/revoked/manually altered context is denied server-side.
5. Different roles for the same user in different Companies expose only granted permissions.
6. Role/grant changes are audited with actor and before/after evidence.
7. Exact national/legal identity reuses/blocks Contact; similarity is candidate-only; uncertain merge cannot run.
8. Contact merge preserves both roles and legacy references.
9. API/DTO/client/route, Persian/English, RTL/LTR and responsive tests.
10. Fresh and upgrade Flyway tests on PostgreSQL with reconciliation checks.

## 14. Acceptance Criteria

- Tenant -> Company -> Branch is enforced for every new Foundation write and scope-aware read.
- A Branch has one valid same-tenant Company and a commercial document cannot change Company after creation.
- Only explicitly granted Company/Branch scope is accessible; missing grants are denied by backend tests.
- Active context is server-validated and stale context fails safely.
- Contact supports Customer and Supplier roles without duplicate canonical identity; controlled merge preserves history.
- Role/grant/contact-merge operations have append-only Foundation audit.
- Existing Company CRUD, Users, Customer/Supplier screens, history and permission behavior remain compatible.
- Backend/frontend/documentation and fresh/upgrade migration gates pass.

## 15. Rollback Strategy

Use additive forward migrations, feature-gated cutover, dual-read compatibility and audited compensating actions. Disable a new path or correct a mapping/grant; never delete Contact, role-assignment, Branch-grant, audit, Customer, Supplier or commercial history to roll back.

## 16. Risks

| Risk | Mitigation |
|---|---|
| Global User/Role paths can leak cross-tenant/company administration. | Scope and test user queries before grant enforcement; never trust UI filters. |
| Forms use raw Company/Branch IDs. | Introduce validated context first; migrate forms incrementally. |
| Customer/Supplier data is inconsistent. | Conservative exact matching, review queue, provenance, reconciliation. |
| Document FKs target Customer/Supplier. | Preserve IDs/mappings and dual-read; do not bulk rewrite history. |
| Audit formats differ. | Add Foundation audit beside existing logs. |
| TenantDefaults exists in older code. | New work uses TenantContext; retire legacy use only after verification. |

## 17. Explicitly Out of Scope

Sales/Purchasing document redesign, Inventory Variant/UOM, Pricing, Accounting/GL, tax computation, CRM scoring, reports, Repair/Warranty/Installments/Payroll/Portal, Asan final import, external integrations, and destructive conversion are out of scope.

## 18. Ordered Implementation Tasks

1. **Preserve existing** — characterize current tenant/RBAC/Company/Customer/Supplier behavior with focused security tests.
2. **Complete existing** — implement Branch ownership over V22 and same-tenant integrity.
3. **Migrate existing** — add Company-role and explicit Branch grants with audited verified seed/backfill.
4. **Complete existing** — add validated active Company/Branch context and reject absent/stale grants.
5. **Refactor existing** — centralize Company/Branch validation behind the scope boundary while retaining compatible APIs.
6. **Complete existing** — add Foundation audit for grants, context and identity actions.
7. **Migrate existing** — add Contact/role/mapping/provenance structures and dual-read compatibility.
8. **Refactor existing** — move controlled merge to Contact only after history/audit/reconciliation tests pass.
9. **Complete existing** — add frontend context, Branch/grant administration and grant-aware selectors after backend security exists.
10. **Preserve existing** — run contract, authorization, tenant, migration, localization and release validation before legacy retirement.

