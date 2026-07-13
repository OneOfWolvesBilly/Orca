# Spec 11 - Logout and Session Revocation

## Goal

Allow a user or API client to explicitly end the presented auth-owned
server-side session without exposing session state to the client.

Successful logout revokes the presented server-side session so the same opaque
session id can no longer establish current user context for protected HTTP
command requests.

This slice defines explicit logout and revocation behavior. It does not define
session renewal, sliding expiration, refresh tokens, remember-me behavior,
account lifecycle revocation, administrator-forced revocation, or cleanup /
retention policy for expired or revoked session records.

## Workflow Traceability

- Workflow: Authentication and Session.
- Workflow gap: product workflow and capability maps list logout and session
  revocation as a planned authentication/session gap.
- Predecessor slices:
  - `auth-08` password login with server-side session.
  - `auth-09` protected HTTP session context.
  - `auth-10` login failure audit.
  - `reference-core-01` stable API error contract.
- Primary actor: registered user or API client presenting an `ORCA_SESSION`
  cookie.
- Session revocation retention, cleanup, and propagation policy: unknown / to
  be discovered.
- Session renewal and sliding expiration policy: follow-up slice candidate, not
  part of this slice.

## Domain Terms

- Logout
  A user-initiated command that asks auth to end the presented server-side
  session.

- Session Revocation
  The auth-owned state transition that makes a server-side session unusable for
  future current-user-context establishment before its normal expiration time.

- Revoked Session
  A server-side session that previously existed but can no longer establish
  current user context.

- Expired Session
  A server-side session whose bounded lifetime has ended. Expiration is not
  logout, but an expired session is not usable.

- Active Session
  A server-side session that exists, is not expired, is not revoked, and is
  otherwise valid for resolving exactly one authenticated user id.

- Logout Session Cookie
  The `ORCA_SESSION` cookie presented by the client to identify the session that
  logout should revoke. Its value remains an opaque lookup reference only.

- Indistinguishable Session Rejection
  The client-visible rule from `auth-09`: missing, blank, malformed, unknown,
  expired, invalid, and revoked session conditions must not reveal which
  condition occurred.

## HTTP Contract

This slice defines one logout command endpoint:

```text
POST /api/auth/logout
```

The request presents the existing auth session cookie when available:

```text
Cookie: ORCA_SESSION=<opaque-session-id>
```

The request body is empty:

```json
{}
```

Logout is a command and MUST use `POST`. It MUST NOT use `GET` because it
depends on user/session context.

Successful logout returns no user, personnel, role, organization, profile,
session state, session id, revocation reason, or expiration details.

The HTTP response may clear the `ORCA_SESSION` cookie using an expired or
zero-lifetime cookie value. Cookie clearing is an HTTP delivery behavior only;
server-side session revocation remains the authoritative logout result when an
active session is presented.

## Scenarios

### Scenario: User logs out with an active session

**Given**
- A client presents an `ORCA_SESSION` cookie.
- The cookie value is an opaque session id.
- Auth-owned server-side session state contains that session id.
- The server-side session is active.

**When**
- The client submits `POST /api/auth/logout`.

**Then**
- Auth revokes the server-side session identified by the presented session id.
- The same session id can no longer establish current user context.
- The response does not expose user id, personnel, role, organization, profile,
  session id, session state, or revocation details.
- The HTTP response may clear the `ORCA_SESSION` cookie.

### Scenario: Revoked session cannot establish protected command context

**Given**
- A server-side session has been revoked by logout.
- A later request targets a protected HTTP command endpoint.
- The request presents the same `ORCA_SESSION` cookie value.

**When**
- The HTTP auth boundary attempts to establish current user context.

**Then**
- Current user context is not established.
- Downstream protected command behavior does not execute.
- The request is rejected as unauthenticated according to `auth-09`.
- The response does not reveal that the session was revoked.

### Scenario: Logout with no active session remains safe

**Given**
- A client submits `POST /api/auth/logout`.
- The request has no `ORCA_SESSION` cookie, a blank session id, a malformed
  session id, an unknown session id, an expired session id, an invalid session
  id, or an already revoked session id.

**When**
- Auth handles the logout request.

**Then**
- No current user context is established from that session condition.
- No new session is created.
- No existing active session is revoked unless it is identified by an active
  presented session id.
- The client-visible response does not reveal whether the session cookie was
  missing, blank, malformed, unknown, expired, invalid, or already revoked.
- The HTTP response may still clear the `ORCA_SESSION` cookie.

### Scenario: Expired session remains unusable without being treated as logout

**Given**
- A server-side session has reached its bounded lifetime from the existing
  session behavior.
- The session was not explicitly logged out.

**When**
- A protected HTTP command request presents that expired session id.

**Then**
- Current user context is not established.
- Downstream protected command behavior does not execute.
- The request is rejected as unauthenticated according to `auth-09`.
- The response does not reveal that the session was expired.
- The expired session is not renewed or revived by this slice.

