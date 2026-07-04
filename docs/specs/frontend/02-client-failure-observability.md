# Frontend 02 - Client Failure Observability

Status: Approved / React reference implemented / Vue and Angular ports planned.

## Goal

Make client-observed password-login failures supportable without exposing raw
technical details to the user.

This behavior starts only after the frontend login shell has loaded in the
browser. When the login client encounters a transport failure, malformed
unsuccessful response, or unexpected successful response, it attempts to store
one safe diagnostic through `reference-core-02`. The user continues to see the
stable `REQUEST_UNAVAILABLE` result and sees a `clientFailureReferenceId` only
when the diagnostic was successfully persisted.

This behavior applies to React, Vue, and Angular. React remains the first
reference implementation. Vue and Angular remain planned ports.

## Workflow Traceability

- Workflows:
  - Frontend Reference Shell
  - Error and Exception Handling
  - Logging, Observability, and Operations
- Workflow gap:
  - frontend-01 collapses client runtime failures into a safe result
  - the user and IT cannot currently connect that result to a persisted client
    diagnostic
  - reference-core-02 now provides a queryable diagnostic destination
- Primary actor:
  - Registered User using the login shell
- Supporting actors:
  - frontend framework application
  - reference-core client diagnostics endpoint
  - `IT_ADMIN` using reference-core diagnostic lookup
- Predecessor slices:
  - `frontend-01` login result shell
  - `reference-core-01` stable API error contract
  - `reference-core-02` client diagnostics foundation
  - `auth-08` password login with server-side session
  - `auth-10` login failure audit

## Frontend Delivery Support Scope

`frontend-02` is a frontend delivery slice. It does not own auth decisions,
stable backend error classification, diagnostic persistence, or IT admin
authorization.

Auth remains authoritative for:

- password login success and rejection
- `ORCA_SESSION`
- `loginFailureReferenceId`

Reference-core remains authoritative for:

- stable API error responses
- accepted client diagnostic fields
- diagnostic persistence
- `clientFailureReferenceId`
- `IT_ADMIN` diagnostic lookup

Frontend hosting failure before the login shell loads is not observable by
React, Vue, or Angular application code and is outside this slice.

## Unified Error Presentation

Frontend result components consume one presentation object:

```text
ErrorPresentation
  code
  message
  optional supportReference
    label
    value
```

- `code` is the safe code displayed to the user.
- `message` is safe client-visible text.
- `supportReference` is present only when a queryable reference exists.
- The presentation component does not determine whether a reference came from
  auth, reference-core client diagnostics, or a future server diagnostic.
- The API adapter supplies the appropriate reference label and value.

Backend stable error codes and messages remain backend-owned. The frontend must
not copy them into its own catalog or redefine their meaning.

## Frontend Error Catalog

Each framework application owns a small framework-local catalog for
frontend-generated presentation errors.

The initial catalog contains:

```text
REQUEST_UNAVAILABLE
  safe message: We could not complete the login request. Please try again.
```

The catalog:

- maps a frontend-owned code to fixed safe presentation text
- does not contain backend stable error codes
- does not contain diagnostic categories
- is consumed by adapter/presentation composition rather than duplicated in UI
  components
- may gain another client-visible code only after authoritative behavior
  requires it

React, Vue, and Angular implement equivalent catalogs in their own
applications. They do not import a React catalog or framework runtime from one
another.

## Failure Categories

The frontend maps only the following client-observed conditions to the
reference-core-02 diagnostic categories.

### `TRANSPORT_FAILURE`

Use when the password-login request does not produce an HTTP `Response`, such
as when native `fetch` rejects.

The diagnostic request must not include `responseStatus`.

### `MALFORMED_RESPONSE`

Use when an unsuccessful password-login HTTP response is received but its body
does not conform to the stable API error contract consumed by frontend-01.

This includes:

- body is not parseable JSON
- body is not an object
- required `status`, `code`, or `message` is missing or invalid
- body `status` does not equal the HTTP response status
- `loginFailureReferenceId` has an invalid type

The diagnostic may include the received HTTP response status. It must not
include the raw response body.

### `UNEXPECTED_RESPONSE`

Use when the password-login request receives a successful HTTP response other
than the specified `204` success response.

The diagnostic may include the received HTTP response status. It must not
include the raw response body.

## Stable Backend Error Boundary

A valid unsuccessful stable API error is not a client runtime failure.

- `LOGIN_REJECTED` continues to display its safe backend message and
  auth-owned `loginFailureReferenceId`.
