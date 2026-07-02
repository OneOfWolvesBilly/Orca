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

Status: partially supported.

Primary actor:

- Login user whose failed attempt receives a safe reference
- Unknown support or operator actor, if a future audit lookup workflow is
  specified

Supporting actor:

- Auth
- Auth-owned login failure audit state

Goal:

Support secure troubleshooting of login failures without exposing credential,
registered-user, personnel, role, organization, or failure-reason details to the
client.

Preconditions:

- Password login behavior from `auth-08` exists.
- Login failure responses remain indistinguishable to the client.

Main success flow:

1. Login user submits a login identifier and password to
   `POST /api/auth/login`.
2. Auth rejects the login attempt without creating a session or issuing an
   `ORCA_SESSION` cookie.
3. Auth creates an auth-owned server-side login failure audit record.
4. Auth returns an indistinguishable failed login response containing an opaque
   login failure reference id.
5. Audit details beyond the opaque reference remain server-side.

Alternative / failure flows:

- Missing input, blank input, unknown identifier, wrong password, invalid
  credential state, invalid registered-user state, invalid account state, no
  authenticated user result, and ambiguous authenticated user results all remain
  indistinguishable to the client.
- Successful login creates no login failure audit record and returns no login
  failure reference.
- Login failure audit does not define support lookup, query, retention, or
  access-policy behavior.

Currently supported slices:

- `auth-10` login failure audit
- `auth-08` password login with server-side session
- `auth-09` protected HTTP session context

Known gaps:

- support lookup workflow
- audit retention and access rules
- broader log/audit safety rules beyond login failure audit

Explicit unknowns:

- who can inspect login audit
- retention period
- whether audit is stored with application logs or a separate audit store
- privacy and security policy

---

## Workflow: Error and Exception Handling

Status: partially supported.

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

1. An API client submits a request to an existing Orca HTTP workflow.
2. A validation, authentication, authorization, not-found, application,
   framework-level, or unexpected failure occurs.
3. The web boundary returns the `reference-core-01` stable API error response.
4. The client handles the failure by stable status and coarse error code without
   parsing exception messages.
5. Sensitive and internal failure details remain server-side.

Currently supported slices:

- `reference-core-01` specifies and implements the stable API error contract.
- Existing web specs define endpoint-level error expectations.

Known gaps:

- correlation id in error responses
- structured logging and operator diagnostics

Explicit unknowns:

- localization
- complete public error code taxonomy
- generic troubleshooting reference
- response timestamp and path fields
- frontend error display requirements

---

## Workflow: Logging, Observability, and Operations

Status: client diagnostics foundation specified / broader workflow remains a
gap.

Primary actor:

- Developer or operator

Supporting actor:

- Application runtime
- Client application
- Backend client diagnostics store
- Logging / metrics backend, if adopted

Goal:

Make Orca supportable as an enterprise application reference core through safe
logs, correlation, health, readiness, liveness, and metrics.

Preconditions:

- Existing backend application runtime exists.
- `reference-core-01` stable API error contract exists.
- Authenticated `IT_ADMIN` role checks exist.

Main success flow:

1. A client submits one allowlisted client diagnostic record.
2. Reference-core assigns an opaque client failure reference and server
   timestamp.
3. The record is persisted before the reference is returned.
4. An authenticated `IT_ADMIN` looks up the safe record by exact reference.

Currently supported slices:

- `auth-10` supports auth-owned login failure audit.

Specified / implementation pending:

- `reference-core-02` client diagnostics foundation.

Known gaps:

- structured logs
- correlation / request id propagation
- client-side failure reporting behavior
- diagnostic retention and cleanup
- safe logging rules for auth/session data
- health, readiness, and liveness endpoints
- metrics
- operational documentation

Explicit unknowns:

- general application logging backend
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

Status: React reference implemented / Vue and Angular ports planned.

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
- Stable API error contract is implemented by `reference-core-01`.

Main success flow:

1. A registered user opens the frontend login shell.
2. The user submits a login identifier and password.
3. The frontend submits the existing backend password login request.
4. The frontend shows a safe login success result or a stable login rejection
   result.
5. Login rejection displays the opaque `loginFailureReferenceId`.
6. Protected route/session state and organization command behavior remain
   outside the first frontend slice.

Currently supported slices:

- `frontend-01` React frontend login result shell

Planned framework ports:

- `frontend-01` Vue implementation
- `frontend-01` Angular implementation

Known gaps:

- protected route/session state
- organization command console
- non-login API error display beyond the login shell
- frontend terminology aligned with backend domain terms
- Vue and Angular parity for the login result shell

Explicit unknowns:

- design system
- route structure
- state management approach
- whether frontend should include admin provisioning screens
- Vue and Angular framework-specific tooling
