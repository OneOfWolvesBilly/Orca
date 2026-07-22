# Spec 13 - Client Session Expiry Coordination

Status: Approved / Implemented.

## Slice Intake

Slice candidate: `auth-13` client session expiry coordination.

Workflow:

- Authentication and Session.
- Frontend Reference Shell, by exposing existing auth-owned expiration as a
  safe client coordination value.

Workflow gap:

- Auth already creates a bounded server-side session through `auth-08`.
- Auth already rejects expired sessions through `auth-09`.
- The successful login response exposes only `204 No Content` and the
  HttpOnly `ORCA_SESSION` cookie.
- A browser frontend cannot read the cookie or reliably schedule presentation
  expiry without guessing the backend lifetime or polling a protected API.
- The provisional React protected-session lifecycle candidate therefore lacks
  a safe auth-owned deadline for automatic logout presentation.

Primary actor:

- A registered user completing the existing password login workflow.

Supporting actors:

- Auth.
- HTTP login boundary.
- Browser frontend client.

Successful outcome:

- Successful password login preserves `204 No Content` and the existing
  `ORCA_SESSION` cookie.
- The response also contains one auth-owned UTC session-expiry instant in the
  `Orca-Session-Expires-At` response header.
- The header value equals the expiration of the server-side session created by
  that login operation.
- A frontend may use the value only as the latest presentation deadline for
  that login result.
- The header exposes no session id, cookie value, actor id, or identity data.

Failure flows:

- Rejected login creates no session and returns no expiry header.
- Login that does not produce a valid session expiry must not be represented as
  a successful response satisfying this contract.
- Expired, revoked, invalid, and otherwise unestablishable sessions continue to
  use the existing indistinguishable `401 UNAUTHENTICATED` behavior.
- Client clock skew, delayed timers, sleeping tabs, or ignored response headers
  do not change server-side session validity.
- The expiry header must not be interpreted as proof that a session remains
  valid before the deadline.

Existing supported slices:

- `auth-08` password login with server-side session.
- `auth-09` protected HTTP session context.
- `auth-11` logout and session revocation.
- `reference-core-01` stable API error contract.
- `frontend-01` login result shell.
- `frontend-03` React consumer login composition and branding.

Planned predecessor slices:

- None.

Unknowns:

- Whether a future renewal or sliding-expiration behavior returns an updated
  client coordination deadline.
- Production clock synchronization policy.
- Browser timer throttling policy beyond checking an already received deadline.

These unknowns do not change the fixed bounded expiry exposed by this slice.

Non-goals:

- Session-status or current-user endpoint.
- Session inspection or restoration.
- Frontend timer or automatic-logout implementation.
- Session renewal or sliding expiration.
- Refresh token, access token, or remember-me behavior.
- Protected command UI or logout UI.
- Background polling.
- Account lifecycle behavior.
- Branding behavior changes.
- Product workspace or navigation.

Decision: enter SDD.

## Goal

Expose the expiration of the server-side session created by successful
password login as one safe HTTP response header so a browser client can
coordinate presentation timeout without reading the HttpOnly cookie, copying
the backend lifetime, or treating another protected command as a session probe.

This slice exposes existing auth-owned expiration. It does not change session
lifetime, validity, renewal, revocation, protected-command rejection, or login
credential behavior.

## Workflow Traceability

- Workflow: Authentication and Session.
- Protected workflow: Frontend Reference Shell.
- Workflow protection need: allow a browser client to stop presenting a stale
  login-success phase at the auth-owned session deadline.
- Primary actor: registered user completing password login.
- Supporting actors: auth, HTTP login boundary, and browser frontend client.

## Scope Ownership

This is an `auth` slice because auth owns:

- creation of the authenticated server-side session;
- the bounded session expiration instant;
- login success and rejection;
- the opaque session cookie;
- session validity for later protected requests.

The HTTP adapter serializes the expiry supplied by the auth application result.
It must not calculate a second expiry from a frontend setting, controller-local
constant, cookie text, or current response time.

Frontend delivery may consume the header in a later authorized slice. This
specification does not implement or prescribe a React state model.

## Contract Terms

- Session Expiry
  The auth-owned server-side instant after which the created session cannot
  establish authenticated current-user context.

- Client Session Expiry Coordination
  Safe delivery of the created session's expiration instant to a client for
  presentation scheduling. It does not transfer session authority to the
  client.

- Presentation Deadline
  The latest time at which a client should continue presenting state derived
  only from the corresponding successful login response. It is an upper bound,
  not proof of current session validity.

