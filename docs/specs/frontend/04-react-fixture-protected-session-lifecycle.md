# Frontend 04 - React Fixture Protected Session Lifecycle

Status: Approved amendment / Implemented.

## Slice Intake

Slice candidate: `frontend-04` React fixture protected session lifecycle.

Workflow:

- Frontend Reference Shell.
- Embedded Core Authentication Consumption.
- Authentication and Session, by consuming existing login, protected-session,
  expiry-coordination, and logout behavior.
- Error and Exception Handling, by consuming the existing stable API error
  contract.

Workflow gap:

- `frontend-03` allows an independent React consumer to compose the existing
  Orca login behavior through the public React package.
- `auth-12` allows the Minimal Consumer Fixture to expose one product-neutral
  protected command that receives exactly one authenticated actor from
  auth-owned session state.
- `auth-11` exposes logout and makes the presented session unusable for later
  protected commands.
- `auth-13` exposes the created session's auth-owned expiry as a safe login
  response header.
- The React Minimal Consumer Fixture does not yet consume the expiry metadata,
  present a protected-session phase, invoke the fixture command, invoke logout,
  or stop presenting protected state when the deadline or server rejection is
  observed.

Primary actor:

- A registered user signing in through the React Minimal Consumer Fixture.

Successful outcome:

- The user signs in through the existing public Orca React login composition.
- The browser continues to manage the opaque HttpOnly `ORCA_SESSION` cookie.
- The React fixture receives only the safe auth-owned session expiry metadata
  needed for memory-only presentation coordination.
- The user explicitly invokes the product-neutral protected fixture command
  and sees a safe success result.
- The user explicitly logs out through the existing Orca logout behavior.
- The same protected fixture command is rejected after logout.
- No actor id, session id, cookie value, identity data, role, organization, or
  profile data is displayed or exposed through the frontend public boundary.

Failure flows:

- A successful login response without exactly one valid future expiry value
  does not enable protected presentation and triggers one best-effort cleanup
  logout.
- A protected command rejected as `401 UNAUTHENTICATED` immediately ends the
  client protected presentation even if the client deadline has not arrived.
- A valid non-401 stable API failure is displayed safely without inventing
  backend business meaning.
- A transport failure or malformed response is displayed as the existing safe
  `REQUEST_UNAVAILABLE` result without exposing technical details.
- A failed manual logout keeps the current protected presentation available for
  retry only until an earlier server rejection or the existing deadline ends
  that presentation.
- Deadline-driven logout failure does not extend or restore protected
  presentation.
- Delayed timers and stale asynchronous request completions do not extend,
  restore, or prove session validity.

Existing supported slices:

- `auth-08` password login with server-side session.
- `auth-09` protected HTTP session context.
- `auth-11` logout and session revocation.
- `auth-12` embedded auth and actor-context integration.
- `auth-13` client session expiry coordination.
- `reference-core-01` stable API error contract.
- `frontend-01` login result shell.
- `frontend-02` client failure observability.
- `frontend-03` React consumer login composition and branding.

Planned predecessor slices:

- None.

Unknowns:

- Production frontend hosting and clock-synchronization policy.
- Whether a future session-renewal behavior supplies a replacement deadline.
- Whether a future product workflow needs countdown, warning, or protected
  navigation behavior.
- Whether Vue or Angular will later consume the same lifecycle behavior.

These unknowns do not change the fixed, memory-only lifecycle in this slice.

Non-goals:

- Changing the `frontend-03` branding, attribution, logo, or copyright policy.
- Customer-owned presentation amendments.
- Cookie reading, parsing, clearing, storage, or simulation.
- Session-status, current-user, profile, or session-inspection endpoints.
- Browser-refresh session restoration.
- Countdown, expiry timestamp display, warning dialog, or timeout route.
- Protected product navigation, workspace, organization console, or real
  product behavior.
- Background polling, keepalive, renewal, sliding expiration, refresh token,
  access token, remember-me, OAuth, OIDC, SSO, or MFA.
- Backend auth, organization, reference-core, database, or infrastructure
  behavior changes.
- Vue or Angular implementation.

Decision: enter SDD.

## Goal

Allow the independently structured React Minimal Consumer Fixture to consume
the existing Orca login, protected actor-context, expiry-coordination, stable
error, and logout contracts as one memory-only protected-session presentation
lifecycle.

The lifecycle is a client presentation boundary only. Auth-owned server-side
session state remains authoritative. A client deadline can stop presentation
and initiate existing logout, but it cannot establish, prove, renew, extend, or
restore a session.

