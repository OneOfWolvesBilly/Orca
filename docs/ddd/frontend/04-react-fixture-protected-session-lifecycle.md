# DDD Derivation - Frontend 04 React Fixture Protected Session Lifecycle

Status: Approved amendment / Implemented.

This note is **derived from**
`docs/specs/frontend/04-react-fixture-protected-session-lifecycle.md`.
It does not introduce new behavior.

## Scope Ownership

**frontend delivery support scope**

Rationale:

- Auth already owns login, server-side session creation, session expiry,
  protected actor resolution, logout, and revocation.
- Reference-core already owns the stable API error contract.
- The Java Minimal Consumer Fixture already owns the product-neutral protected
  command established by `auth-12`.
- This slice coordinates only React presentation state, existing HTTP command
  consumption, one memory-only deadline, and frontend-local race cleanup.

No frontend bounded context, product domain, or authenticated client-side
source of truth is introduced.

## No Aggregate Root

This slice introduces no aggregate root and no domain model.

The protected-session presentation is not authoritative session state:

- it is derived from one successful login result and its validated expiry
  metadata;
- it exists only in React memory;
- it can end because of manual logout, the presentation deadline, or an
  earlier server rejection;
- it cannot establish, prove, renew, extend, revoke, or restore a server-side
  session;
- it is discarded on refresh or unmount.

Auth's existing `AuthenticatedSession` remains the only session aggregate.
The frontend does not mirror that aggregate or its state transitions.

## Dependency Direction

```text
React Minimal Consumer Fixture
  -> @oneofwolvesbilly/orca-react-login public package root
     -> existing login adapter and presentation
     -> auth-13 expiry-header consumption
     -> existing logout HTTP adapter

React Minimal Consumer Fixture
  -> fixture-owned protected-command adapter
     -> POST /api/fixture/actor-context-check

All browser requests
  -> existing backend public HTTP contracts
     -> auth / reference-core / fixture backend behavior
```

The dependency must not reverse from Orca frontend code into fixture
components, fixture HTTP paths, or fixture result text.

The fixture may depend on the Orca public package and its own command adapter.
It must not import Orca frontend internals, backend implementation packages, or
auth persistence details.

## Public React Package Boundary

The existing package remains:

```text
@oneofwolvesbilly/orca-react-login
```

`OrcaLogin` remains the only login composition. The package should add the
smallest opt-in session-coordination surface needed by the specification.

Recommended public concepts:

- `OrcaLoginSession`
  - immutable successful-login presentation value;
  - contains only `expiresAt` as the validated UTC ISO-8601 value;
  - contains no raw response, cookie, session id, actor, identity, role,
    organization, or profile data.

- optional `onSessionEstablished` input on `OrcaLogin`
  - opts a consumer into expiry validation and session-aware handoff;
  - receives one `OrcaLoginSession` only after the auth-13 header is valid and
    later than the frontend clock;
  - lets the fixture replace the login composition with its protected
    presentation;
  - does not invoke a protected command automatically.

- `logoutOrcaSession`
  - invokes only the existing `POST /api/auth/logout` contract;
  - sends `{}` with browser credentials;
  - returns a safe discriminated result rather than a raw `Response`;
  - exposes no session state and does not clear or inspect cookies.

These names are implementation guidance. The authoritative requirements are
the opt-in compatibility, safe expiry-only handoff, existing logout
consumption, and sensitive-data exclusions in the spec.

The package export map remains root-only. Adding approved root exports for this
slice must not create deep or wildcard exports.

## Legacy Login Compatibility

The public login component has two composition modes.

### Existing login-result mode

When no session-established consumer is supplied:

- `204 No Content` retains the existing `frontend-01` success presentation;
- the consumer may ignore the auth-13 header;
- existing `frontend-02` error and diagnostic behavior remains unchanged;
- branding, fallback, attribution, and copyright remain unchanged.

### Opted-in lifecycle mode

When a session-established consumer is supplied:

- `204 No Content` is accepted for protected presentation only after strict
  expiry validation;