## Successful Login HTTP Contract

The existing endpoint remains:

```text
POST /api/auth/login
```

The request body remains:

```json
{
  "loginIdentifier": "login-id",
  "password": "password"
}
```

Successful login remains:

```text
204 No Content
```

The response body remains empty.

The response continues to issue the existing session cookie and additionally
contains exactly one value for this response header:

```text
Orca-Session-Expires-At: 2026-07-20T15:30:00Z
```

Header rules:

- The header name is exactly `Orca-Session-Expires-At`.
- The response contains exactly one value for this header.
- The value is a UTC ISO-8601 instant.
- The value equals the expiration instant of the server-side session created by
  this login operation.
- The value comes from the same auth application result used to issue the
  corresponding session cookie.
- The value must not be reconstructed by adding a configured duration at the
  web boundary.
- The response contains no second client-visible session-expiry field.
- Existing clients may ignore the new header.

The existing cookie contract from `auth-08` remains authoritative for cookie
name, opacity, HttpOnly, Secure, SameSite, path, and bounded lifetime behavior.

## Expiry Semantics

The response header communicates only a latest deadline.

Before that deadline, the session may already be unusable because it was:

- logged out or revoked;
- invalidated by existing auth-owned state;
- otherwise rejected by the existing session-resolution behavior.

At or after the deadline:

- the server-side session cannot establish current-user context;
- a protected request remains subject to `auth-09`;
- the client cannot renew or revive the session from the header;
- the client must not treat a locally delayed timer as extended validity.

The header is not:

- a session token;
- proof of authentication;
- a current-user representation;
- permission or role data;
- a renewal promise;
- a replacement for server-side validation.

## Scenarios

### Scenario: Successful login exposes the created session expiry

**Given**

- A registered user submits credentials that authenticate exactly one user.
- Auth creates a bounded server-side session.

**When**

- The HTTP boundary returns the successful login response.

**Then**

- The response status is `204`.
- The response body is empty.
- The response includes the existing `ORCA_SESSION` cookie.
- The response includes exactly one `Orca-Session-Expires-At` header value.
- The header value is the UTC ISO-8601 serialization of the created session's
  auth-owned expiration instant.
- The response does not expose user, actor, role, organization, profile,
  session id, or cookie value through the expiry header.

### Scenario: Rejected login exposes no session expiry

**Given**

- Auth rejects a password login according to the existing login behavior.

**When**

- The rejection is returned to the client.

**Then**

- No server-side session is created.
- No authenticated session cookie is issued.
- `Orca-Session-Expires-At` is absent.
- Existing login rejection and troubleshooting-reference behavior remains
  unchanged.

### Scenario: Client treats expiry as an upper bound

**Given**

- A client previously received a successful login response and its expiry
  header.
- The corresponding session is revoked before the exposed deadline.

**When**

- The client later invokes a protected operation.

**Then**

- Auth evaluates server-side session state.
- The protected operation is rejected under the existing unauthenticated
  behavior.
- The earlier expiry header does not make the session valid.

### Scenario: Client timer runs after the server deadline

**Given**

- A client received the session-expiry header.
- Browser scheduling delays client work until after that instant.

**When**

- The client next processes its presentation deadline or invokes a protected
  operation.

**Then**

- The delayed client activity does not extend the server-side session.
- Protected behavior remains unavailable for the expired session.
- No new session is created or renewed by this slice.

### Scenario: Existing client ignores the new header

**Given**

- An existing client already consumes the `204` login response and browser
  cookie behavior.

**When**

- The client ignores `Orca-Session-Expires-At`.

**Then**

- Existing login success remains compatible.
- Existing protected requests remain governed by server-side session state.
- Ignoring the header changes no auth behavior.

## Acceptance Criteria

- Successful password login MUST continue to return `204 No Content`.
- The successful response body MUST remain empty.
- Successful password login MUST include exactly one
  `Orca-Session-Expires-At` response-header value.
- The header value MUST use UTC ISO-8601 instant format.
- The header value MUST equal the expiration instant of the server-side session
  created by that login operation.
- The auth application result MUST supply the expiry used by the web adapter.
- The web adapter MUST NOT calculate a second expiry from response time or a
  duplicated session-lifetime setting.
- Rejected login MUST NOT return `Orca-Session-Expires-At`.
- The header MUST NOT contain or encode the session id or cookie value.
- The header MUST NOT contain or encode user, actor, personnel, profile, role,
  organization, membership, or permission information.