## Scope Ownership

`frontend-04` is a frontend delivery slice.

Auth remains authoritative for:

- password login success and rejection;
- server-side session creation and expiration;
- the opaque `ORCA_SESSION` cookie;
- protected actor resolution;
- logout and revocation;
- every later session-validity decision.

Reference-core remains authoritative for stable API error fields and codes.
The Java Minimal Consumer Fixture remains authoritative for the product-neutral
protected command declared by `auth-12`.

Frontend delivery owns only:

- safe consumption of the auth-13 expiry response metadata;
- memory-only presentation phase and deadline coordination;
- invocation of existing protected fixture and logout commands;
- safe, product-neutral presentation of their observable lifecycle outcomes;
- cancellation and race handling for frontend-local work.

This slice does not create a frontend domain bounded context or authenticated
client-side source of truth.

## Backend Contracts Consumed

### Login

```text
POST /api/auth/login
204 No Content
Orca-Session-Expires-At: <UTC ISO-8601 instant>
```

The response body remains empty. The browser receives the existing
`ORCA_SESSION` cookie through normal cookie handling. Frontend application code
does not access that cookie.

### Protected Fixture Command

```text
POST /api/fixture/actor-context-check
Content-Type: application/json

{}
```

Success remains `204 No Content` with an empty body.

### Logout

```text
POST /api/auth/logout
Content-Type: application/json

{}
```

Success and the existing no-active-session conditions remain `204 No Content`
with an empty body.

All three requests use normal browser credential handling. No request body,
query parameter, custom identity header, or application state carries a
session id or actor id.

## Public React Integration Boundary

The supported `@oneofwolvesbilly/orca-react-login` package must allow a React
consumer to opt into successful-login session coordination without replacing
or copying the existing login implementation.

For an opted-in consumer, the successful-login signal contains only the
validated `Orca-Session-Expires-At` value needed to establish the presentation
deadline. It must not contain or expose:

- cookie or session id;
- actor or user id;
- credential or login identifier;
- role, organization, membership, permission, personnel, or profile data;
- response headers other than the authorized expiry value;
- a raw `Response` object.

Consumers that do not opt into the lifecycle retain the completed
`frontend-01` and `frontend-03` login-success presentation. The new boundary
must not require a branding change and must not provide branding, attribution,
or copyright overrides.

The exact React type and callback names are implementation details to be
derived in DDD. The observable input, output, compatibility, and sensitive-data
rules in this specification are authoritative.

## Session Presentation Phases

The React fixture has only these conceptual phases:

- login presentation;
- protected fixture presentation;
- transition in progress for protected command or manual logout;
- safe result presentation associated with the current phase.

The phase is memory-only. It is not stored in local storage, session storage,
IndexedDB, a service worker, a cookie, a URL, or another persistence mechanism.

Initial load and browser refresh show the login presentation. Cookie presence
is not inspected and does not restore protected presentation.

The protected fixture presentation provides explicit user actions for:

- invoking the protected fixture command;
- logging out.

Login success does not automatically invoke the protected command. This slice
adds no router or protected product route.

## Lifecycle End Presentation

The React fixture distinguishes a completed user-requested logout from a
session lifecycle that ended because the frontend reached its deadline or the
server rejected a protected command.

After a successful manual logout, the login presentation shows this primary
message exactly:

```text
You have signed out.
```

After either deadline handling or a valid stable `401 UNAUTHENTICATED`
protected-command response, the login presentation shows this primary message
exactly:

```text
Your session has ended. Please sign in again.
```

Deadline handling and server rejection remain distinct observable causes for
frontend coordination, but intentionally share one user-facing primary
message. The shared wording does not claim whether the backend session expired,
was revoked, was forcibly ended, or ended for another reason.

For a valid stable `401 UNAUTHENTICATED` response, the stable code and backend
safe message may remain visible as supplementary rejection detail. They must
not replace or reclassify the primary lifecycle-end message. Deadline logout
failure may likewise be shown only as supplementary safe cleanup detail and
must not replace the primary lifecycle-end message.

The manual-logout message is shown only after that manual request receives
`204 No Content`. A failed manual logout must not show it. If deadline handling
or server rejection ends the lifecycle while manual logout is pending, the
shared lifecycle-end message takes precedence and a later manual-logout
response must not overwrite it.

These fixed messages are product-neutral lifecycle status. They do not change
or authorize customer-owned branding, attribution, logo, copyright, or general
presentation overrides.