- valid metadata is mapped to the expiry-only public value;
- invalid metadata remains inside the package's safe login-result path;
- invalid metadata starts at most one cleanup logout and produces
  `REQUEST_UNAVAILABLE` without a client failure reference;
- cleanup failure is swallowed after the safe result and never becomes a
  recursive diagnostic.

This split preserves completed clients while making the fixture's stronger
lifecycle precondition explicit.

## Expiry Value and Parser

Recommended internal value:

```text
SessionPresentationExpiry
  original UTC ISO-8601 value
  parsed epoch instant used only for scheduling/comparison
```

It is a frontend adapter value, not a domain value or auth token.

The parser belongs inside the public login package because that package owns
the login `Response` and must keep the raw response out of consumer code.

Parser responsibilities derived from the spec:

- read only `Orca-Session-Expires-At` from the successful response;
- reject missing, blank, duplicate, malformed, non-UTC, or non-future input;
- preserve only the authorized expiry value after validation;
- compare it with the injected frontend clock;
- never consult cookie text, cookie attributes, a backend duration, or another
  header;
- never add a client duration to the current time.

Browser `Headers` implementations may combine duplicate values. The adapter
must treat an accessible combined value as duplicate rather than selecting a
first or last value. The exact parsing mechanism is an adapter detail; the
spec's exactly-one-value rule remains authoritative.

## Public Package Result Model

The existing login adapter result may be extended conceptually as:

```text
LoginResult
  success
  session-established(expiry)
  stable-error(presentation)
  generic-error(presentation)
```

- `success` remains the legacy `204` outcome.
- `session-established` is available only for opted-in valid metadata.
- `stable-error` preserves reference-core and auth-owned login rejection.
- `generic-error` continues to use the frontend error catalog.

No result variant contains `Response`, headers, cookies, session ids, actors,
passwords, login identifiers, or exception objects.

The public component maps `session-established` to its opted-in consumer rather
than rendering the legacy success panel. All other result presentation remains
inside the existing package.

## Logout Adapter

The logout adapter belongs in the public React package because logout is an
Orca auth contract consumed by the lifecycle and must not be copied into every
consumer.

Conceptual safe result:

```text
LogoutResult
  success
  stable-error(presentation)
  generic-error(presentation)
```

Responsibilities:

- submit `POST /api/auth/logout`;
- include `Content-Type: application/json`;
- include browser credentials;
- serialize only `{}`;
- treat `204` as success;
- parse a valid unsuccessful stable error by status, code, and safe message;
- map transport, malformed, or unexpected results to
  `REQUEST_UNAVAILABLE`;
- return no raw response, header, cookie, session, actor, or exception data;
- perform no retry, polling, renewal, cookie cleanup, or recursive diagnostic.

The invalid-login-metadata cleanup path calls the same adapter but deliberately
ignores its result after starting it once.

## React Fixture Presentation Model

Recommended fixture composition responsibility:

```text
FixtureSessionLifecycle
  login presentation
  protected presentation
  current lifecycle generation
  current expiry deadline
  current operation state
  safe current result
  lifecycle-end presentation
```

The fixture component owns no credentials beyond the existing `OrcaLogin`
form lifecycle and receives no actor or session id.

### Presentation phase

Recommended phase representation:

```text
login
protected
```

In-progress state is orthogonal to phase:

```text
idle
protected-command
manual-logout
deadline-logout
```

This avoids treating an in-flight request as a third kind of authentication
state. The phase answers what is presented; operation state controls disabled
actions and race handling.

### Protected result

Recommended presentation-only result:

```text
none
protected-success
stable-error(code, safe message)
generic-error(REQUEST_UNAVAILABLE, safe message)
```

It contains no actor, session, response body, technical cause, role,
organization, or profile information.

### Lifecycle-end presentation

The fixture owns a separate presentation-only result for transitions back to
login:

```text
LifecycleEndPresentation
  manual-logout-completed
  session-ended(optional supplementary safe error)
```

