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

Orca is a portfolio-grade full-stack architecture showcase built around a
reusable authentication, session, audit, and logging core.

It demonstrates how a product foundation can be developed through disciplined
SDD, DDD, and TDD across backend behavior, frontend clients, database
migrations, local runtime support, and future cloud-oriented deployment
boundaries:

- auth-owned registered user identity
- password login and server-side sessions
- session-backed protected command actor context
- admin-managed user provisioning
- organization membership and invitation workflows
- persistence and HTTP delivery integration
- React frontend login, client failure observability, reusable consumer
  composition, and product-neutral protected session lifecycle
- local MariaDB runtime support for login-facing workflows
- safe diagnostic and audit/log support boundaries

Orca is not currently a CRM product, a project-management product, or a generic
SaaS platform. It may supply reusable development patterns for those products,
but product-domain workflows for CRM, project management, or other domains must
be defined in their own project baselines.

Frontend work is part of the showcase only where an authoritative frontend
delivery slice exists. The current implemented frontend baseline includes the
React login result shell, client failure observability, reusable consumer login
composition and branding, and the product-neutral React fixture protected
session lifecycle. Vue and Angular ports remain planned unless their
implementation slices are completed.

Reusable audit and logging support are product-neutral boundaries. Orca may
provide shared interfaces, record envelopes, diagnostics, and safety rules, but
consuming products own their product-specific events, metadata, storage choices,
and audit decisions.

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
- auth-owned session expiry coordination value for browser presentation
- session-backed protected HTTP command actor context
- logout and session revocation

Planned / gap:

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

---

## Reference Core Support Areas

These areas are not product workflows. They are cross-cutting support areas
that make Orca usable as an enterprise application core reference.

Support areas may justify future slices only when they protect, expose, or make
supportable an existing product workflow or an explicitly planned workflow gap.

### Operational Reliability

Makes the reference core observable, supportable, and stable under expected
enterprise application conditions.

Already supported:

- only limited endpoint-level error behavior from existing specs
- login failure audit and troubleshooting reference
- stable API error contract and global exception handling
- database-backed safe client diagnostic ingestion and exact-reference lookup
  by `IT_ADMIN`
- reusable product-neutral audit recording port and structurally validated
  envelope without centralized Orca audit storage

Planned / gap:

- structured application logging
- correlation / request id propagation
- audit storage adapters and workflow-specific audit emission
- health, readiness, liveness, and metrics
- OpenAPI / API contract publication

### Performance and Cache

Improves repeated lookup paths without changing product workflow behavior.

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

- React frontend login result shell
- stable login result and error presentation in the React reference
- React client failure observability and safe diagnostic reference presentation
- reusable React login package with bounded consumer branding
- product-neutral React fixture protected session lifecycle using the existing
  login, expiry, protected-command, and logout boundaries

Planned framework ports:

- Vue frontend login result shell
- Angular frontend login result shell

Planned / gap:

- protected product route and refresh-time session restoration
- organization command console
- stable error display beyond the login shell
- Vue and Angular parity for implemented frontend behaviors

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

Every future slice must identify which product workflow gap or reference-core
support gap it serves.

---

## Reference Core Capability Baseline

The reference-core direction is allowed to guide future planning. This section
lists the enterprise core capabilities discussed for Orca and records their
current status.

Each planned capability still needs SDD before implementation. The list below
does not create behavior by itself.

### Stable Error and Exception Management

Status: implemented baseline.

Purpose:

- provide a stable API error response shape
- centralize global exception handling
- map validation, unauthenticated, unauthorized, not found, conflict, and
  application/domain errors consistently
- support frontend error display without exposing internal exception details

Current support:

- existing web specs define some endpoint-level error expectations
- `reference-core-01` defines the authoritative stable API error contract
- `reference-core-01` implements the global HTTP error contract

Future change boundary:

- follow the `reference-core-01` API error contract
- preserve existing endpoint semantics unless an authoritative spec changes them

### Log Management and Correlation

Status: client diagnostics foundation implemented / broader logging remains a
gap.

Purpose:

- produce structured application logs
- propagate request / correlation id
- make backend behavior diagnosable without leaking secrets
- distinguish application logs from audit records

Current support:

- `reference-core-02` provides database-backed safe client diagnostic
  ingestion and exact-reference lookup by `IT_ADMIN`
- no general structured application logging slice exists yet

Required safety boundaries:

- do not log passwords
- do not log raw session cookie values
- do not log credential secrets
- do not log sensitive profile details unless a future policy explicitly allows
  it

### Audit and Operational Support

Status: reusable audit boundary implemented / broader audit workflow remains a
gap.

Purpose:

- record security-relevant and user-operation events where a support or audit
  workflow exists
- support login failure troubleshooting without exposing credential state
- support future audit of provisioning and organization membership commands

Current support:

- `auth-10` defines auth-owned login failure audit records and opaque
  troubleshooting references
