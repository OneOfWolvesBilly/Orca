# DDD Derivation - 13 Client Session Expiry Coordination

Status: Draft / Awaiting DDD approval.

This note is **derived from**
`docs/specs/auth/13-client-session-expiry-coordination.md`.
It does not introduce new behavior.

## Bounded Context

**auth**

Rationale:

- Auth already owns password login and server-side session creation.
- Auth already owns the bounded session expiration instant and later session
  validity decisions.
- The HTTP login boundary only delivers one safe coordination value from the
  successful auth application result.
- Frontend presentation may consume that value only in a later authorized
  frontend slice.

Reference-core remains authoritative for rejected-login and unexpected-error
response bodies. Frontend-03 remains authoritative for the existing React
login composition and branding contract.

## No New Aggregate Root

No new aggregate root is introduced.

`AuthenticatedSession` remains the auth-owned aggregate created by successful
password login. Its existing `expiresAt` value remains the only authoritative
expiration for the created session.

This slice does not add a second client-session model, presentation-session
aggregate, expiry calculator, or frontend-owned session state.

## Existing Model Reused

### `AuthenticatedSession`

- continues to contain the opaque session id;
- continues to contain exactly one authenticated user id server-side;
- continues to contain `createdAt` and bounded `expiresAt` instants;
- continues to decide whether the session is expired or otherwise unusable;
- is persisted before the successful login result is returned.

No domain transition, lifetime rule, renewal behavior, or expiry invariant is
added by this slice.

### `PasswordLoginResult`

The successful application result supplies:

- the opaque session id required for the existing cookie;
- the exact `expiresAt` instant stored on the session created by that login.

The result is the single application-to-web delivery source for the exposed
expiry. It must not contain user, actor, credential, role, organization,
membership, personnel, profile, revocation, or internal session-status data.

Rejected login produces no successful `PasswordLoginResult`; therefore it
provides no expiry value for the web adapter to serialize.

## Consistency Boundary

One successful login operation performs this sequence:

1. verify the submitted credentials through the existing auth-owned boundary;
2. calculate the bounded session expiration in the existing auth application
   behavior;
3. create one `AuthenticatedSession` with that expiration;
4. persist that session;
5. return the same expiration through `PasswordLoginResult`;
6. serialize that returned expiration at the HTTP boundary.

The persisted session and successful application result must use the same
`Instant` value. The web adapter must not call a clock, add a duration, parse a
cookie, or consult a separate frontend setting to produce the header.

## Rule Placement

### Auth domain rules

- Existing `AuthenticatedSession` creation and bounded-lifetime invariants
  remain unchanged.
- Existing expiration and revocation rules remain unchanged.
- The exposed expiry is not a domain token or proof of authentication.
- No new domain test is required unless TDD discovers a defect in an existing
  session invariant.

### Auth application rules

- Successful login returns the expiration stored on the newly persisted
  session.
- The returned expiration and persisted expiration are exactly equal.
- Failed login creates no session and returns no successful result or expiry.
- The application result remains independent of HTTP header names and
  serialization.
- Session lifetime, renewal, and validity behavior remain unchanged.

### Web adapter rules

- Continue to expose `POST /api/auth/login`.
- Continue to return `204 No Content` with an empty body on success.
- Continue to issue the existing `ORCA_SESSION` cookie.
- Add exactly one `Orca-Session-Expires-At` response-header value on successful
  login.
- Serialize `PasswordLoginResult.expiresAt` as a UTC ISO-8601 instant.
- Do not calculate an expiry from response time, controller-local clock, cookie
  attributes, or duplicated session-lifetime configuration.
- Do not add the header on rejected login or another error response.
- Do not expose session id, cookie value, actor, credential, role,
  organization, membership, personnel, or profile data through the header.

The existing cookie `Max-Age` construction remains an HTTP cookie concern from
auth-08. It does not authorize the controller to derive the new expiry header
from that duration.

### Infrastructure rules

- Continue to persist `AuthenticatedSession.expiresAt` through the existing
  auth session repository.
- No new repository port, database column, table, migration, cache, clock, or
  configuration value is derived.
- Persistence remains authoritative only as an adapter for auth-owned session
  state; it does not define client presentation behavior.

### Reference-core rules

- Existing `LOGIN_REJECTED` behavior remains unchanged.
- Existing `loginFailureReferenceId` behavior remains unchanged.
- Existing stable error responses do not gain an expiry header.
- Unexpected failures remain governed by the existing safe error boundary.

### Frontend rules

- No frontend production behavior is implemented by this slice.
- Frontend-03 branding, attribution, logo, and login-composition policy remain
  unchanged.
- A future frontend slice may treat the header only as a presentation-deadline
  upper bound.
