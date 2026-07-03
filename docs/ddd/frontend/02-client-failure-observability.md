# DDD Derivation - Frontend 02 Client Failure Observability

Status: Approved / React implementation pending / Vue and Angular ports planned.

This note is **derived from**
`docs/specs/frontend/02-client-failure-observability.md`.
It does not introduce new behavior.

## Scope Ownership

**frontend delivery support scope**

The slice consumes existing auth and reference-core contracts.

- Auth owns login success, login rejection, session behavior, and
  `loginFailureReferenceId`.
- Reference-core owns the stable API error contract, client diagnostic
  persistence, `clientFailureReferenceId`, and `IT_ADMIN` lookup.
- Frontend delivery owns client-side response classification, one diagnostic
  submission attempt, and safe reference presentation.

No frontend domain bounded context or business aggregate is introduced.

## No Aggregate Root

The frontend state introduced here is request adapter and presentation state.

- It is not authoritative business state.
- It is not persisted by the frontend.
- It does not decide whether login succeeds.
- It does not decide whether a diagnostic record exists; it trusts only a valid
  reference-core-02 creation response.

## Framework-Neutral Behavior

All framework ports implement the same conceptual pipeline:

```text
submit password login
-> classify response
-> return stable backend result
   or attempt one safe client diagnostic
-> present REQUEST_UNAVAILABLE
   with reference only after diagnostic success
```

Shared concepts:

- error presentation:
  - code
  - safe message
  - optional support reference label and value
- client diagnostic category
- client operation
- framework application identifier
- client diagnostic creation response
- generic client failure result with optional client failure reference
- stable API error result with optional login failure reference

These concepts are duplicated locally in each independently buildable
framework application unless a later slice explicitly approves a shared
versioned non-UI package.

## Frontend Error Catalog

Each framework application owns a small local module that maps frontend-owned
codes to safe presentation definitions.

React recommendation:

```text
src/errors/clientErrorCatalog.ts
```

Minimum conceptual types:

```text
ClientErrorCode
ClientErrorDefinition
ErrorPresentation
SupportReference
```

The initial `ClientErrorCode` set contains only `REQUEST_UNAVAILABLE`.

The catalog must not:

- redefine backend stable codes such as `LOGIN_REJECTED` or `INTERNAL_ERROR`
- contain `TRANSPORT_FAILURE`, `MALFORMED_RESPONSE`, or
  `UNEXPECTED_RESPONSE`
- receive exception objects, raw response bodies, passwords, or session values
- become a cross-framework React package

Backend stable errors are normalized directly from their safe API response into
`ErrorPresentation`. Frontend-generated failures use the catalog.

## React Reference Boundary

The React implementation remains under:

```text
orca_frontend/react/
```

Minimum changes:

- extend the login API adapter to classify client failures
- add a small diagnostic API adapter
- extend the generic login result with optional
  `clientFailureReferenceId`
- extend `LoginResultView` with a distinct client failure reference label
- add adapter and presentation tests

No router, global state store, service worker, logging SDK, or data-fetching
framework is derived.

## Framework Port Boundary

| Framework | Location | frontend-02 status |
| --- | --- | --- |
| React | `orca_frontend/react/` | Reference implementation pending |
| Vue | `orca_frontend/vue/` | Planned |
| Angular | `orca_frontend/angular/` | Planned |

Each port owns:

- framework runtime
- request adapter
- state integration
- component implementation
- tests
- build configuration

A port must not import another framework's UI component, state object, hook,
service, or runtime adapter.

## Minimum Client Model

### Stable backend result

- kind: `stable-error`
- code
- safe message
- optional `loginFailureReferenceId` only for `LOGIN_REJECTED`

### Client failure result

- kind: `generic-error`
- optional `clientFailureReferenceId`

The client failure result does not retain:

- diagnostic category
- response status
- raw exception
- raw response body
- diagnostic request or response

Before rendering, both stable and client failure results are normalized to:

- code
- safe message
- optional support reference:
  - label
  - value

The rendering component does not branch on transport, malformed response, or
server implementation details.

### Diagnostic command

- category
- operation: `PASSWORD_LOGIN`
- client application
- optional response status

The diagnostic command is an HTTP adapter model matching reference-core-02. It
is not a domain model.

## Response Classification

Recommended React adapter ordering:

1. Call native `fetch` for password login.
2. If `fetch` rejects:
   - classify `TRANSPORT_FAILURE`
   - attempt diagnostic without response status
3. If status is `204`:
   - return success
4. If `response.ok` is true:
   - classify `UNEXPECTED_RESPONSE`
   - attempt diagnostic with response status
5. Otherwise parse stable API error:
   - require object body
   - require numeric body status equal to HTTP status
   - require non-blank code and message
   - require optional login failure reference to be a string
