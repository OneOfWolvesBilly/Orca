# Spec 01 - Stable API Error Contract

## Goal

Provide one stable and safe error response contract for Orca HTTP APIs so API
clients and a future frontend reference shell can handle failures without
parsing exception messages, depending on framework-default response shapes, or
guessing why a request failed.

This slice standardizes the client-visible HTTP error boundary for existing
auth and organization HTTP workflows. It does not change organization domain or
application behavior, auth credential verification, session creation,
protected session context, or auth-10 login failure audit behavior.

## Workflow Traceability

- Workflows:
  - Error and Exception Handling
  - Frontend Reference Shell
  - existing HTTP workflows
- Workflow gap:
  - existing specs define endpoint-level failure outcomes
  - no authoritative cross-endpoint API error response shape exists
  - clients may currently depend on framework-default responses or exception
    messages
- Primary actor:
  - API client
  - future frontend user through the API client
- Supporting actor:
  - web adapter
- Safe diagnostics actor:
  - developer or operator is limited to future diagnostics behavior that is
    not defined by this slice
- Predecessor slices:
  - `organization-08`
  - `auth-08`
  - `auth-09`
  - `auth-10`

## Reference-Core Scope

`reference-core` is a cross-cutting support scope, not a domain bounded context.

It owns the stable client-visible API error contract and HTTP error
normalization behavior. It does not own or redefine auth or organization
failure rules. Auth and organization specs remain authoritative for deciding
whether an operation succeeds or fails.

## Contract Terms

- API Error Response
  The single client-visible JSON object returned for an unsuccessful Orca API
  request covered by this slice.

- Error Code
  A stable, coarse-grained machine-readable category that allows a client to
  handle the failure without parsing the message.

- Safe Message
  A client-visible description that does not expose internal exception
  messages, stack traces, sensitive state, or hidden failure reasons. Clients
  must use the error code, not message text, for branching behavior.

- Login Failure Reference Id
  The opaque auth-10 troubleshooting reference returned only for rejected
  password login attempts. It remains auth-owned and carries no encoded failure
  reason or sensitive state.

## HTTP Error Contract

