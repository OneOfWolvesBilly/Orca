# Orca Workflow Map

This document maps currently known end-to-end workflows to existing slices,
planned gaps, and unknowns.

It is a product / SA document. It does not introduce behavior. Authoritative
behavior remains in `docs/specs/<bounded-context>/*`.

---

## Status Legend

- Already supported: supported by completed authoritative specs.
- Planned / gap: suitable future work, but not implemented or not yet specified.
- Unknown / to be discovered: cannot be invented without product decision or
  future discovery.

---

## Workflow: User Provisioning / Identity Preparation

Status: partially supported.

Primary actor:

- IT Admin for admin-managed provisioning
- Target Person for provisioning verification

Supporting actor:

- Auth

Goal:

Prepare a person to become or be recognized as an Orca registered user without
mixing identity readiness with login, organization membership, or profile
behavior.

Preconditions:

- A bootstrap IT admin already exists.
- For verification, a provisioning verification request already exists and the
  target person has received the request id and verification code outside the
  slice.

Main success flow:

1. Auth owns registered user identity state.
2. An authenticated IT Admin provisions a regular registered user identity.
3. A target person confirms a provisioning verification request with an opaque
   request id and verification code.
4. Registered identity state becomes available to auth current-user-context
   checks and organization registered-user checks where specified.

Alternative / failure flows:

- Non-admin actor cannot provision users.
- Duplicate requested user id is rejected.
- Missing, blank, unknown, expired, already verified, or mismatched
  verification data is rejected without exposing which condition failed.
- Verification does not create login state, session state, or registered user
  identity by itself.

Currently supported slices:

- `auth-05` registered user identity integration
- `auth-06` admin-managed user provisioning
- `auth-07` provisioning identity verification

Known gaps:

- bootstrap IT admin lifecycle
- provisioning verification request initiation
- relationship between verified request and final identity provisioning
- credential setup after provisioning
- account disable / reactivate lifecycle

Explicit unknowns:

- personnel system source
- email, SMS, or other verification delivery channel
- approval workflow
- bulk import
- IT department or helpdesk model

---

## Workflow: Authentication and Session

Status: partially supported.

Primary actor:

- Registered User

Supporting actor:

- Auth
- HTTP auth boundary

Goal:

Allow a registered user to log in and allow protected commands to resolve one
authenticated actor from server-side session state.

Preconditions:

- A registered user identity exists.
- Auth-owned credential state can verify a login identifier and password.

Main success flow:

1. User submits login identifier and password to `POST /api/auth/login`.
2. Auth verifies credentials against auth-owned credential state.
3. Auth creates server-side session state for exactly one registered user.
4. Auth returns an opaque `ORCA_SESSION` cookie.
5. A protected command request presents the cookie.
6. Auth resolves current user context from server-side session state.
7. Downstream protected command behavior receives the authenticated actor id.

Alternative / failure flows:

- Invalid login attempts return one indistinguishable failure response.
- Failed login creates no session and issues no cookie.
- Missing, blank, unknown, expired, invalid, or revoked session values reject
  protected commands as unauthenticated without revealing which condition failed.
- `X-User-Id` no longer establishes protected command context after `auth-09`.

Currently supported slices:

- `auth-01` establish authenticated user context
- `auth-02` HTTP current user context integration
- `auth-03` HTTP request current user context access
- `auth-04` protected HTTP command boundary mapping
- `auth-05` registered user identity integration
- `auth-08` password login with server-side session
- `auth-09` protected HTTP session context

Known gaps:

- login failure audit / troubleshooting reference
- logout and session revocation
- credential setup and password reset / recovery
- account disable checks during login and session use
- current-user endpoint if required by frontend workflow
- Google / OAuth login
- SSO / OIDC login
- MFA

Explicit unknowns:

- production identity provider strategy
- MFA requirement
- session revocation retention and propagation rules
- login audit retention and access policy
- frontend session UX

---

## Workflow: Organization Membership

Status: partially supported.

Primary actors:

- Registered User
- Authenticated User
- GroupAdmin
- Invitee

Supporting actors:

- Organization
- Auth registered-user source
- HTTP auth boundary

Goal:

Allow authenticated users to create groups and manage membership through
registered-user invitations.

Preconditions:

- Actor is authenticated for protected HTTP commands.
- Invitee is a registered user where invitation behavior requires it.
- Group exists for invitation lifecycle operations after group creation.

Main success flow:

1. A registered authenticated user creates a group and becomes GroupAdmin.
2. GroupAdmin invites a registered user with an intended group role.
3. Invitee accepts the invitation and becomes a GroupMember, or rejects it.
4. GroupAdmin may revoke a pending invitation.
5. Organization persists group and invitation state consistently.
6. HTTP endpoints expose the existing commands without adding new domain rules.

Alternative / failure flows:

- Unauthenticated protected command is rejected.
- Non-GroupAdmin cannot invite or revoke.
- Non-invitee cannot accept or reject an invitation.
- Unknown group or invitation is rejected or not found.
- Duplicate pending invitations and invitations for existing members are
  rejected.

