# Orca Product / SA Baseline

This document defines the current product and solution-architecture baseline
for Orca.

It is an upstream guide for SDD slice discovery. It is not a final product
requirements document, and it must not introduce behavior that is not supported
by authoritative specs.

The purpose of this baseline is to keep future slices traceable to a user or
operational workflow gap instead of deriving slices from technical ideas alone.

---

## Product Positioning

Orca is an enterprise application core reference project.

It demonstrates how common enterprise backend capabilities can be developed
through disciplined SDD, DDD, and TDD:

- auth-owned registered user identity
- password login and server-side sessions
- session-backed protected command actor context
- admin-managed user provisioning
- organization membership and invitation workflows
- persistence and HTTP delivery integration

Orca is not currently a CRM product, a project-management product, or a generic
SaaS platform. It may supply reusable development patterns for those products,
but product-domain workflows for CRM, project management, or other domains must
be defined in their own project baselines.

Frontend work has not yet become part of the implemented baseline. A small
frontend shell may be added later as part of the reference-core direction, but
it must still be traced to a workflow gap.

---

## Authority Relationship

Product / SA documents sit upstream of future SDD slice selection:

```text
Human SOP
  -> Project SOP
  -> Domain skills
  -> Project workflow
  -> SDD slice
  -> DDD / TDD / code
```

For Orca, this document and the related product maps define the project
workflow baseline. They do not replace specs.

- `docs/specs/<bounded-context>/*` remains authoritative for behavior.
- `docs/ddd/<bounded-context>/*` remains derived from specs.
- `docs/product/*` explains product direction, workflow gaps, and slice intake
  rules.
- If a product document and a spec conflict about implemented behavior, the
  spec wins until the product document is corrected.

---

## Baseline Actors

These actors are derived from completed auth and organization slices.

### Registered User

An auth-owned user identity that Orca recognizes as an existing user.

Supported by `auth-05` and `auth-06`.

### Authenticated User

A user identity accepted for one operation or HTTP request through the auth
boundary. After `auth-09`, protected HTTP command requests establish this
context from the `ORCA_SESSION` cookie and auth-owned server-side session state.

Supported by `auth-01` through `auth-05`, `auth-08`, and `auth-09`.

### IT Admin

A registered user identity with the auth-owned `IT_ADMIN` system role required
to provision regular registered user identities.

Supported by `auth-06`.

### Bootstrap IT Admin

The first IT admin identity that exists before normal admin-managed
provisioning can be used.

Known from `auth-06`, but its lifecycle is unknown / to be discovered.

### Target Person

The person who proves possession of a provisioning verification code before a
later provisioning step can use that verification result.

Supported by `auth-07`.

### GroupAdmin

A group-scoped organization role that can manage membership invitations for a
specific group.

Supported by organization slices.

### GroupMember

A registered user who belongs to a group with exactly one group role.

Supported by organization slices.

### Invitee

A registered user targeted by a group invitation.

Supported by `organization-02` through `organization-04`.

---

## Baseline Role Boundaries

Orca currently supports role separation, not full role management.

Already supported:

- auth system role boundary: `IT_ADMIN`
- organization group role boundary: `GroupAdmin` and member roles
- separation between auth system roles and organization group roles

Planned / gap:

- assigning, revoking, listing, or changing auth system roles
- assigning, revoking, listing, or changing organization roles outside the
  existing invitation lifecycle
- reusable permission decision boundaries
- resource-scoped access checks beyond the currently specified group behavior

Unknown / to be discovered:

- custom roles
- permission matrix shape
- role inheritance
- tenant-wide admin model

---

## Product Workflow Areas

The current product workflow areas are intentionally small because Orca is a
reference core, not a complete business application.

### Identity Readiness

Makes people recognizable to Orca as registered users and prepares them for
later authenticated work.

Already supported:

- auth-owned registered user identity
- IT admin-managed regular user provisioning
- provisioning identity verification

Planned / gap:

- bootstrap IT admin lifecycle
- verification request initiation
- credential setup after provisioning
- account disable / reactivate lifecycle

### Access Establishment

Allows a registered user to log in and allows protected operations to resolve a
single authenticated actor.

Already supported:

- password login with server-side session
- opaque session cookie
- session-backed protected HTTP command actor context

Planned / gap:

- logout and session revocation
- credential setup and password reset / recovery
- account disabled checks during login and session use
- current-user endpoint, if needed by frontend workflow
- Google / OAuth login
- SSO / OIDC login
- MFA, if explicitly adopted

### Organization Membership

Allows authenticated users to create groups and manage membership through
invitations.

Already supported:

- create group
- invite registered user
- accept invitation
- reject invitation
- revoke invitation
- persistence and HTTP delivery for these commands

Planned / gap:

- list groups, members, or invitations
- invitation notification delivery
- invitation expiration / reinvite policy
- role changes after membership creation

### Operational Reliability

Makes the reference core observable, supportable, and stable under expected
enterprise application conditions.

Already supported:

- only limited endpoint-level error behavior from existing specs

Planned / gap:

- stable API error contract
- global exception handling
- structured application logging
- correlation / request id propagation
- security and user-operation audit trail
- login failure audit and troubleshooting reference
- health, readiness, liveness, and metrics
- OpenAPI / API contract publication

### Performance and Cache

Improves repeated lookup paths without changing user-visible behavior.

Already supported:

- no explicit cache behavior is currently specified

Planned / gap:

- positive registered-user existence lookup cache for existing invitation
  workflows

Not currently recommended:

- password, credential, profile, role-detail, or session-value caching
- negative registered-user cache unless the stale-data rule is explicitly
  specified

### Frontend Reference Shell

Shows how a frontend consumes the backend reference core.

Already supported:

- no frontend workflow is currently specified

Planned / gap:

- login screen
- protected route/session state
- organization command console
- stable error display based on API error contract

Unknown / to be discovered:

- design system
- navigation model
- production frontend architecture

---

## Supporting Slice Types

These are implementation slice types, not product workflows. They may support a
workflow gap but must not create future slices by themselves.

- domain behavior slice
- application orchestration slice
- persistence integration slice
- HTTP delivery slice
- auth boundary integration slice
- logging / exception infrastructure slice
- cache infrastructure slice
- frontend delivery slice

Every future slice must identify which workflow gap it serves.

---

## Reference Core Direction

The reference-core direction is allowed to guide future planning, but each
planned capability still needs SDD before implementation.

Reference-core capabilities that may be introduced later:

- stable API error contract and global exception handling
- structured logs with correlation id
- security and user-operation audit trail
- login failure audit / troubleshooting reference
- logout and session revocation
- credential setup and password reset
- account disable / reactivation lifecycle
- registered-user existence positive cache
- health/readiness/liveness and metrics
- OpenAPI / API docs
- small frontend shell
- Google / OAuth login
- SSO / OIDC login

Capabilities that should not be added unless a project workflow explicitly
adopts them:

- CRM domain workflows
- project management workflows
- notification center
- billing
- workflow engine
- approval engine
- custom role builder
- generic SaaS tenant platform

---

## Slice Derivation Rule

A future slice must be traceable to one of these:

- a user workflow gap
- an operational workflow gap
- an existing workflow protection need, such as reliability, auditability,
  performance, or supportability

A future slice must not be derived only from:

- a framework feature
- a database table idea
- an endpoint list
- a cache technology
- an AI-generated roadmap
- a generic enterprise checklist

If a slice cannot identify its workflow gap, it should not enter SDD.

---

## Unknown / To Be Discovered

The following must not be filled in by assumption:

- complete enterprise permission model
- production security framework choice
- first bootstrap IT admin creation
- full account lifecycle policy
- complete SSO / OIDC provider rules
- MFA requirement
- frontend application design
- CRM, project-management, or other business-domain workflows
- notification and delivery channels
- operational retention policies for logs and audit records
