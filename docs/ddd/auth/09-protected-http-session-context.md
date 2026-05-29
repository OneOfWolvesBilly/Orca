# DDD Derivation - 09 Protected HTTP Session Context

This note is **derived from**
`docs/specs/auth/09-protected-http-session-context.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- The slice defines how auth-owned session state establishes current user
  context for protected HTTP commands.
- The `ORCA_SESSION` cookie and server-side authenticated session are auth
  concepts introduced by auth-08.
- Organization command semantics remain unchanged; organization continues to
  consume an already-established `CurrentUserContext`.

---

## Aggregate Root

No new aggregate root is introduced by this slice.

Why:
- Auth-08 already introduced `AuthenticatedSession` as the session aggregate.
- This slice reads existing server-side session state to establish current user
  context.
- This slice does not create, renew, revoke, or mutate sessions.
- Current user context invariants already belong to the existing auth model from
  Spec 01.

---

## Minimum Model Changes

### Existing domain model reused

- `AuthenticatedSession`
  - remains the auth-owned server-side session model introduced by auth-08
- `AuthenticatedSessionId`
  - remains the opaque session id value object
- `CurrentUserContext`
  - remains the request-scoped context consumed by downstream protected commands

### Application behavior

- Extend the auth application boundary so current user context can be
  established from one presented session id.
- The session lookup must return an authenticated user id only when the
  server-side session exists and is unexpired.
- The resolved authenticated user id should still pass through the existing
  current-user-context establishment behavior so registered-user checks and
  current-user-context invariants remain unchanged.

### Application port

- Extend or add a session lookup capability on auth-owned session persistence.
- The lookup must be phrased in auth application terms, not HTTP terms.
- Missing, unknown, expired, invalid, or revoked sessions must collapse to the
  same unauthenticated outcome for the web boundary.

### Web adapter

- Read the `ORCA_SESSION` cookie for protected command requests.
- Use the session cookie value as an opaque lookup key only.
- Store the established `CurrentUserContext` in the existing request-scoped
  attribute for downstream web adapters.
- Stop using `X-User-Id` as the protected command auth source.

### Infrastructure

- Reuse the auth-owned session table introduced by auth-08.
- Infrastructure maps persisted session state to the application session lookup
  port.
- Infrastructure does not define authentication or authorization behavior.

---

## Rule Placement

### Domain rules

- Unchanged from Spec 01 and auth-08.
- `AuthenticatedSessionId` remains opaque.
- `CurrentUserContext` still contains exactly one authenticated user id.

### Application rules

- A protected command can establish current user context only when one presented
  session id resolves to a valid unexpired server-side session.
- Unknown, expired, invalid, or revoked sessions are unauthenticated.
- The resolved authenticated user id remains subject to existing registered-user
  checks.
- No session creation, renewal, revocation, or login behavior is added.

### Web adapter rules

- Read `ORCA_SESSION` from the request cookies.
- Do not decode identity, role, organization, profile, or permission information
  from the cookie value.
- Reject protected commands without an establishable session.
- Use one unauthenticated response for missing, blank, malformed, unknown,
  expired, invalid, or revoked sessions.
- Ignore `X-User-Id` for protected command current-user-context establishment.

### Organization rules

- No organization domain rule changes.
- No organization application command behavior changes.
- Existing organization web controllers may continue to consume
  `CurrentUserContext`; the source of that context changes at the auth boundary.

---

## Protected Command Mapping

The protected command path list remains the list from Spec 04:

- `POST /api/groups`
- `POST /api/groups/{groupId}/invitations`
- `POST /api/group-invitations/{invitationId}/accept`
- `POST /api/group-invitations/{invitationId}/reject`
- `POST /api/group-invitations/{invitationId}/revoke`

No endpoint is added, removed, or renamed.

---

## Test Layer Placement

Domain tests:
- No new domain tests are required unless existing value objects need additional
  validation to support session lookup input.

Application tests:
- valid session id resolves to current user context
- missing or blank session id is rejected as unauthenticated
- unknown session id is rejected as unauthenticated
- expired session is rejected as unauthenticated
- resolved session user still passes through existing registered-user checks

Infrastructure tests:
- JDBC session lookup returns authenticated user id only for an existing
  unexpired session
- JDBC session lookup returns no authenticated user id for missing or expired
  sessions

Web/integration tests:
- protected command succeeds when a valid `ORCA_SESSION` cookie is present
- protected command rejects missing `ORCA_SESSION`
- protected command rejects blank, unknown, expired, or invalid sessions with
  the same unauthenticated response
- protected command rejects a request that presents only `X-User-Id`
- downstream organization command receives the session-resolved authenticated
  user id through `CurrentUserContext`
- login endpoint behavior remains unchanged

---

## Non-Goals / Out of Scope

- Login credential verification.
- Session creation.
- Login endpoint changes.
- Logout.
- Session renewal or sliding expiration.
- Login failure reference id or login audit.
- Refresh tokens, access tokens, OAuth, MFA, or password reset.
- Role assignment, revocation, listing, or authorization model changes.
- IT_ADMIN, GroupAdmin, DBM, ITSM, or approval workflow lifecycle changes.
- Frontend UI.
- User profile or current-user endpoint.
- Organization domain behavior changes.
- Organization application command behavior changes.
- Spring Security or a production authentication framework.