Currently supported slices:

- `organization-01` create group
- `organization-02` invite member
- `organization-03` accept invitation
- `organization-04` reject invitation
- `organization-05` revoke invitation
- `organization-06` application integration
- `organization-07` persistence integration
- `organization-08` web API integration
- `auth-05` registered user identity integration
- `auth-09` protected HTTP session context

Known gaps:

- list groups, members, or invitations
- invitation notification delivery
- invitation expiration
- reinvite policy
- role changes after membership creation
- frontend organization command surface

Explicit unknowns:

- group hierarchy
- tenant model
- cross-group transfer
- non-registered user invitation by email
- membership reporting

---

## Workflow: Login Failure Support / Audit

Status: planned / gap.

Primary actor:

- Unknown support or operator actor
- Login user is the actor whose failed attempt may receive a safe reference

Supporting actor:

- Auth
- Operational audit store, if specified later

Goal:

Support secure troubleshooting of login failures without exposing credential,
registered-user, personnel, role, organization, or failure-reason details to the
client.

Preconditions:

- Password login behavior from `auth-08` exists.
- Login failure responses remain indistinguishable to the client unless a later
  spec defines a safe opaque troubleshooting reference.

Main success flow:

Not yet specified.

Alternative / failure flows:

Not yet specified.

Currently supported slices:

- None as implemented behavior.
- `auth-08` and `auth-09` explicitly exclude login failure audit/reference
  behavior.

Known gaps:

- login failure reference id
- login attempt audit record
- support lookup workflow
- audit retention and access rules
- log/audit safety rules

Explicit unknowns:

- who can inspect login audit
- retention period
- whether the reference is client-visible
- whether audit is stored with application logs or a separate audit store
- privacy and security policy

---

## Workflow: Error and Exception Handling

Status: planned / gap.

Primary actor:

- API client
- Developer or operator consuming logs and diagnostics

Supporting actor:

- Web adapter
- Application exception mapper

Goal:

Provide stable error responses to clients and safe diagnostic information to
operators without leaking sensitive data.

Preconditions:

- Existing HTTP endpoints and application errors exist.

Main success flow:

Not yet specified.

Currently supported slices:

- Existing web specs define some endpoint-level error expectations, but no
  global error contract exists.

Known gaps:

- global exception handling
- stable API error response shape
- validation error response policy
- unauthenticated / unauthorized / not found / conflict mapping policy
- correlation id in error responses

Explicit unknowns:

- exact error schema
- localization
- public error code taxonomy
- frontend error display requirements

---

## Workflow: Logging, Observability, and Operations

Status: planned / gap.

Primary actor:

- Developer or operator

Supporting actor:

- Application runtime
- Logging / metrics backend, if adopted

Goal:

Make Orca supportable as an enterprise application reference core through safe
logs, correlation, health, readiness, liveness, and metrics.

Preconditions:

- Existing backend application runtime exists.

Main success flow:

Not yet specified.

Currently supported slices:

- None as explicit workflow behavior.

Known gaps:

- structured logs
- correlation / request id propagation
- safe logging rules for auth/session data
- health, readiness, and liveness endpoints
- metrics
- operational documentation

Explicit unknowns:

- logging backend
- metrics backend
- retention policy
- deployment environment

---

## Workflow: Performance Cache for Existing Lookups

Status: planned / gap.

Primary actor:

- GroupAdmin using invitation workflow
- Application operator concerned with repeated lookup load

Supporting actor:

- Auth registered-user source
- Organization invitation workflow

Goal:

Improve repeated registered-user existence checks without changing invitation
behavior or caching sensitive profile, credential, password, role, or session
details.

Preconditions:

- Auth owns registered user identity state.
- Organization asks auth whether an invitee user id is registered.

Main success flow:

Not yet specified.

Currently supported slices:

- No cache behavior.
- Existing lookup dependency is supported by `auth-05`, `organization-02`,
  `organization-06`, and `organization-08`.

Known gaps:

- positive registered-user existence lookup cache
- cache miss fallback to authoritative auth-owned source
- cache invalidation/update after provisioning
- explicit no-negative-cache or very-short-negative-cache rule

Explicit unknowns:

- local cache versus shared cache
- TTL
- cache provider
- multi-instance deployment needs

---

## Workflow: Frontend Reference Shell

Status: planned / gap.

Primary actor:

- Registered User
- GroupAdmin
- Invitee

Supporting actor:

- Frontend application shell
- Backend API

Goal:

Demonstrate how a client consumes Orca's backend reference core without
re-implementing business rules.

Preconditions:

- Login/session and organization command APIs exist.
- Stable API error contract exists or is planned as predecessor work.

Main success flow:

Not yet specified.

Currently supported slices:

- None.

Known gaps:

- login screen
- protected route/session state
- organization command console
- API error display
- frontend terminology aligned with backend domain terms

Explicit unknowns:

- design system
- route structure
- state management approach
- whether frontend should include admin provisioning screens