Every covered unsuccessful API response MUST use this JSON shape:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed"
}
```

Required fields:

- `status`
  - integer HTTP status code
  - MUST equal the response HTTP status
- `code`
  - non-blank stable coarse error code defined by this slice
  - clients MAY use this field for branching behavior
- `message`
  - non-blank safe client-visible message
  - clients MUST NOT depend on exact message wording for branching behavior

Optional fields:

- `loginFailureReferenceId`
  - MUST appear for rejected password login attempts as required by auth-10
  - MUST NOT appear for other error categories

No additional client-visible field is defined by this slice.

Covered API responses are the failure categories defined by this slice for
requests under `/api`. Additional framework-level HTTP failure categories are
unknown / to be discovered. Non-API routes are not defined by this slice.

## Stable Error Categories

This slice defines only the minimum categories needed by existing HTTP
workflows and their framework-level failure boundary.

| HTTP status | Error code | Applies to |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | malformed request body, missing required request body or field, invalid transport input |
| `400` | `APPLICATION_REJECTED` | existing domain/application rejection currently mapped to bad request |
| `401` | `UNAUTHENTICATED` | protected request without establishable current user context |
| `401` | `LOGIN_REJECTED` | rejected password login |
| `403` | `FORBIDDEN` | authenticated actor is not allowed to perform the existing operation |
| `404` | `NOT_FOUND` | requested API resource or API route is not found |
| `409` | `CONFLICT` | only a failure explicitly specified by an authoritative endpoint spec as conflict |
| `405` | `METHOD_NOT_ALLOWED` | API route exists but does not support the submitted HTTP method |
| `500` | `INTERNAL_ERROR` | unexpected server failure |

This table is not a complete public error taxonomy. A future slice may add a
new stable code only when an authoritative workflow requires it.

## Existing Status Semantics

This slice preserves existing specified and tested endpoint status semantics.

- Existing validation failures remain `400`.
- Existing generic organization domain/application rejections remain `400`.
- Existing unauthenticated protected command failures remain `401`.
- Existing rejected password login attempts remain `401`.
- Existing organization permission mismatch failures remain `403`.
- Existing unknown group or invitation failures remain `404`.
- Existing unsupported HTTP method failures remain `405`.

No existing organization rejection is reclassified as `409 Conflict` by this
slice. `CONFLICT` is reserved for a future or existing endpoint behavior only
after an authoritative spec explicitly requires conflict semantics.

## Scenarios

### Scenario: API client receives a stable validation error

**Given**
- An API request has a malformed body, a missing required body, a missing
  required field, or invalid transport input.

**When**
- The web boundary rejects the request as validation failure.

**Then**
- The response uses the stable API error response shape.
- The response status is `400`.
- The response code is `VALIDATION_ERROR`.
- The message is safe and does not expose parser, binding, or exception details.

### Scenario: Protected request is rejected as unauthenticated

**Given**
- A protected HTTP command request has no establishable current user context.

**When**
- The auth HTTP boundary rejects the request.

**Then**
- The response status is `401`.
- The response code is `UNAUTHENTICATED`.
- Missing, blank, malformed, unknown, expired, invalid, and revoked session
  conditions remain indistinguishable.
- The response does not expose session state or a session value.

### Scenario: Existing permission failure uses a stable unauthorized response

**Given**
- An authenticated actor is not allowed to perform an existing organization
  operation.

**When**
- The existing organization behavior rejects the operation.

**Then**
- The response status remains `403`.
- The response code is `FORBIDDEN`.
- The response does not expose role assignments, membership details, or
  internal authorization reasoning.

### Scenario: Existing resource is not found

**Given**
- An API request references an unknown group, an unknown invitation, or an
  unmapped API route.

**When**
- The web boundary returns not found.

**Then**
- The response status is `404`.
- The response code is `NOT_FOUND`.
- The response uses a safe message and does not expose internal lookup details.

### Scenario: Existing domain or application behavior rejects a request

**Given**
- An existing domain or application rule rejects an operation.
- No authoritative endpoint spec classifies that failure as unauthenticated,
  unauthorized, not found, or conflict.

**When**
- The web boundary maps the rejection.

**Then**
- The response preserves the existing `400` status semantics.
- The response code is `APPLICATION_REJECTED`.
- The response does not directly expose the thrown exception message.
- The web boundary does not re-evaluate or change the underlying rule.

### Scenario: Rejected login preserves opaque troubleshooting reference

**Given**
- Auth rejects a password login attempt according to auth-08 and auth-10.

**When**
- The login failure is returned to the client.

**Then**
- The response status remains `401`.
- The response code is `LOGIN_REJECTED`.
- The response includes the auth-10 opaque `loginFailureReferenceId`.
- All login rejection conditions remain indistinguishable.
- The response does not expose login failure reason, credential state,
  registered-user state, account state, session state, or audit details.

### Scenario: Unsupported method uses the stable contract

**Given**
- An API route exists.
- The submitted HTTP method is not supported by that route.

**When**
- The web boundary rejects the method.

**Then**
- The response status is `405`.
- The response code is `METHOD_NOT_ALLOWED`.
- The response uses the stable API error response shape.

### Scenario: Unexpected server failure is safely normalized

**Given**
- An unexpected exception reaches the API error boundary.

**When**
- The error response is returned to the client.

**Then**
- The response status is `500`.
- The response code is `INTERNAL_ERROR`.
- The response uses a safe message.
- The response does not expose the internal exception message, exception type,
  stack trace, database details, or sensitive state.

## Acceptance Criteria

- Covered unsuccessful Orca API responses MUST use one stable JSON shape.
- `status`, `code`, and `message` MUST always be present.
- The body `status` MUST equal the HTTP response status.
- Clients MUST be able to distinguish the minimum categories defined by this
  slice without parsing message text.
- Safe client messages MUST NOT directly use internal exception messages.
- Framework-level malformed body, not-found API route, unsupported method, and
  unexpected server failures MUST use the stable error contract.
- Existing endpoint status semantics MUST remain unchanged unless an
  authoritative endpoint spec explicitly changes them.
- `loginFailureReferenceId` MUST remain present for rejected password login
  attempts.
- `loginFailureReferenceId` MUST remain opaque and MUST NOT be renamed,
  generalized, or returned for non-login errors.
- This slice MUST NOT change organization domain or application behavior.
- This slice MUST NOT change auth credential verification, session creation,
  protected session context, or login failure audit behavior.

## Sensitive Data Boundary

Client-visible API error responses MUST NOT expose:

- submitted password or credential secret
- raw session cookie value or session id
- internal exception message, exception type, or stack trace
- database, persistence, or infrastructure details
- login failure reason
- credential, registered-user, account, or session state
- authenticated user id, employee id, personnel id, name, email, department,
  supervisor status, or profile data
- auth system role, organization role, membership details, or permission
  evaluation details
- organization aggregate internals
- auth-10 audit details other than the opaque `loginFailureReferenceId`

## Error Cases

- Malformed request body -> `400 VALIDATION_ERROR`.
- Missing required request body or field -> `400 VALIDATION_ERROR`.
- Existing generic domain/application rejection -> `400 APPLICATION_REJECTED`.
- Protected request without establishable current user context ->
  `401 UNAUTHENTICATED`.
- Rejected password login -> `401 LOGIN_REJECTED` with opaque
  `loginFailureReferenceId`.
- Existing permission mismatch -> `403 FORBIDDEN`.
- Unknown group, invitation, or API route -> `404 NOT_FOUND`.
- Explicitly specified conflict -> `409 CONFLICT`.
- Unsupported HTTP method for an existing API route ->
  `405 METHOD_NOT_ALLOWED`.
- Unexpected server failure -> `500 INTERNAL_ERROR`.

## Invariants

- Auth and organization specs remain authoritative for underlying behavior.
- The API error boundary translates failures without creating business rules.
- Error codes are coarse client-handling categories, not domain error identity.
- Internal exception messages are not a public contract.
- Login rejection remains indistinguishable across all auth-10 failure
  conditions.
- Unexpected failures are safe by default.

## Unknown / To Be Discovered

- localization and localized message selection
- complete public error code taxonomy
- correlation or request id propagation
- generic troubleshooting or error reference id
- timestamp in the public error response
- request path in the public error response
- logging backend and structured logging behavior
- operator diagnostic workflow
- frontend error display wording and presentation
- which future workflow failures require `409 Conflict`
- additional framework-level HTTP failure categories

## Non-Goals

- Changing organization domain, application, persistence, or command behavior.
- Changing auth credential verification, session creation, protected session
  context, or auth-10 login failure audit behavior.
- Removing or generalizing `loginFailureReferenceId`.
- Defining a complete public error taxonomy.
- Reclassifying existing `400` rejections as `409 Conflict`.
- Localization.
- Correlation or request id propagation.
- Structured logging, logging backend, audit lookup, or operator diagnostics.
- OpenAPI generation or API versioning.
- Frontend implementation.
- Returning field-level validation details.
- Exposing internal exception details for troubleshooting.