## Expiry Metadata Validation

An opted-in lifecycle may enter protected fixture presentation only when the
same successful login response provides exactly one acceptable
`Orca-Session-Expires-At` value.

An acceptable value:

- is present and non-blank;
- represents exactly one header value;
- parses as a UTC ISO-8601 instant;
- is later than the frontend clock value observed while processing that login
  response.

The frontend must not:

- derive expiry from cookie attributes;
- copy or import the backend session duration;
- calculate expiry by adding a duration to client time;
- read or parse `ORCA_SESSION`;
- treat expiry as proof that the session is currently valid.

Missing, blank, duplicate, malformed, non-UTC, or already-reached expiry
metadata makes that successful response unusable for this protected lifecycle.
The frontend then:

1. does not enter protected fixture presentation;
2. attempts the existing logout request at most once as best-effort cleanup;
3. displays the existing safe `REQUEST_UNAVAILABLE` presentation without a
   support reference;
4. submits no new client diagnostic category;
5. does not recursively report cleanup failure.

This cleanup policy does not claim that logout succeeded. It prevents the
frontend from presenting a lifecycle that it cannot coordinate safely.

## Presentation Deadline Coordination

After accepting login expiry metadata, the frontend schedules one memory-only
deadline for that login lifecycle.

- The delay is based only on the received absolute expiry instant and the
  frontend clock.
- No backend duration is duplicated.
- No countdown, warning, expiry timestamp, or remaining-time value is shown.
- A delayed browser timer does not extend the deadline or server session.
- When delayed work resumes at or after the deadline, deadline handling runs
  immediately.

When the deadline is handled, the frontend:

1. disables protected fixture actions and ends protected presentation
   immediately;
2. cancels the current lifecycle timer;
3. submits the existing logout request at most once;
4. returns to login presentation and shows `Your session has ended. Please
   sign in again.` regardless of logout success or failure;
5. does not restore protected presentation because of the logout response;
6. may display `REQUEST_UNAVAILABLE` only as supplementary cleanup detail if
   the logout request cannot be completed, but must not replace the primary
   lifecycle-end message or claim that the session remains valid.

Client clock skew, sleeping tabs, and timer throttling may affect when the
presentation transition runs. They never affect server-side validity. This
slice adds no clock-synchronization protocol and does not consume another
server header to estimate clock skew.

## Protected Fixture Command Behavior

The protected command is invoked only when the user selects the explicit
fixture action during protected presentation.

The frontend sends an empty JSON object with browser credentials. It does not
send actor, user, session, role, organization, or product data.

### Successful command

For `204 No Content`:

- the frontend shows a safe protected-command success result;
- the result contains no actor id, session id, cookie value, response body,
  identity, role, organization, or profile detail;
- the current presentation deadline remains unchanged;
- success does not renew or prove the session.

### Unauthenticated command

For a valid stable `401 UNAUTHENTICATED` response:

- server rejection takes precedence over the client timer;
- the frontend immediately disables protected actions;
- the current timer is cancelled;
- protected presentation ends;
- login presentation is restored;
- `Your session has ended. Please sign in again.` is displayed as the primary
  lifecycle-end message;
- the stable code and safe message may be displayed only as supplementary
  rejection detail;
- no cleanup logout is required to claim that the rejected session is invalid;
- a later response or timer callback must not restore the ended lifecycle.

### Other command failure

For another valid stable API failure:

- the frontend displays the stable code and safe message;
- it does not branch on message wording;
- it does not infer session state from a non-401 category;
- protected presentation remains available until logout, server rejection, or
  the existing deadline ends it.

For transport failure, malformed stable error, or unexpected success status:

- the frontend displays `REQUEST_UNAVAILABLE` without raw technical details;
- it does not introduce a new diagnostic category in this slice;
- it does not infer that the server session is valid or invalid;
- protected presentation remains bounded by the existing deadline.

## Manual Logout Behavior

The user may select an explicit logout action during protected presentation.

The frontend:

- disables protected actions while the manual logout request is in progress;
- submits exactly one empty JSON object to the existing logout endpoint with
  browser credentials;
- does not clear, parse, overwrite, or simulate the cookie or server session;
- does not expose session state in the request or result.

For `204 No Content`:

- the timer is cancelled;
- protected presentation ends;
- login presentation is restored;
- `You have signed out.` is displayed as the primary lifecycle-end message;
- no session, actor, or revocation detail is displayed.

For a transport failure, non-204 response, or malformed response:

- a safe error is displayed without technical details;
- protected presentation remains available for an explicit retry only while
  the current deadline has not been handled and no server rejection has ended
  it;
- the timer remains authoritative only as a presentation upper bound;
- the frontend does not claim logout or revocation succeeded.

If the deadline is reached while manual logout is in progress, deadline
handling wins: protected presentation ends, `Your session has ended. Please
sign in again.` remains the primary message, no second logout request is
started for the same lifecycle, and a later manual-logout response cannot
replace that message.

## Logout and Post-logout Rejection Proof

The React fixture contract verification must prove this sequence against the
existing public backend boundary:

1. login succeeds with valid expiry metadata;
2. protected fixture command succeeds;
3. React invokes the existing logout behavior;
4. the same browser session attempts the same protected fixture command;
5. the command is rejected as `401 UNAUTHENTICATED` using the stable API error
   contract;
6. no protected fixture behavior executes after rejection.

This proof may invoke the command through the fixture adapter after the UI has
returned to login presentation. It must not read a cookie value from frontend
application code or expose it through a public frontend type.

## Concurrency and Cleanup Rules

Frontend-local coordination must ensure:

- at most one active deadline exists for the current login lifecycle;
- a new accepted login lifecycle cancels obsolete timer work;
- unmounting cancels frontend timer work and makes no server-side logout claim;
- one user action cannot submit duplicate protected or logout commands while
  its request is in progress;
- manual logout and deadline handling start at most one logout request for the
  same lifecycle;
- a `401 UNAUTHENTICATED` result ends the lifecycle even when another request
  or timer is pending;
- a response belonging to an ended or replaced lifecycle cannot update the
  current lifecycle, restore protected presentation, or overwrite the primary
  lifecycle-end message;
- cleanup is frontend-local and introduces no polling, retry loop, keepalive,
  renewal, or browser persistence.

These are race-safety rules for presentation. They do not serialize or alter
server-side session commands.

## Scenarios

### Scenario: Login establishes protected fixture presentation

**Given**

- The React Minimal Consumer Fixture composes the public Orca login package.
- The backend accepts the submitted credentials.
- The successful response contains exactly one valid future expiry header.

**When**

- The public login composition processes the successful response.

**Then**

- The frontend accepts only the expiry instant as lifecycle metadata.
- One memory-only deadline is scheduled.
- The fixture shows explicit protected-command and logout actions.
- No protected command is invoked automatically.
- No cookie, session id, actor id, identity, role, organization, or profile data
  is read or displayed.

### Scenario: User invokes the protected fixture command

**Given**

- The fixture is in protected presentation before its deadline.

**When**

- The user selects the protected fixture action.

**Then**

- The frontend posts one empty JSON object with browser credentials.
- A `204` response produces a safe success result.
- The result exposes no actor or session data.
- The deadline is not changed.

### Scenario: Server rejection ends presentation before the deadline

**Given**

- The fixture has a future client presentation deadline.
- The server no longer accepts the browser session.

**When**

- The protected command returns stable `401 UNAUTHENTICATED`.

**Then**

- The timer is cancelled.
- Protected presentation ends immediately.
- Login presentation shows `Your session has ended. Please sign in again.` as
  the primary message.
- The stable code and safe message may remain visible only as supplementary
  rejection detail.
- The future client deadline does not override the server rejection.

### Scenario: User logs out and the same command is rejected

**Given**

- Login and the protected command previously succeeded.

**When**

- React submits the existing logout command and receives `204`.

**Then**

- Protected presentation ends and the timer is cancelled.
- Login presentation shows `You have signed out.` as the primary message.
- The same protected fixture command is rejected as unauthenticated when the
  contract proof invokes it again.
- Frontend application code does not clear or inspect the cookie.

### Scenario: Presentation deadline invokes existing logout

**Given**

- The fixture accepted a login expiry deadline.
- No earlier logout or server rejection ended the lifecycle.

**When**

- The frontend processes the deadline at or after the received instant.

**Then**

- Protected presentation ends immediately.
- React submits the existing logout request at most once.
- Login presentation is restored regardless of request outcome.
- Login presentation shows `Your session has ended. Please sign in again.` as
  the primary message regardless of request outcome.
- A failed or delayed logout does not extend or restore presentation.

### Scenario: Successful login has unusable expiry metadata

**Given**

- Login returns `204` but the expiry header is missing, blank, duplicate,
  malformed, non-UTC, or already reached.

**When**