The fixed primary copy is mapped by the fixture presentation layer:

| Observable transition | Presentation variant | Primary message |
| --- | --- | --- |
| Manual logout receives `204` while its lifecycle is current | `manual-logout-completed` | `You have signed out.` |
| The frontend handles the accepted deadline | `session-ended` | `Your session has ended. Please sign in again.` |
| Protected command receives stable `401 UNAUTHENTICATED` | `session-ended` | `Your session has ended. Please sign in again.` |

The presentation variant is not an auth result, backend error code, diagnostic
category, or session-revocation reason. Deadline and server rejection remain
separate coordinator inputs even though they map to the same visible primary
copy.

`LifecycleEndPresentation` remains private to the React Minimal Consumer
Fixture. The amendment requires no new public package export and no change to
the login, logout, or protected-command adapter result contracts.

Only the stable `401 UNAUTHENTICATED` path may attach its existing safe error
presentation as supplementary rejection detail. Deadline cleanup failure may
optionally attach `REQUEST_UNAVAILABLE` as supplementary cleanup detail, but
the smallest implementation may ignore that cleanup result as before. Neither
supplementary detail can replace the primary message.

The fixture must not derive presentation behavior from backend message text.
It branches only on the stable status/code pair already recognized by the
protected-command adapter and on frontend-observed lifecycle transitions.
A new accepted login clears the prior lifecycle-end presentation before the
fixture enters protected presentation.

## Fixture Protected-command Adapter

The protected command belongs to the Minimal Consumer Fixture, not to the Orca
login package.

Recommended placement:

```text
minimal_consumer_fixture/react/src/api/fixtureActorContext.ts
```

Responsibilities:

- submit `POST /api/fixture/actor-context-check`;
- send `{}` with browser credentials;
- treat `204` as safe success;
- recognize a valid `401 UNAUTHENTICATED` stable response as lifecycle-ending;
- return other valid stable errors without reclassifying them;
- map transport, malformed, or unexpected results to the existing frontend
  generic presentation;
- return no raw `Response`, body, header, cookie, session, or actor data.

The adapter consumes the stable error contract. It does not redefine backend
codes or parse safe message wording for behavior.

## Frontend Clock and Deadline Scheduler

Time and scheduling are frontend adapter dependencies.

Recommended abstractions:

```text
FrontendClock
  now(): epoch milliseconds

DeadlineScheduler
  schedule(deadline, callback): cancellation
```

Production adapters may use `Date.now`, `setTimeout`, and `clearTimeout`.
Tests use a fixed clock and fake timers.

The scheduler:

- receives the absolute parsed deadline;
- derives only the delay between that deadline and the current frontend clock;
- owns one cancellation handle for the current lifecycle;
- executes immediately when processing observes that the deadline has already
  arrived;
- does not display time, calculate a backend lifetime, poll, or renew.

Clock skew and browser throttling remain delivery limitations. The scheduler
does not consume the HTTP `Date` header or introduce a synchronization model.

## Lifecycle Generation and Stale-result Isolation

Each accepted login lifecycle receives a frontend-local generation token.

The token:

- is generated only inside the React fixture;
- is not a session id and has no auth meaning;
- is never sent to the backend or displayed;
- lets callbacks determine whether they still belong to the current
  presentation lifecycle.

Before applying an asynchronous result, the component checks that its captured
generation is still current.

A lifecycle is ended or replaced when:

- manual logout succeeds, producing `manual-logout-completed`;
- the deadline is handled, producing `session-ended`;
- protected command returns stable `401 UNAUTHENTICATED`, producing
  `session-ended` with optional supplementary safe rejection detail;
- a later valid login establishes a replacement lifecycle;
- the component unmounts.

Ending a lifecycle invalidates its generation and cancels its timer. A stale
protected response, logout response, or timer callback then has no authority to
restore presentation, modify the new lifecycle, or replace its lifecycle-end
presentation.

This is frontend race isolation only. It is not a distributed lock, server
transaction, session version, or concurrency protocol.

