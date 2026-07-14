# DDD Derivation - 11 Logout and Session Revocation

This note is **derived from**
`docs/specs/auth/11-logout-session-revocation.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- Logout ends an auth-owned server-side session.
- Session revocation changes auth-owned session state.
- The `ORCA_SESSION` cookie is only an opaque reference to auth-owned session
  state.
- Organization consumes only an already-established `CurrentUserContext`; it
  does not own logout, revocation, expiration, or session lookup behavior.
- Frontend UI behavior is not defined by this slice.

---

## Aggregate Root

**AuthenticatedSession**

Why:
- Auth-08 introduced server-side session state for a successful password login.
- Auth-09 consumes existing session state to establish current user context.
- Auth-11 mutates that same session state by revoking the presented active
  session.
- The rule "revoked sessions cannot become active again in this slice" belongs
  with the session state being revoked.
- The rule "expired sessions cannot become active again in this slice" is
  evaluated from auth-owned session state and time.

No new aggregate root is required for logout itself. Logout is the command that
asks the auth application boundary to find and revoke the presented
`AuthenticatedSession`.

---

## Minimum Model Changes

### Existing domain model extended

- `AuthenticatedSession`
  - remains the auth-owned server-side session model
  - must be able to represent whether the session is active, expired, or
    revoked
  - owns the transition from active to revoked
  - must not support transition from revoked back to active in this slice
  - must not support transition from expired back to active in this slice

- `AuthenticatedSessionId`
  - remains an opaque session id value object
  - must not encode user, personnel, role, organization, profile, permission,
    expiration, or revocation information

- Session status / revocation state
  - may be represented as a status, timestamp, boolean, or equivalent domain
    state as long as the model can distinguish active from revoked for internal
    decisions
  - must not become client-visible session-state detail

- Time source
  - remains necessary to evaluate expiration
  - expiration is not logout
  - expiration does not renew or revive a session

### Application behavior

- Add a logout use case that receives at most one presented session id from the
  HTTP boundary.
- The use case revokes the session only when the presented session id resolves
  to an active server-side session.
- Missing, blank, malformed, unknown, expired, invalid, or already revoked
  session conditions collapse to the same safe logout outcome for the client.
- The use case creates no session, renews no session, replaces no session, and
  issues no new authenticated session cookie.

### Application ports

- Session repository / session store capability
  - lookup session by opaque session id
  - persist revoked session state
  - support current-user-context lookup behavior that treats revoked sessions as
    unauthenticated

- Time source
  - supplies the current time for expiration evaluation

The ports are phrased in auth application terms. They must not depend on HTTP,
cookies, Spring, JPA, or database schema details.

### Web adapter

- Expose `POST /api/auth/logout`.
- Read only the `ORCA_SESSION` cookie as the presented session reference.
- Accept only an empty JSON object request body and reject a non-empty body
  before invoking the application boundary.
- Treat the cookie value as opaque.
- Do not decode identity, role, organization, profile, permission, expiration,
  or revocation data from the cookie value.
- Pass the presented session id to the auth application boundary when present
  and acceptable as transport input.
- Return `204 No Content` with an empty body for an active session and every
  no-active-session condition.
- Return no user, profile, role, organization, session id, revocation reason,
  expiration state, or internal session state.
- May clear the `ORCA_SESSION` cookie as HTTP delivery behavior.

### Infrastructure

- Reuse auth-owned session persistence.
- Persist the revoked state needed by the domain/application model.
- If existing schema cannot represent revocation, add schema only through
  Flyway in the implementation slice.
- Infrastructure must not decide which session conditions are client-visible.
- Infrastructure must not define cleanup, retention, propagation, or renewal
  behavior in this slice.

---

## Rule Placement

### Auth domain rules

- `AuthenticatedSessionId` is opaque.
- An active session may be revoked.
- A revoked session cannot become active again in this slice.
- An expired session cannot become active again in this slice.
- A revoked session cannot establish current user context.
- An expired session cannot establish current user context.
- Revocation does not create authenticated session state.
- Revocation does not renew, extend, or replace a session.

### Auth application rules

- Logout acts on at most the presented server-side session.
- Logout revokes a presented active session.
- Logout creates no new server-side session.
- Logout issues no authenticated replacement session.
- Missing, blank, malformed, unknown, expired, invalid, and already revoked
  presented session conditions produce the same safe client-visible logout
  outcome.
- Protected command session lookup must treat revoked sessions as
  unauthenticated, preserving auth-09 rejection semantics.
- Expiration remains separate from user-initiated logout.

### Web adapter rules

- Logout is exposed as `POST /api/auth/logout`.
- Logout does not use `GET`.
- Logout request body is empty.
- The only session reference accepted by logout is `ORCA_SESSION`.
- Cookie clearing is allowed but is not the authoritative server-side
  revocation mechanism.
- Client-visible logout and protected-command rejection responses must not
  reveal whether a session was missing, blank, malformed, unknown, expired,
  invalid, or revoked.
- Web mapping must not expose raw session cookie values or internal session
  state.

### Infrastructure rules

- Session revocation state is stored in auth-owned persistence.
- Schema changes, if needed, are owned by Flyway.
- Persistence adapters implement application ports; they do not define auth
  behavior.
- Cleanup schedule, retention period, archival, and propagation policy are not
  implemented by this slice.
- Session renewal, sliding expiration, remember-me, and refresh-token behavior
  are not implemented by this slice.

### Organization rules

- No organization domain rule changes.
- No organization application command behavior changes.
- Organization protected commands continue to receive authenticated actor
  information only through `CurrentUserContext`.
- Revoked or expired sessions fail before downstream organization behavior
  executes.

### Frontend rules

- No frontend UI behavior is derived from this slice.
- Frontend must not infer business rules from cookie clearing.
- A future frontend slice may consume the backend logout API, but this DDD note
  does not define that UI or route behavior.

---

## Sensitive Data Boundary

Forbidden in client-visible logout responses:

- user id
- employee id or personnel id
- name, email, department, supervisor status, or profile data
- auth system role or organization role
- session id
- raw session cookie value
- revocation reason
- expiration state
- internal session state
- permission or authorization reasoning

Forbidden in logs or diagnostics by this slice:

- raw session cookie values
- credential secrets
- passwords
- user/profile details not explicitly allowed by a future logging or audit
  slice

Allowed internal state, when needed by the auth model:

- opaque session id as server-side lookup reference
- authenticated user id associated with the session
- expiration time
- revoked state or revocation timestamp

The allowed internal state list is not a client response contract.

---

## Explicitly Not In This Slice

- Login credential verification.
- Successful login behavior changes.
- Login failure reference id or login audit changes.
- Session creation.
- Session renewal or sliding expiration implementation.
- Idle timeout renewal implementation.
- Refresh token or access token behavior.
- Remember-me behavior.
- OAuth, SSO, OIDC, or external identity provider flows.
- MFA.
- Password reset or credential recovery.
- Account disable, suspend, reactivate, or delete behavior.
- Account lifecycle triggered session revocation implementation.
- Administrator-forced session revocation implementation.
- Revoking all sessions for a user.
- Multi-device session management implementation.
- Session cache or revocation propagation implementation.
- Storage cleanup implementation for expired or revoked sessions.
- Session retention implementation.
- Audit storage or reusable audit recording implementation.
- Full log management framework.
- General application logging or correlation id propagation.
- Authorization permission model changes.
- Role assignment, revocation, or listing.
- IT_ADMIN lifecycle.
- GroupAdmin lifecycle.
- Frontend UI.
- Current-user endpoint.
- User profile endpoint.
- Organization behavior changes.
- Spring Security or a production authentication framework.

---

## Unknown / To Be Discovered

- What retention policy should apply to revoked session records?
- What cleanup schedule should apply to expired or revoked session records?
- If a future session cache exists, what revocation propagation policy should
  prevent stale authenticated sessions?
- Should logout or revocation emit audit records or application logs?
- Should account disable or suspend revoke existing sessions?
- Which actor, if any, may force-revoke another user's session?
- Should users be able to list sessions or revoke all sessions?
- What frontend session UX is needed for logout, expiration, or renewal?
- What session renewal or sliding expiration policy should be specified by a
  future slice?

---

## Test Layer Placement

Domain tests validate:
- active session can be revoked
- revoked session cannot become active again in this slice
- expired session cannot become active again in this slice
- revoked session cannot establish current user context
- expired session cannot establish current user context
- revocation does not renew, extend, replace, or create a session

Application tests validate:
- logout with an active presented session persists revoked session state
- logout with an active presented session prevents later session resolution
- logout with missing session input returns the safe logout outcome
- logout with blank or malformed session input returns the safe logout outcome
- logout with unknown session id returns the safe logout outcome
- logout with expired session id returns the safe logout outcome
- logout with already revoked session id returns the safe logout outcome
- revoked sessions are rejected as unauthenticated by protected-command session
  lookup
- expired sessions are rejected as unauthenticated by protected-command session
  lookup
- logout creates no new session and returns no replacement authenticated session

Infrastructure tests may validate:
- persisted active sessions can be updated to revoked
- persisted revoked sessions remain revoked across reload
- session lookup returns no authenticated user id for revoked sessions
- session lookup returns no authenticated user id for expired sessions
- Flyway owns any schema change required for revocation state

Web integration tests validate:
- `POST /api/auth/logout` with an active `ORCA_SESSION` returns the safe logout
  response and may clear the cookie
- after logout, the same `ORCA_SESSION` is rejected by protected command
  endpoints as unauthenticated
- logout with missing, blank, unknown, expired, invalid, or already revoked
  sessions does not reveal the session condition
- logout response does not expose user, role, organization, profile, session
  id, cookie value, revocation reason, expiration state, or internal session
  state
- logout does not change `POST /api/auth/login` success or failure behavior
- logout does not change organization command behavior except by preventing
  execution when current user context cannot be established