6. If parsing fails:
   - classify `MALFORMED_RESPONSE`
   - attempt diagnostic with response status
7. If parsing succeeds:
   - return stable backend error
   - do not submit client diagnostic

This ordering preserves frontend-01 success and stable-error behavior.

## Diagnostic Adapter

The diagnostic adapter:

- submits only reference-core-02 allowlisted fields
- accepts only `201` with a non-blank string
  `clientFailureReferenceId`
- catches its own transport and parsing failures
- returns no reference on any failure
- never calls itself recursively

The adapter must not receive the login request object, password, login
identifier, raw response body, or exception object.

Passing a coarse category and optional numeric response status into the adapter
is the structural safety boundary.

## Reference Presentation

`LoginResultView` keeps references semantically distinct:

- stable login rejection:
  - `Login failure reference`
  - auth-owned `loginFailureReferenceId`
- generic client failure:
  - `Client failure reference`
  - reference-core-owned `clientFailureReferenceId`

The generic result remains:

- code: `REQUEST_UNAVAILABLE`
- safe generic message

No technical category or HTTP status is rendered.

## Failure Isolation

Diagnostic reporting is best effort.

- Login request outcome remains the primary result.
- Diagnostic failure must not replace the primary safe display with another
  error.
- No retry, queue, timer, browser storage, or service worker is introduced.
- One client failure produces at most one diagnostic request.

This prevents recursive observability failures and unbounded client behavior.

## Sensitive Data Design

Use construction from allowlisted constants, not redaction.

The diagnostic adapter receives:

- category enum
- operation constant
- framework application constant
- optional numeric response status

It does not receive:

- `LoginRequest`
- `Response` body
- `Error` or exception
- headers
- cookie or browser storage
- auth or organization state

This keeps forbidden values outside the diagnostic boundary rather than
attempting to scrub them later.

## Test Layer Placement

React adapter tests:

- `204` returns success without diagnostic
- valid `LOGIN_REJECTED` remains stable and preserves
  `loginFailureReferenceId`
- valid non-login stable error creates no diagnostic
- fetch rejection submits `TRANSPORT_FAILURE` without response status
- malformed unsuccessful response submits `MALFORMED_RESPONSE` with status
- body status mismatch is malformed
- successful non-`204` response submits `UNEXPECTED_RESPONSE`
- successful diagnostic returns a client failure reference
- diagnostic fetch rejection returns generic failure without reference
- diagnostic non-`201` response returns generic failure without reference
- malformed diagnostic response returns generic failure without reference
- each original failure attempts at most one diagnostic
- diagnostic payload contains no login request or forbidden field

React presentation tests:

- generic failure with reference displays `REQUEST_UNAVAILABLE`
- generic failure with reference displays `Client failure reference`
- generic failure without reference displays no reference
- login rejection displays `Login failure reference`
- references never appear under the wrong label
- raw exception, response, password, and diagnostic category remain absent

Each future Vue and Angular port requires an equivalent suite in its own test
tooling. React tests do not mark those ports implemented.

## Backend Test Boundary

No backend changes or backend tests are required by frontend-02.

Reference-core-02 integration tests remain authoritative for:

- diagnostic ingestion
- persistence-before-reference behavior
- `IT_ADMIN` lookup
- sensitive field rejection

Frontend tests use mocked HTTP responses and verify client consumption only.

## Local and Production Boundary

Local:

- Vite proxies both `/api/auth/login` and `/api/client-diagnostics`
- the already-loaded login shell can render `REQUEST_UNAVAILABLE`
- browser and Vite console output may assist development but is not part of the
  contract

Production:

- uses the same relative `/api` contracts
- hosting, CORS, rate limiting, retention, and external telemetry remain
  deployment/reference-core concerns

No environment-specific logging implementation is derived.

If the initial frontend HTML or JavaScript cannot load, no framework component
can render this state. Hosting fallback behavior belongs to a separate future
slice.

## Future Boundaries

Possible later slices:

- diagnostic lookup UI for `IT_ADMIN`
- correlation/request id propagation
- retry or offline reporting, only if a workflow requires it
- retention and cleanup
- Vue frontend-02 port
- Angular frontend-02 port

These are not part of the React reference implementation.

## Non-Goals

- Frontend domain aggregate.
- Backend or schema changes.
- Frontend hosting fallback before the application loads.
- Service worker, PWA, CDN, or reverse-proxy offline page.
- Auth role changes.
- IT admin diagnostics UI.
- Diagnostic search or dashboard.
- External telemetry SDK.
- Console logging contract.
- Retry, offline queue, or browser persistence.
- Router or global state management.
- Shared cross-framework UI or runtime adapter.
- Vue or Angular scaffolding.