## Logout Request Deduplication

The fixture tracks whether logout has already started for the current
generation.

- Manual logout sets the marker before starting the request.
- If the deadline arrives during that request, protected presentation ends but
  no second logout request starts.
- If deadline logout starts first, manual actions are no longer available.
- Invalid login metadata uses a separate one-shot cleanup before a protected
  lifecycle exists.
- No retry loop is added. A failed manual logout may be retried only through a
  later explicit user action before lifecycle end.

The marker is memory-only and has no server-side meaning.

## Rule Placement

### Auth rules

- Auth decides login success, expiry, protected-session validity, logout, and
  revocation.
- Auth remains authoritative when client presentation and server state differ.
- No auth rule or backend code change is derived.

### Reference-core rules

- Reference-core defines stable error fields and codes.
- Frontend adapters validate and present the contract without copying exception
  details or inventing new backend categories.
- No reference-core change is derived.

### Public login package rules

- Preserve the single existing login implementation.
- Keep lifecycle participation opt-in for compatibility.
- Validate and expose only the authorized expiry value.
- Own the reusable existing logout adapter.
- Preserve existing login errors, diagnostics, branding, and sensitive-data
  boundaries.

### React fixture rules

- Own memory-only presentation phase, generation, deadline, operation state,
  and safe fixture results.
- Invoke its protected command only from an explicit user action.
- End presentation on stable `401 UNAUTHENTICATED` before any client timer
  claim.
- Map successful manual logout to `manual-logout-completed` and map deadline or
  stable `401 UNAUTHENTICATED` to `session-ended`.
- Coordinate manual and deadline logout without duplicate requests.
- Ignore stale asynchronous results, including results that would overwrite a
  lifecycle-end presentation selected by an earlier winning transition.

### Browser adapter rules

- Let the browser receive and send the HttpOnly cookie through
  `credentials: include`.
- Never inspect, parse, clear, persist, or simulate that cookie.
- Use no storage, service worker, polling, keepalive, or session probe.

### Backend and infrastructure rules

- Reuse existing routes, controllers, application behavior, persistence, and
  Flyway migrations unchanged.
- Add no backend module, endpoint, table, cache, clock, or configuration.

## Component Placement

Recommended repository placement:

```text
orca_frontend/
  packages/react-login/
    src/
      OrcaLogin.tsx
      index.ts
      internal/api/login.ts
      internal/api/logout.ts
      internal/session/expiry.ts

minimal_consumer_fixture/
  react/
    src/
      FixtureSessionLifecycle.tsx
      api/fixtureActorContext.ts
      main.tsx
```

Exact filenames are implementation details. The required ownership is:

- login response and expiry parsing stay in the public login package;
- reusable Orca logout HTTP mapping stays in the public login package;
- fixture command mapping and protected presentation stay in the fixture;
- no fixture route or copy of the login implementation enters the Orca
  package.

The Orca React reference host may remain a legacy login-result consumer. It is
not required to become a protected fixture or duplicate the fixture lifecycle.

## UI Composition Boundary

The fixture initially renders the existing `OrcaLogin` with its existing
customer branding input.

After a valid session-established handoff, the fixture renders a small
product-neutral panel containing:

- the existing configured product identity as presentation context;
- one explicit protected-command action;
- one explicit logout action;
- one safe result area.

The protected panel must not become a workspace, navigation shell,
organization console, profile view, or branding amendment.

Protected-command copy and styling remain presentation implementation details
as long as the visible result distinguishes safe success, stable error, and
generic error without exposing forbidden data. Lifecycle-end primary copy is
fixed by the authoritative SDD and is rendered adjacent to the restored login
presentation through a dedicated safe status view.

That status view should use status semantics for the successful
user-requested logout and lifecycle-end notification. Supplementary stable or
generic error detail, when present, retains the existing safe error semantics.
The fixed lifecycle copy is product-neutral and does not add a customer
branding or general copy override.

## Error Mapping

### Login metadata error