- Cookie presence or the expiry header must not be treated as proof that a
  session is currently valid.

## HTTP Mapping

Successful response:

```text
POST /api/auth/login
204 No Content
Set-Cookie: ORCA_SESSION=<opaque-session-id>; ...
Orca-Session-Expires-At: <UTC ISO-8601 instant>
```

Mapping source:

```text
PasswordLoginResult.sessionId
  -> existing ORCA_SESSION cookie value

PasswordLoginResult.expiresAt
  -> Orca-Session-Expires-At header value
```

The two mapped values describe the same newly persisted session. The header is
not constructed from the cookie and does not reveal the cookie value.

Rejected login continues to return the existing stable error response without
`Set-Cookie` or `Orca-Session-Expires-At`.

## Expiry Semantics

The header provides a latest presentation deadline only.

- Before the deadline, logout, revocation, or another existing auth-owned
  condition may already make the session unusable.
- At or after the deadline, the existing server-side session cannot establish
  current-user context.
- Client clock skew, sleeping tabs, and delayed timers do not extend server-side
  validity.
- Ignoring or failing to parse the header changes no server-side behavior.
- The header cannot establish, renew, revive, extend, or prove a session.

## Sensitive Data Design

The new header and application-to-web mapping must expose only the expiration
instant.

They must not expose or encode:

- raw session id or `ORCA_SESSION` cookie value;
- login identifier, submitted password, or credential secret;
- authenticated user or actor id;
- personnel, profile, name, email, department, or supervisor data;
- auth system role, organization role, membership, or permission data;
- revocation reason or internal session status;
- persistence, exception, or stack-trace details.

Tests may use opaque fixture values as inputs or persisted records, but must not
make those values part of the new public header contract.

## Test Layer Placement

### Auth application tests

Validate:

- successful login returns the created session expiry;
- the returned expiry exactly equals the persisted session expiry;
- the result remains associated with the same generated session id;
- rejected login creates no session and returns no successful result.

Existing application coverage may satisfy part of this behavior. TDD must still
make the equality requirement explicit and preserve all existing assertions.

### Auth web integration tests

Validate:

- successful login remains `204 No Content` with an empty body;
- successful login continues to issue exactly one existing session cookie;
- successful login returns exactly one `Orca-Session-Expires-At` value;
- the header parses as an `Instant` and uses UTC ISO-8601 serialization;
- the parsed header value exactly equals the persisted session expiry;
- rejected login omits the expiry header;
- stable rejected-login content and reference behavior remain unchanged;
- the response and header do not expose forbidden sensitive values.

### Regression verification

Validate that existing behavior remains green for:

- auth-08 successful and rejected password login;
- auth-09 expired-session rejection;
- auth-11 logout and revocation;
- auth-12 embedded auth consumption;
- reference-core-01 stable error mapping;
- the Maven reactor.

No frontend production-code test is derived for auth-13.

## Expected Implementation Boundary After Authorization

After TDD and implementation are separately authorized, the minimum expected
change is at the auth application-result and login web-mapping boundary.

Repository state already determines whether the application result supplies
the persisted expiry. Implementation must preserve an existing compliant
result rather than introduce a duplicate expiry representation.

The implementation phase must not:

- add a controller clock or expiry calculator;
- add a database migration or new persistence field;
- change session lifetime or cookie behavior;
- add a session-status or current-user endpoint;
- add renewal, sliding expiration, refresh tokens, or polling;
- modify frontend code or frontend-03 branding;
- begin provisional frontend-04.

## Unknown / To Be Discovered

- Whether a future explicit renewal returns a replacement coordination value.
- Whether future sliding expiration changes the deadline contract.
- Whether production operation requires a clock-synchronization policy.
- Whether a future frontend presents a warning before expiry.

These unknowns do not alter the fixed bounded expiry defined by auth-13.

## Non-Goals

- New auth aggregate or domain invariant.
- New persistence adapter, schema, migration, or infrastructure configuration.
- Session-status, current-user, or session-inspection endpoint.
- Frontend timer, warning, automatic logout, restoration, or polling.
- Protected fixture command or frontend session-lifecycle implementation.
- Session renewal, sliding expiration, keepalive, or idle extension.
- Refresh token, access token, remember-me, or cross-product session sharing.
- OAuth, OIDC, SSO, MFA, or hosted login.
- Account lifecycle or forced revocation behavior.
- Audit, logging, correlation, or retention expansion.
- Branding, attribution, logo, workspace, navigation, or organization UI
  changes.

## Follow-up Boundary

Only after auth-13 is complete and merged to local `main` may provisional
frontend-04 enter its own intake in the original independent task.

This DDD note does not authorize frontend-04, customer-owned presentation
amendments, TDD, or implementation.