- Other valid stable API errors continue to display their stable code and safe
  message.
- The frontend does not create a client diagnostic for a valid stable API
  error.

The frontend branches by status and stable code, not safe message wording.

## Client Diagnostic Submission

For one classified client failure, the frontend attempts at most one:

```text
POST /api/client-diagnostics
```

Request body:

```json
{
  "category": "MALFORMED_RESPONSE",
  "operation": "PASSWORD_LOGIN",
  "clientApplication": "REACT",
  "responseStatus": 500
}
```

Framework application value:

- React implementation -> `REACT`
- Vue implementation -> `VUE`
- Angular implementation -> `ANGULAR`

The client accepts a diagnostic reference only when:

- the diagnostic response status is `201`
- the response body is an object
- `clientFailureReferenceId` is a non-blank string

The frontend does not need to validate the reference as a UUID. Opacity means
the client displays and transports the value without interpreting it.

## Diagnostic Failure Fallback

If diagnostic submission fails for any reason:

- no `clientFailureReferenceId` is displayed
- the user still sees `REQUEST_UNAVAILABLE`
- the original login failure category is not displayed
- the frontend does not retry or queue the diagnostic in this slice
- the diagnostic failure does not recursively create another diagnostic

This fallback applies when:

- the diagnostics endpoint is unreachable
- it returns a non-`201` response
- it returns a malformed response
- persistence fails server-side

## Scenarios

### Scenario: Login transport fails and diagnostic is persisted

**Given**
- The user submits the login form.
- The login request rejects before an HTTP response is received.
- The diagnostics endpoint remains reachable.

**When**
- The frontend handles the failure.

**Then**
- The frontend submits one `TRANSPORT_FAILURE` diagnostic.
- The diagnostic identifies `PASSWORD_LOGIN` and the current framework
  application.
- The diagnostic does not include a response status.
- The frontend displays `REQUEST_UNAVAILABLE`.
- The frontend displays the returned opaque `clientFailureReferenceId`.
- The frontend does not display the exception message or stack trace.

### Scenario: Login transport and diagnostics endpoint are both unavailable

**Given**
- The login request rejects before an HTTP response is received.
- The diagnostic submission also fails.

**When**
- The frontend handles both failures.

**Then**
- The frontend displays `REQUEST_UNAVAILABLE` without a client failure
  reference.
- The frontend does not retry or queue either request.
- The frontend does not expose either technical failure.

### Scenario: Unsuccessful response has malformed stable error body

**Given**
- The login request returns an unsuccessful HTTP response.
- The response does not satisfy the stable API error contract.

**When**
- The frontend handles the response.

**Then**
- The frontend submits one `MALFORMED_RESPONSE` diagnostic.
- The diagnostic may include the HTTP response status.
- The frontend does not submit the raw response body.
- The frontend displays `REQUEST_UNAVAILABLE`.
- The frontend displays a client failure reference only if diagnostic
  persistence succeeds.

### Scenario: Successful response has unexpected status

**Given**
- The login request returns a successful HTTP response other than `204`.

**When**
- The frontend handles the response.

**Then**
- The frontend submits one `UNEXPECTED_RESPONSE` diagnostic.
- The diagnostic may include the HTTP response status.
- The frontend displays `REQUEST_UNAVAILABLE`.
- The frontend displays a client failure reference only if diagnostic
  persistence succeeds.

### Scenario: Valid stable backend error remains a backend error

**Given**
- The login request returns an unsuccessful response conforming to the stable
  API error contract.

**When**
- The frontend handles the response.

**Then**
- The frontend displays the stable code and safe message.
- `loginFailureReferenceId` is displayed only for `LOGIN_REJECTED`.
- No client diagnostic is submitted.
- No `clientFailureReferenceId` is displayed.

### Scenario: Framework implements client failure observability

**Given**
- React, Vue, and Angular are the selected framework targets.
- This specification is authoritative for all three.

**When**
- A framework implementation delivers frontend-02.

**Then**
- It preserves the same classification, diagnostic, fallback, reference, and
  sensitive-data behavior.
- It remains independently installable, buildable, testable, and runnable.
- It does not import UI components or runtime code from another framework.
- React is delivered first.
- Missing Vue and Angular ports remain explicitly planned.

## Acceptance Criteria

- Client failures MUST be classified only as defined by this specification.
- A valid stable backend error MUST NOT create a client diagnostic.
- An unsuccessful malformed response MUST create at most one
  `MALFORMED_RESPONSE` submission attempt.
