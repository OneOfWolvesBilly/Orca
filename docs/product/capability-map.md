# Orca Capability Map

This document derives capability groups from product workflows.

It must not invent capabilities from technology categories alone. A capability
belongs here only when it supports a known workflow, planned workflow gap, or
existing workflow protection need.

---

## Capability Group: Identity Registry and Provisioning

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

---

## Capability Group: Authentication and Session

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
- External login should come after the internal auth/session semantics are stable.

---

## Capability Group: Role Boundary and Authorization Checks

Related workflows:

- User Provisioning / Identity Preparation
- Organization Membership
- future resource-scoped reference-core workflows

Existing slices:

- `auth-06`
- `organization-01` through `organization-08`

Existing capabilities:

- auth system role boundary for `IT_ADMIN`
- group-scoped role boundary for `GroupAdmin`
- separation between auth system roles and organization group roles
- permission checks for invite, accept, reject, and revoke invitation commands

Missing capabilities / possible future slices:

- auth system role assignment / revocation / listing
- organization role change after membership creation
- reusable permission decision boundary
- resource-scoped access checks for future capabilities

Sequencing notes:

- Do not introduce a generic RBAC framework before a workflow needs it.
- Future authorization slices should start from a concrete protected operation
  and explain which actor is allowed to do it.

---

## Capability Group: Group Membership Lifecycle

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
- persist and expose these commands through HTTP

Missing capabilities / possible future slices:

- list groups
- list group members
- list pending invitations
- invitation notification delivery
- invitation expiration
- reinvite policy
- membership role changes

Sequencing notes:

- Query/read slices should identify a user workflow that needs the read model.
- Notification and expiration should not be added until the user operation and
  operational owner are clear.

---

## Capability Group: Error and Exception Contract

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

- This should precede a serious frontend shell so UI error states can rely on a
  stable backend contract.
- It must not expose sensitive auth/session failure details.

---

## Capability Group: Logging, Audit, and Observability

Related workflows:

- Login Failure Support / Audit
- Logging, Observability, and Operations

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

- Login failure audit should remain separate from general logging if it has
  support or security workflow semantics.
- Logs and audit must not store passwords, raw session cookie values, or
  credential secrets.

---

## Capability Group: Performance and Cache

Related workflows:

- Performance Cache for Existing Lookups
- Organization Membership

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

---

## Capability Group: API Contract and Documentation

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

---

## Capability Group: Frontend Reference Shell

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