- The opted-in lifecycle processes the response.

**Then**

- Protected presentation is not enabled.
- One best-effort cleanup logout is attempted.
- `REQUEST_UNAVAILABLE` is shown without a support reference.
- No new diagnostic is submitted and cleanup failure is not recursively
  reported.

### Scenario: Refresh does not restore presentation

**Given**

- A prior login may have established a server-side session.

**When**

- The browser reloads the React fixture.

**Then**

- The fixture shows login presentation.
- No cookie is inspected.
- No current-user, session-status, protected-command probe, or background poll
  is used to restore protected state.

## Acceptance Criteria

- The React fixture MUST reuse the public Orca login package and MUST NOT copy
  the login implementation.
- The public login integration MUST expose only validated expiry metadata to an
  opted-in lifecycle consumer.
- Existing consumers that do not opt in MUST retain the completed login-success
  presentation.
- Frontend lifecycle metadata MUST NOT contain a cookie value, session id,
  actor id, user id, identity, role, organization, membership, permission,
  personnel, or profile data.
- The lifecycle MUST require exactly one valid future UTC ISO-8601 expiry value
  from the successful login response.
- Invalid expiry metadata MUST NOT enable protected presentation.
- Invalid expiry metadata MUST trigger at most one best-effort cleanup logout
  and MUST show `REQUEST_UNAVAILABLE` without a new diagnostic category.
- The accepted deadline MUST be held only in memory.
- The frontend MUST NOT display a countdown, warning, expiry instant, or
  remaining duration.
- The frontend MUST NOT duplicate or calculate the backend session lifetime.
- Initial load and refresh MUST show login presentation without inspecting
  cookie presence or probing a protected endpoint.
- Protected command invocation MUST be an explicit user action.
- Protected and logout requests MUST send an empty JSON object with browser
  credentials.
- Protected command success MUST show no actor, session, cookie, identity,
  role, organization, or profile data.
- Protected command success MUST NOT change the presentation deadline.
- Stable `401 UNAUTHENTICATED` MUST immediately end protected presentation and
  cancel the timer.
- Stable `401 UNAUTHENTICATED` MUST show `Your session has ended. Please sign
  in again.` as the primary lifecycle-end message.
- The stable code and safe backend message from `401 UNAUTHENTICATED` MAY be
  shown only as supplementary rejection detail.
- Server rejection MUST take precedence over the client deadline.
- Valid non-401 stable errors MUST be displayed by code and safe message
  without inferring session validity.
- Transport, malformed, and unexpected results MUST expose no raw exception,
  response body, header, or technical detail.
- Manual logout `204` MUST cancel the timer, restore login presentation, and
  show `You have signed out.` as the primary lifecycle-end message.
- Failed manual logout MUST NOT claim revocation succeeded and MAY remain
  retryable only before the deadline or an earlier server rejection.
- Failed manual logout MUST NOT show the successful manual-logout message.
- Deadline handling MUST end protected presentation immediately and invoke the
  existing logout request at most once.
- Deadline handling MUST show `Your session has ended. Please sign in again.`
  as the primary lifecycle-end message regardless of logout outcome.
- Deadline logout failure MUST NOT extend or restore protected presentation.
- One lifecycle MUST have at most one active timer and at most one logout
  request across a manual-logout/deadline race.
- Stale asynchronous results MUST NOT restore an ended or replaced lifecycle.
- A stale manual-logout response MUST NOT replace the primary lifecycle-end
  message selected by deadline handling or server rejection.
- Frontend presentation MUST NOT describe deadline handling or stable
  `401 UNAUTHENTICATED` as forced logout, revocation, or proven session expiry.
- Contract verification MUST prove login, protected success, logout, and
  post-logout stable unauthenticated rejection.
- Frontend application code MUST NOT read, parse, clear, display, or persist
  `ORCA_SESSION`.
- This slice MUST preserve `frontend-03` branding, attribution, logo, and
  copyright behavior.
- This slice MUST NOT change backend auth, organization, reference-core,
  database, migration, or infrastructure behavior.

## Sensitive Data Boundary

Frontend public types, state, rendered output, diagnostics, logs, and tests must
not expose or persist:

- submitted password after the existing submission lifecycle;
- login identifier outside the existing form request lifecycle;
- raw `ORCA_SESSION` value or raw session id;
- raw request or response headers;
- raw request or response bodies;
- actor or user id;
- credential, registered-user, account, or internal session state;
- session revocation reason or validity proof;
- role, organization, membership, permission, personnel, or profile data;
- raw exception message, exception type, or stack trace;
- browser storage contents.