```text
204 + unusable expiry
  -> one cleanup logout attempt
  -> generic REQUEST_UNAVAILABLE without support reference
  -> remain in login presentation
```

No client diagnostic is submitted because frontend-02 defines no category for
this lifecycle-specific contract failure.

### Protected command

```text
204
  -> protected-success

401 UNAUTHENTICATED
  -> end lifecycle
  -> login presentation
  -> primary session-ended message
  -> optional supplementary stable safe error

other valid stable error
  -> keep lifecycle + stable safe error

transport / malformed / unexpected
  -> keep lifecycle + REQUEST_UNAVAILABLE
```

### Manual logout

```text
204
  -> end lifecycle
  -> login presentation
  -> primary manual-logout-completed message

valid stable error
  -> keep lifecycle before deadline + stable safe error

transport / malformed / unexpected
  -> keep lifecycle before deadline + REQUEST_UNAVAILABLE
```

### Deadline logout

```text
deadline observed
  -> end lifecycle immediately
  -> start logout once if not already started
  -> login presentation regardless of result
  -> primary session-ended message
  -> optional supplementary REQUEST_UNAVAILABLE on failure
```

None of these mappings infer a hidden session reason or override auth-owned
server decisions. In particular, neither deadline nor `401 UNAUTHENTICATED`
is labelled as forced logout, revocation, or proven expiry.

## Sensitive Data Design

Use data minimization at construction boundaries rather than redaction.

Public login handoff receives only:

- validated expiry string.

Logout adapter receives only:

- no session input;
- fixed empty command body.

Fixture protected adapter receives only:

- no actor or session input;
- fixed empty command body.

Lifecycle state receives only:

- frontend-local generation;
- parsed presentation deadline;
- phase and operation state;
- safe result code/message;
- lifecycle-end presentation variant and optional supplementary safe error.

The following values must never enter those boundaries:

- cookie or raw session id;
- actor or user id;
- password or login request;
- raw `Response`, body, or headers;
- exception object or stack trace;
- credential, registered-user, account, role, organization, membership,
  permission, personnel, or profile data;
- browser storage contents.

## Test Layer Placement

### Public package tests

Validate:

- legacy `204` login success remains unchanged without opt-in;
- opt-in valid expiry produces the expiry-only handoff;
- missing, blank, duplicate, malformed, non-UTC, and already-reached values
  produce one cleanup logout and safe generic result;
- cleanup failure does not recurse or submit a client diagnostic;
- public root exports only the previously supported surface plus the minimum
  approved frontend-04 session coordination surface;
- deep imports remain unavailable;
- branding, attribution, logo, and copyright tests remain unchanged;
- no public type contains raw response, cookie, session, actor, identity, role,
  organization, or profile data.

### Logout adapter tests

Validate:

- request method, path, `{}` body, JSON content type, and browser credentials;
- `204` safe success;
- valid stable error mapping;
- transport, malformed, and unexpected result mapping;
- no raw response or technical details escape;
- no retry or diagnostic recursion.

### React fixture component tests

Validate with fixed clock and fake timers:

- initial login presentation and no restoration probe;
- valid handoff schedules one deadline and shows explicit actions;
- no automatic protected command;
- protected success and safe data exclusions;
- stable 401 ends lifecycle and cancels the timer;
- stable 401 shows the fixed session-ended primary message and retains any
  stable safe error only as supplementary detail;
- other stable error retains lifecycle;
- generic protected failure remains deadline-bounded;
- manual logout success returns to login with the fixed signed-out primary
  message;
- failed manual logout never shows the signed-out primary message;
- failed manual logout remains explicitly retryable before deadline;
- deadline ends presentation, starts logout once, and shows the fixed
  session-ended primary message regardless of cleanup result;
- manual logout/deadline race does not duplicate logout;
- stale protected, logout, timer, and replacement-lifecycle results are ignored
  and cannot overwrite the winning lifecycle-end presentation;
- deadline and stable 401 presentation never claims forced logout, revocation,
  or proven session expiry;
- unmount cancels local timer work without claiming logout.

