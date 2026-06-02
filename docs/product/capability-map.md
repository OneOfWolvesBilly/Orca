# Orca Capability Map

This document derives capability groups from product workflows and current
development state.

It is not a feature checklist and must not invent capabilities from technology
categories alone. A capability belongs here only when it supports a known
workflow, planned workflow gap, or existing workflow protection need.

---

## How To Read This Map

This map is organized by Orca's development posture, not by a generic enterprise
capability dependency list.

Current posture:

1. Organization membership is the completed baseline.
2. Auth is the active development track.
3. Cross-cutting reference-core capabilities come after those baselines and
   protect or expose existing workflows.

This ordering keeps future work clear:

- organization slices are completed and should not be reopened unless a new
  workflow gap explicitly changes organization behavior
- auth may continue with `auth-10+` slices
- logging, exception handling, cache, API documentation, and frontend shell are
  reference-core support capabilities, not organization or auth domain behavior

---

## Track 1: Completed Organization Baseline

Development status: completed baseline / currently stable.

This track records what Orca already supports for organization membership. It is
listed first because organization was the first completed workflow area.

### Capability Group: Group Membership Lifecycle

Related workflow:

- Organization Membership

Existing slices:

- `organization-01`
- `organization-02`
- `organization-03`
- `organization-04`
- `organization-05`
- `organization-06`
- `organization-07`
- `organization-08`

Existing capabilities:

- create group
- invite registered user
- accept invitation
- reject invitation
- revoke invitation
- persist group and invitation state
- expose the completed commands through HTTP

Missing capabilities / possible future slices:

- list groups
- list group members
- list pending invitations
- invitation notification delivery
- invitation expiration
- reinvite policy
- membership role changes

Sequencing notes:

- These missing items should not be treated as automatic next work.
- A new organization slice should enter SDD only when a concrete organization
  workflow gap is selected.
- Query/read slices should identify the user workflow that needs the read model.

### Capability Group: Organization Role Boundary

Related workflow:

- Organization Membership

Existing slices:

- `organization-01` through `organization-08`

Existing capabilities:

- group-scoped `GroupAdmin` role
- member role from group creation and invitation acceptance
- permission checks for invite, accept, reject, and revoke invitation commands
- separation between organization group roles and auth system roles

Missing capabilities / possible future slices:

- organization role change after membership creation
- group role listing
- role delegation policy

Sequencing notes:

- Do not add generic role management from this gap alone.
- Future role work must name the actor, resource, and operation it protects.

---

## Track 2: Active Auth Development

Development status: active / expected to continue.

This track records the identity and access capabilities already implemented by
auth and the auth gaps that are reasonable candidates for future `auth-*`
slices.

### Capability Group: Identity Registry and Provisioning

Related workflow:

- User Provisioning / Identity Preparation

Existing slices:

- `auth-05`
- `auth-06`
- `auth-07`

Existing capabilities:

- auth-owned registered user identity source
- registered-user existence checks for auth and organization
- IT admin-managed regular user provisioning
- provisioning verification confirmation
- separation between `IT_ADMIN` auth system role and organization group roles

Missing capabilities / possible future slices:

- bootstrap IT admin lifecycle
- provisioning verification request initiation
- verified request completion into identity provisioning
- credential setup after provisioning
- account disable / reactivation lifecycle

Sequencing notes:

- Credential setup should not be derived until the provisioning workflow decides
  how a newly provisioned user receives or creates credentials.
- Account disable should define how disabled accounts affect login, sessions,
  and organization operations.

### Capability Group: Authentication and Session

Related workflow:

- Authentication and Session

Existing slices:

- `auth-01`
- `auth-02`
- `auth-03`
- `auth-04`
- `auth-05`
- `auth-08`
- `auth-09`

Existing capabilities:

- current user context
- protected HTTP command boundary
- password login
- opaque server-side session
- session-backed actor context for protected commands

Missing capabilities / possible future slices:

- login failure audit / troubleshooting reference
- logout and session revocation
- credential setup
- password reset / credential recovery
- current-user endpoint, if needed by frontend workflow
- Google / OAuth login
- SSO / OIDC login
- MFA, if explicitly adopted

Sequencing notes:

- `auth-10` is expected by existing docs to cover login failure audit/reference.
- Logout and revocation should be specified before session cache is considered.
- External login should come after internal auth/session semantics are stable.

### Capability Group: Auth System Role Boundary

Related workflow:

- User Provisioning / Identity Preparation
- Authentication and Session

Existing slices:

- `auth-06`

Existing capabilities:

- `IT_ADMIN` as an auth-owned system role
- `IT_ADMIN` authorizes auth user provisioning only
- regular provisioned users do not receive system roles by default