The validated expiry instant is the only new auth-owned client-visible metadata
accepted by this slice. The fixture does not display it.

## Verification Requirements

Public package and React component tests must verify:

- legacy login consumers remain compatible when lifecycle coordination is not
  enabled;
- opted-in login success accepts exactly one valid future expiry value;
- missing, blank, duplicate, malformed, non-UTC, and already-reached values do
  not enter protected presentation;
- invalid metadata triggers one cleanup logout and no recursive diagnostic;
- branding and mandatory Orca attribution remain unchanged;
- no forbidden auth or identity data enters public types or rendered output.

React fixture lifecycle tests must verify:

- initial load and refresh show login presentation;
- accepted login schedules one memory-only deadline;
- protected command is explicit rather than automatic;
- protected `204` shows a safe success without changing the deadline;
- stable `401 UNAUTHENTICATED` cancels the timer and ends protected
  presentation with `Your session has ended. Please sign in again.` as the
  primary message;
- stable `401 UNAUTHENTICATED` safe error detail, when retained, remains
  supplementary to the primary lifecycle-end message;
- another stable error is displayed without ending presentation;
- transport or malformed protected response uses a safe generic result;
- manual logout `204` returns to login, cancels the timer, and shows `You have
  signed out.` as the primary message;
- failed manual logout does not show the successful manual-logout message;
- failed manual logout remains safely retryable only before lifecycle end;
- deadline handling invokes logout once and returns to login regardless of
  logout outcome, showing `Your session has ended. Please sign in again.` as
  the primary message;
- manual logout, deadline, protected response, and replacement-login races
  ignore stale results, never restore ended presentation, and do not overwrite
  the primary lifecycle-end message selected by the winning transition;
- deadline and stable `401 UNAUTHENTICATED` presentation never claim forced
  logout, revocation, or proven session expiry;
- no cookie or sensitive value is read or displayed.

Consumer contract verification must prove through the existing backend:

- login response includes usable expiry metadata;
- protected fixture command succeeds;
- logout succeeds;
- the same protected command is rejected afterward with stable
  `401 UNAUTHENTICATED`;
- fixture behavior does not execute after rejection.

Build verification must include:

- public React login package consumption;
- Orca React reference application tests and build;
- React Minimal Consumer Fixture tests and build;
- Maven reactor tests, including the Java Minimal Consumer Fixture.

## Compatibility Boundary

This slice may extend the public React login package only to deliver the
approved safe successful-login expiry metadata and existing logout behavior to
an opted-in lifecycle consumer.

It must preserve:

- existing login request fields and endpoint;
- `frontend-01` success and stable-error presentation for consumers that do not
  opt in;
- `frontend-02` diagnostic classification and reference behavior;
- `frontend-03` package-root consumption and branding contract;
- auth-13 `204`, empty body, cookie, and expiry-header contract;
- auth-12 protected fixture command contract;
- auth-11 logout and revocation contract;
- reference-core-01 stable error contract.

## Non-Goals

- Frontend domain aggregate or authoritative client session.
- Cookie parsing, clearing, storage, or simulation.
- Current-user, profile, session-status, session-inspection, or restoration API.
- Browser-refresh restoration.
- Automatic protected command invocation after login.
- Countdown, warning, expiry display, or timeout navigation.
- Router, protected product route, workspace, organization UI, or real product
  domain.
- Background polling, keepalive, retry loop, or offline queue.
- Session renewal, sliding expiration, idle extension, refresh token, access
  token, remember-me, or session-duration duplication.
- OAuth, OIDC, SSO, MFA, hosted login, or cross-product session sharing.
- Account lifecycle or forced revocation behavior.
- New client diagnostic category or backend diagnostic behavior.
- Audit, structured logging, correlation, or retention expansion.
- `frontend-03` branding, attribution, logo fallback, copyright, design-system,
  or customer-owned presentation amendment.
- Vue or Angular implementation.
- Backend API, auth, reference-core, organization, persistence, migration, or
  infrastructure changes.
- Production hosting, CORS, clock synchronization, CDN, or deployment policy.

## Later Phase Boundary

This SDD authorizes no DDD, TDD, implementation, commit, merge, push, or pull
request by itself.

The derived DDD note must explain frontend state ownership, public package
boundary, adapters, timer lifecycle, race isolation, and test placement without
creating new behavior. TDD and implementation require their own explicit phase
authorizations.