- The header MUST NOT establish, renew, extend, or prove session validity.
- Existing `ORCA_SESSION` cookie behavior MUST remain unchanged.
- Existing login rejection behavior MUST remain unchanged.
- Existing protected-session rejection behavior MUST remain unchanged.
- Existing logout and revocation behavior MUST remain unchanged.
- Existing clients MUST remain compatible when they ignore the header.
- This slice MUST NOT add a response body to successful login.
- This slice MUST NOT add a session-status or current-user endpoint.
- This slice MUST NOT implement frontend timing or automatic logout.

## Invariants

- Auth-owned server-side state remains authoritative for session validity.
- One successful login response exposes the expiry of the session created by
  that same login operation.
- Client-visible expiry is an upper bound only.
- Client time and timer execution cannot extend server-side session lifetime.
- No client-visible expiry value contains authentication material.
- Rejected login exposes no session expiry.

## Sensitive Data Boundary

`Orca-Session-Expires-At` and related client-visible responses must not expose
or encode:

- raw `ORCA_SESSION` cookie value or session id;
- password or credential secret;
- login identifier;
- authenticated user or actor id;
- employee or personnel id;
- name, email, department, supervisor status, or profile data;
- auth system role;
- organization role, membership, or permission details;
- revocation reason or internal session status;
- persistence, database, exception, or stack-trace details.

The expiration instant itself is the only new client-visible value authorized
by this slice.

## Error Cases

- Rejected login -> existing stable login rejection without expiry header.
- Unexpected failure before a valid successful response can be constructed ->
  existing safe error boundary; no successful expiry contract is returned.
- Expired session presented later -> existing `401 UNAUTHENTICATED` behavior.
- Revoked session presented before the exposed expiry -> existing
  `401 UNAUTHENTICATED` behavior.
- Client cannot parse or ignores the header -> no change to server-side session
  validity; frontend fallback belongs to a later frontend specification.

## Verification Requirements

Auth application tests must verify:

- successful login result supplies the created session's expiration instant;
- the returned expiry is the same value persisted with the created session;
- rejected login produces no successful session result or expiry value.

Auth web integration tests must verify:

- successful login returns `204`, an empty body, the existing cookie, and the
  exact expiry header;
- the response contains exactly one expiry-header value;
- the header parses as a UTC ISO-8601 instant;
- the parsed value matches the persisted created session expiry;
- rejected login omits the expiry header;
- the header and response expose no session id, actor, credential, role,
  organization, or profile data;
- existing login, protected-session, logout, and stable-error behavior remains
  unchanged.

The Maven reactor must remain green. No frontend production-code test is
required until a later authorized frontend slice consumes this contract.

## Compatibility Boundary

This slice adds one response header to successful login without changing:

- route;
- method;
- request body;
- success status;
- response body;
- session cookie contract;
- login failure response;
- protected-session semantics;
- logout or revocation semantics.

Clients that do not recognize the header continue to use the existing login
contract.

## Unknown / To Be Discovered

- Whether future explicit renewal returns a replacement expiry header.
- Whether future sliding expiration changes the meaning or update frequency of
  the client deadline.
- Whether production environments require an explicit clock-synchronization
  operational policy.
- Whether a future frontend uses a warning period before the deadline.

These questions require later authoritative behavior and do not change this
slice.

## Non-Goals

- Frontend timer, warning dialog, or automatic logout.
- Protected fixture command or React session-lifecycle UI.
- Current-user, session-status, or session-inspection endpoint.
- Refresh-time or startup session restoration.
- Background polling or keepalive.
- Session renewal, sliding expiration, or idle-time extension.
- Refresh token, access token, remember-me, or cross-product session sharing.
- OAuth, OIDC, SSO, MFA, or hosted login.
- Account disable, suspension, or forced revocation.
- Session listing, device management, or revoke-all behavior.
- Audit, structured logging, correlation, or retention expansion.
- Branding, attribution, logo fallback, or white-label behavior.
- Product workspace, navigation, organization UI, or profile UI.
- Production hosting, CORS, registry publication, or cloud topology.

## Follow-up Boundary

After this slice satisfies its done definition, the provisional React fixture
protected-session lifecycle candidate may enter intake again and decide how to:

- consume `Orca-Session-Expires-At` through the public React login composition;
- schedule a memory-only presentation deadline;
- invoke existing logout when that deadline is reached;
- reset presentation immediately after an earlier `401 UNAUTHENTICATED`;
- verify login, protected command success, logout, post-logout rejection, and
  timeout-driven presentation reset without reading the session cookie.

This specification does not authorize that frontend implementation.