- A successful non-`204` response MUST create at most one
  `UNEXPECTED_RESPONSE` submission attempt.
- A rejected login fetch MUST create at most one `TRANSPORT_FAILURE`
  submission attempt.
- Diagnostic submission MUST use the reference-core-02 allowlisted contract.
- The framework application identifier MUST match the implementing framework.
- A `clientFailureReferenceId` MUST be displayed only after a valid `201`
  diagnostic response.
- Diagnostic failure MUST preserve `REQUEST_UNAVAILABLE` without a reference.
- Diagnostic failure MUST NOT recursively report itself.
- The frontend MUST NOT retry or persist an offline diagnostic queue.
- The frontend MUST NOT interpret the contents of either reference id.
- `loginFailureReferenceId` and `clientFailureReferenceId` MUST use distinct UI
  labels.
- React, Vue, and Angular MUST conform to the same authoritative behavior.
- Each framework application MUST remain independently implemented and tested.
- This slice MUST NOT require backend changes.

## Presentation Rules

All error results are normalized to `ErrorPresentation` before rendering.

For a valid stable login rejection:

- label: `Login failure reference`
- value: `loginFailureReferenceId`

For a client runtime failure whose diagnostic was persisted:

- stable result code: `REQUEST_UNAVAILABLE`
- label: `Client failure reference`
- value: `clientFailureReferenceId`

The frontend must not display the diagnostic category, response status, raw
technical cause, or diagnostic payload.

## Sensitive Data Boundary

Frontend diagnostic requests, frontend state, and user-visible results MUST NOT
contain:

- password or credential secret
- login identifier
- credential, registered-user, or account state
- request body or raw response body
- request or response headers
- cookie or session value
- user, personnel, role, organization, membership, or profile data
- raw exception message
- exception type or stack trace
- source file path or source code location
- browser storage contents
- IP address or device fingerprint
- `loginFailureReferenceId` inside a client diagnostic

The existing login form may hold the submitted password only for form
submission and must continue clearing it after submission as defined by
frontend-01.

## Invariants

- Stable backend errors and client runtime failures remain distinct.
- Auth owns login failure references.
- Reference-core owns client failure references and diagnostic persistence.
- The frontend only classifies, submits, and safely presents.
- A failed diagnostic submission produces no visible client reference.
- One original client failure causes at most one diagnostic submission attempt.
- Framework parity means equivalent behavior, not shared UI runtime code.

## Error Cases

- Login fetch rejects, diagnostic succeeds -> `REQUEST_UNAVAILABLE` with client
  failure reference.
- Login fetch rejects, diagnostic fails -> `REQUEST_UNAVAILABLE` without
  reference.
- Unsuccessful malformed response, diagnostic succeeds ->
  `REQUEST_UNAVAILABLE` with client failure reference.
- Successful non-`204` response, diagnostic succeeds ->
  `REQUEST_UNAVAILABLE` with client failure reference.
- Diagnostic returns non-`201` or malformed body -> `REQUEST_UNAVAILABLE`
  without reference.
- Valid stable API error -> stable backend error display without client
  diagnostic.

## Local and Production Boundary

The same HTTP behavior applies in local and production deployments.

- Local Vite continues proxying `/api` to the backend.
- `REQUEST_UNAVAILABLE` can be rendered only after the login shell has loaded.
- Local browser or Vite proxy console output is not a diagnostic contract.
- Production hosting, CORS, rate limiting, retention, and external telemetry
  remain outside this frontend slice.
- If the backend and diagnostics endpoint are unavailable together, no client
  failure reference can be produced.

## Unknown / To Be Discovered

- production frontend deployment model
- diagnostic retention and cleanup
- ingestion rate limiting and abuse protection
- correlation or request id propagation
- offline reporting requirements
- Vue and Angular framework-specific tooling

## Non-Goals

- Backend behavior or schema changes.
- Initial navigation or refresh while the frontend hosting origin is
  unavailable.
- CDN, reverse-proxy, hosting-platform, service-worker, or PWA fallback pages.
- Diagnostic lookup UI.
- `IT_ADMIN` frontend console.
- Diagnostic listing, search, dashboard, or export.
- Correlation or request id propagation.
- Retry, offline queue, service worker, or browser persistence.
- Console logging as production observability.
- External telemetry or analytics SDK.
- Current-user endpoint.
- Protected route or organization command console.
- Auth credential, session, or login audit changes.
- Removing, renaming, or generalizing `loginFailureReferenceId`.
- Vue or Angular production implementation in the React delivery.
- Cross-framework UI components or runtime package.