Missing capabilities / possible future slices:

- auth system role assignment
- auth system role revocation
- auth system role listing
- disabled/suspended user policy

Sequencing notes:

- Do not introduce a generic RBAC framework before a workflow needs it.
- Future auth role slices should start from a concrete protected operation and
  explain which actor is allowed to do it.

---

## Track 3: Cross-Cutting Reference Core

Development status: planned / gap.

These capabilities are not organization or auth domain behavior. They are
shared reference-core capabilities that make existing and future workflows
stable, supportable, observable, and frontend-consumable.

They should be developed after the relevant predecessor workflow exists.

### Capability Group: Error and Exception Contract

Related workflows:

- Error and Exception Handling
- Frontend Reference Shell
- all HTTP workflows

Existing slices:

- No dedicated slice.
- Existing web specs contain endpoint-specific error expectations.

Existing capabilities:

- limited endpoint-level error mapping in current specs

Missing capabilities / possible future slices:

- global exception handler
- stable API error response shape
- validation error response
- unauthenticated / unauthorized / not found / conflict mapping
- correlation id exposure in error responses

Sequencing notes:

- This should come before serious frontend shell work.
- It must not expose sensitive auth/session failure details.
- It should preserve existing endpoint behavior unless a spec changes it.

### Capability Group: Logging, Audit, and Observability

Related workflows:

- Login Failure Support / Audit
- Logging, Observability, and Operations
- all protected command workflows

Existing slices:

- No dedicated logging or observability slice.
- `auth-08` and `auth-09` explicitly exclude login failure audit/reference.

Existing capabilities:

- none as explicit product / SA behavior

Missing capabilities / possible future slices:

- structured application logging
- correlation / request id propagation
- safe logging policy for auth inputs, cookies, and credentials
- login failure audit / troubleshooting reference
- security audit trail for provisioning and organization membership commands
- health, readiness, liveness, and metrics

Sequencing notes:

- Login failure audit belongs to the active auth track when it defines login
  support behavior.
- General logging and observability are cross-cutting support capabilities.
- Logs and audit must not store passwords, raw session cookie values, or
  credential secrets.
- A logging slice should not reopen organization behavior just because
  organization commands are audit/log sources.

### Capability Group: Performance and Cache

Related workflows:

- Performance Cache for Existing Lookups
- Organization Membership
- User Provisioning / Identity Preparation

Existing slices:

- No dedicated cache slice.

Existing capabilities:

- repeated registered-user existence lookup path exists through auth and
  organization integration

Missing capabilities / possible future slices:

- positive registered-user existence lookup cache
- cache invalidation/update after user provisioning
- provider-neutral cache boundary
- cache safety documentation for local versus shared cache

Sequencing notes:

- Do not cache passwords, credentials, profile details, role details, or raw
  session values.
- Session lookup cache should be delayed until logout/revocation semantics are
  specified.
- Positive registered-user existence cache is a safer first cache slice than
  session cache.
- Cache work is an infrastructure optimization and must preserve existing
  workflow behavior.

### Capability Group: API Contract and Documentation

Related workflows:

- Error and Exception Handling
- Frontend Reference Shell
- all HTTP workflows

Existing slices:

- Current specs document endpoint behavior, but no OpenAPI contract exists.

Existing capabilities:

- human-readable HTTP contracts in specs

Missing capabilities / possible future slices:

- OpenAPI generation or maintained API docs
- API versioning policy, if needed
- frontend-consumable error schema docs

Sequencing notes:

- API docs should follow stable behavior and error contracts.
- API docs must not become the source of behavior truth ahead of specs.

### Capability Group: Frontend Reference Shell

Related workflow:

- Frontend Reference Shell

Existing slices:

- None.

Existing capabilities:

- none

Missing capabilities / possible future slices:

- login page
- protected route/session state
- organization command console
- error display based on stable API contract
- possibly admin provisioning screens, if the workflow is adopted

Sequencing notes:

- Frontend must not re-implement business rules.
- Frontend slices should live under the bounded context whose behavior they
  expose unless cross-context shell behavior is explicitly specified.

---

## Cross-Track Dependency Notes

- Organization is stable enough to serve as a workflow source for logging,
  audit, API docs, and frontend shell.
- Auth remains active and may produce predecessor slices for logging, audit,
  cache, frontend, and external login capabilities.
- Cross-cutting support slices should say which completed or active workflow
  they protect.
- A cross-cutting slice must not silently add domain behavior to organization
  or auth.

---

## Excluded From Current Core Baseline

These are not current Orca capabilities and should not be added without a new
product decision:

- CRM customer workflows
- project management workflows
- billing
- notification center
- workflow engine
- approval engine
- custom role builder
- generic multi-tenant SaaS platform