- `reference-core-03` defines the product-neutral audit envelope, structural
  validation, and replaceable recording port
- `organization-01` mentions auditable creation, but there is no complete audit
  workflow or audit storage model

Completed first auth-related slice:

- `auth-10` login failure audit / troubleshooting reference

Completed reference-core slice:

- `reference-core-03` reusable audit recording boundary

Unknown / to be discovered:

- audit retention
- audit reader actor
- access policy
- audit lookup workflow

### Complete Login and Session Management

Status: partially supported / active auth development.

Already supported:

- password login through auth-owned credential verification
- opaque server-side session
- `ORCA_SESSION` cookie
- auth-owned session expiry coordination through
  `Orca-Session-Expires-At`
- protected command context from server-side session state
- login failure audit / troubleshooting reference
- logout
- session revocation

Planned / gap:

- credential setup
- password reset / credential recovery
- account disabled / suspended login checks
- current-user endpoint, if required by frontend workflow

External login options, planned / gap only:

- email-based login identifier support
- employee-id login identifier support
- Google / OAuth login
- SSO / OIDC login
- MFA, if explicitly adopted

Required boundaries:

- login identifier types must not expose personnel, role, organization, or
  profile meaning unless a future spec defines that mapping
- external login must not replace existing auth/session semantics without a
  migration workflow
- SSO / OIDC provider rules are unknown / to be discovered

### Account Lifecycle Management

Status: planned / gap.

Purpose:

- disable, suspend, reactivate, or otherwise restrict registered users when an
  account lifecycle workflow exists
- define how account state affects login, session use, and protected commands

Current support:

- user provisioning exists
- disabling, deleting, suspending, or reactivating users is explicitly outside
  existing auth slices

Unknown / to be discovered:

- disable versus delete policy
- session revocation after disable
- audit requirements
- who can manage account lifecycle

### Authorization and Role Management

Status: role boundaries supported / full role management gap.

Already supported:

- `IT_ADMIN` auth system role for user provisioning
- `GroupAdmin` organization group role for membership invitation behavior
- separation between auth system roles and organization group roles

Planned / gap:

- auth system role assignment / revocation / listing
- organization role changes after membership creation
- resource-scoped permission decision boundary
- full permission model, if future workflows require it

Required boundary:

- do not introduce a generic RBAC framework before a concrete workflow requires
  it

### Cache and Performance

Status: planned / gap.

Purpose:

- optimize repeated lookup paths while preserving existing workflow behavior

Recommended first cache candidate:

- positive registered-user existence lookup cache for invitation workflows

Required safety boundaries:

- do not cache passwords
- do not cache credential secrets
- do not cache profile details
- do not cache role-detail payloads
- do not cache raw session cookie values
- require any future session lookup cache to preserve the logout and revocation
  behavior defined by `auth-11`
- avoid negative registered-user cache unless a stale-data rule is explicitly
  specified

### Observability and Runtime Health

Status: planned / gap.

Purpose:

- expose health, readiness, liveness, and metrics needed by a reference core
- support deployment and operational diagnostics

Current support:

- no explicit observability workflow is defined yet

Unknown / to be discovered:

- metrics backend
- deployment profile
- retention and alerting policy

### API Contract and Documentation

Status: planned / gap.

Purpose:

- publish API contracts that frontend and external clients can consume
- document stable error shapes after the error contract exists

Current support:

- specs document HTTP behavior in prose
- no OpenAPI contract exists yet

Required boundary:

- OpenAPI/API docs must follow specs and must not become the source of behavior
  truth ahead of SDD

### Frontend Reference Shell

Status: React reference implemented / Vue and Angular ports planned.

Purpose:

- show how a frontend consumes Orca's backend reference core
- demonstrate login and safe protected-session integration, with future
  protected product routes, organization commands, and broader stable error
  display

Current support:

- `frontend-01` specifies the login result shell behavior for React, Vue, and
  Angular.
- the React reference implementation is available under
  `orca_frontend/react/`
- `reference-core-02` provides the safe client diagnostic destination required
  by frontend-02
- `frontend-02` specifies client failure observability behavior for React, Vue,
  and Angular
- the React frontend-02 reference implementation is available under
  `orca_frontend/react/`
- `frontend-03` provides the reusable React login package, consumer composition,
  and bounded branding contract
- `auth-13` exposes the auth-owned session expiry coordination value consumed
  by the protected-session fixture lifecycle
- `frontend-04` provides the memory-only product-neutral React fixture
  protected session lifecycle
- Vue and Angular ports remain planned

Planned / gap:

- protected product route
- refresh-time session restoration or inspection
- organization command console
- stable error display beyond the login shell
- Vue and Angular parity for the login result shell

Required boundary:

- frontend must not re-implement backend business rules
- frontend slices should live under the bounded context whose behavior they
  expose unless cross-context shell behavior is explicitly specified

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