### Fixture adapter tests

Validate:

- protected command method, path, `{}` body, JSON content type, and browser
  credentials;
- `204` success;
- stable `401 UNAUTHENTICATED` distinction;
- other stable error preservation;
- generic safe mapping for transport, malformed, and unexpected results;
- no actor, session, cookie, raw response, or technical detail escapes.

### Cross-boundary contract proof

The existing Java `EmbeddedAuthConsumerContractTest` remains the real backend
proof for login, protected actor resolution, logout, and post-logout rejection.
It should also assert the auth-13 login expiry header needed by the React
lifecycle without reading that value from frontend code.

The React fixture suite proves the browser request sequence and presentation
coordination through its public adapters. Together these suites prove the
frontend/backend contract without adding a browser cookie parser or duplicating
backend behavior.

### Regression and build verification

Run:

- all existing Orca React tests;
- public package contract and branding tests;
- React Minimal Consumer Fixture tests;
- Orca React build;
- React Minimal Consumer Fixture build;
- Maven reactor tests.

No backend production-code test change is derived. A contract assertion may be
added to the existing fixture test for the already-implemented auth-13 header.

## TDD Order

After explicit TDD authorization, the recommended RED sequence is:

1. fixture lifecycle-end primary-message and failure-exclusion tests;
2. deadline, server-rejection, and manual-logout message-precedence tests;
3. public opt-in expiry handoff and legacy compatibility tests;
4. invalid expiry cleanup and logout adapter tests;
5. fixture protected-command adapter tests;
6. fixture component phase and explicit-action tests;
7. fake-timer deadline tests;
8. server-rejection and stale-result race tests;
9. cross-boundary login/protected/logout/rejection contract assertion;
10. branding, public-export, build, and Maven regression verification.

TDD RED must not modify production implementation and must not be committed
while failing.

## Expected Implementation Boundary After Authorization

Implementation is expected to remain within:

- the public React login package's safe login-result, expiry-parser, and logout
  adapter boundary;
- the React Minimal Consumer Fixture lifecycle component and protected-command
  adapter;
- relevant React package/fixture tests and build configuration;
- an auth-13 header assertion in the existing Java fixture contract test if
  needed for the cross-boundary proof.

Implementation must not modify:

- backend auth, organization, reference-core, persistence, migrations, or
  infrastructure behavior;
- `frontend-03` branding, logo, attribution, or copyright rules;
- customer-owned presentation behavior;
- Vue or Angular code.

## Unknowns Preserved

- Production hosting and clock-synchronization policy.
- Future renewal or replacement-expiry behavior.
- Future countdown, warning, routing, or protected product navigation.
- Vue and Angular lifecycle ports.

No model, port, UI, or configuration is added for these unknowns.

## Non-Goals

- Frontend domain aggregate or authoritative client session.
- Mirroring `AuthenticatedSession` in TypeScript.
- Cookie parsing, clearing, storage, or simulation.
- Current-user, profile, session-status, inspection, or restoration API.
- Browser-refresh session restoration.
- Automatic protected command after login.
- Countdown, warning, expiry display, or timeout routing.
- Router, product navigation, workspace, organization UI, or product domain.
- Background polling, keepalive, retry loop, offline queue, or browser
  persistence.
- Session renewal, sliding expiration, idle extension, refresh token, access
  token, remember-me, or copied backend duration.
- OAuth, OIDC, SSO, MFA, hosted login, or cross-product session sharing.
- New client diagnostic category or backend diagnostic behavior.
- Audit, logging, correlation, or retention expansion.
- Branding, attribution, logo fallback, copyright, design-system, or
  customer-owned presentation changes.
- Vue or Angular implementation.
- Backend API, domain, application, persistence, migration, infrastructure, or
  deployment changes.

## Later Phase Boundary

This DDD note authorizes no tests, implementation, commit, merge, push, or pull
request by itself.

TDD must first express the approved behavior as failing tests, preserve all
existing tests, report the exact failure count, and stop for explicit
implementation authorization.