### Scenario: Logout does not renew session lifetime

**Given**
- A client presents an active `ORCA_SESSION` cookie.

**When**
- The client submits `POST /api/auth/logout`.

**Then**
- Auth revokes the presented session.
- Auth does not extend, renew, or replace the session.
- Auth does not issue a new authenticated session cookie.

## Acceptance Criteria

- Logout MUST be exposed as `POST /api/auth/logout`.
- Logout MUST NOT use `GET`.
- The logout request body MUST be empty.
- Logout MUST identify the session to revoke only from the `ORCA_SESSION`
  cookie when a session is presented.
- The `ORCA_SESSION` cookie value MUST be treated as an opaque session id.
- The logout behavior MUST NOT decode user id, personnel, role, organization,
  profile, or permission information from the cookie value.
- An active presented server-side session MUST be revoked by logout.
- A revoked session MUST NOT establish current user context for later protected
  HTTP command requests.
- Expired sessions MUST NOT establish current user context.
- Expiration MUST NOT be treated as user-initiated logout.
- Missing, blank, malformed, unknown, expired, invalid, and already revoked
  session conditions MUST NOT reveal which condition occurred.
- Logout MUST NOT create a server-side session.
- Logout MUST NOT renew, extend, or replace a server-side session.
- Logout MUST NOT issue a new authenticated session cookie.
- The logout response MUST NOT expose user id, employee id, personnel id, name,
  email, department, supervisor status, system role, organization role, profile
  data, session id, session cookie value, revocation reason, expiration state,
  or internal session state.
- The HTTP response MAY clear the `ORCA_SESSION` cookie.
- Cookie clearing MUST NOT be the authoritative server-side revocation
  mechanism.
- This slice MUST preserve `auth-09` protected command session rejection
  semantics.
- This slice MUST NOT change password login credential verification.
- This slice MUST NOT change successful login session creation.
- This slice MUST NOT change login failure audit behavior.
- This slice MUST NOT change organization behavior.

## Invariants

- Session state is auth-owned.
- A session id is an opaque lookup reference only.
- A revoked session cannot become active again in this slice.
- An expired session cannot become active again in this slice.
- Current user context can be established only from an active server-side
  session.
- Client-held session state is limited to the opaque session cookie value.
- Logout ends at most the presented server-side session.
- Logout does not create authenticated session state.

## Error Cases

- Missing `ORCA_SESSION` cookie on logout -> safe logout response without
  session-state disclosure.
- Blank session id on logout -> safe logout response without session-state
  disclosure.
- Malformed or otherwise unacceptable session id on logout -> safe logout
  response without session-state disclosure.
- Unknown session id on logout -> safe logout response without session-state
  disclosure.
- Expired session id on logout -> safe logout response without session-state
  disclosure.
- Invalid session id on logout -> safe logout response without session-state
  disclosure.
- Already revoked session id on logout -> safe logout response without
  session-state disclosure.
- Revoked session presented to protected command -> rejected as unauthenticated
  under `auth-09`.
- Expired session presented to protected command -> rejected as unauthenticated
  under `auth-09`.

All client-visible responses for non-active logout session conditions must avoid
identifying the failed session condition.

## Non-Goals

- Login credential verification.
- Successful login behavior changes.
- Login failure reference id or login audit changes.
- Session creation.
- Session renewal or sliding expiration.
- Idle timeout renewal policy.
- Absolute timeout policy beyond the existing bounded lifetime requirement.
- Refresh token or access token behavior.
- Remember-me behavior.
- OAuth, SSO, OIDC, or external identity provider flows.
- MFA.
- Password reset or credential recovery.
- Account disable, suspend, reactivate, or delete behavior.
- Account lifecycle triggered session revocation.
- Administrator-forced session revocation.
- Revoking all sessions for a user.
- Multi-device session management.
- Session cache or revocation propagation.
- Storage cleanup schedule for expired or revoked sessions.
- Session retention policy.
- Audit storage or reusable audit recording integration.
- Full log management framework.
- General application logging or correlation id propagation.
- Authorization permission model changes.
- Role assignment, revocation, or listing.
- IT_ADMIN lifecycle.
- GroupAdmin lifecycle.
- Frontend UI.
- Current-user endpoint.
- User profile endpoint.
- Changing organization domain behavior.
- Changing organization application command behavior.
- Spring Security or a production authentication framework.

## Follow-up Slice Boundaries

- A future session renewal / sliding expiration slice may define when active
  sessions are extended instead of requiring a new login. That future slice must
  preserve the rule that revoked sessions cannot be renewed.
- A future account lifecycle slice may define whether disabling or suspending an
  account revokes existing sessions.
- A future administrator session-management slice may define forced revocation,
  revoke-all-sessions, or device/session listing behavior.
- A future operational cleanup slice may define retention, scheduled deletion,
  or archival of expired and revoked session records.
- A future audit/logging slice may define whether logout or revocation emits
  audit records or application logs.
