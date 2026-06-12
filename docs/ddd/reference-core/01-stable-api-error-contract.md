# DDD Derivation - 01 Stable API Error Contract

This note is **derived from**
`docs/specs/reference-core/01-stable-api-error-contract.md`.
It does not introduce new behavior.

---

## Scope Ownership

**reference-core cross-cutting support scope**

`reference-core` is not a domain bounded context. It owns a shared HTTP delivery
contract that protects and exposes existing bounded-context workflows.

Rationale:

- The error response shape is consumed across auth, organization, framework
  errors, and future frontend delivery.
- Assigning the contract to auth would incorrectly make auth responsible for
  organization and generic HTTP failures.
- Assigning the contract to organization would incorrectly make organization
  responsible for auth and framework failures.
- No reference-core domain aggregate or business rule is introduced.

Auth and organization remain authoritative for their own failure behavior.
Reference-core only owns safe client-visible normalization at the HTTP boundary.

## No Aggregate Root

This slice introduces no aggregate root and no domain model.

The stable API error response is an HTTP delivery contract, not persisted
business state. Error codes do not replace auth or organization domain errors
and must not become a shared domain taxonomy.

## Minimum Model Additions

### Web Contract Model

- API error response
  - required integer `status`
  - required stable coarse `code`
  - required safe `message`
  - optional auth-owned `loginFailureReferenceId` for login rejection only

### Error Category Mapping

- validation and malformed request input -> `400 VALIDATION_ERROR`
- existing generic domain/application rejection -> `400 APPLICATION_REJECTED`
- unauthenticated protected request -> `401 UNAUTHENTICATED`
- rejected password login -> `401 LOGIN_REJECTED`
- existing permission mismatch -> `403 FORBIDDEN`
- existing not found behavior and unmapped API route -> `404 NOT_FOUND`
- explicitly specified conflict only -> `409 CONFLICT`
- unsupported method -> `405 METHOD_NOT_ALLOWED`
- unexpected failure -> `500 INTERNAL_ERROR`

The mapping must preserve existing endpoint status semantics.

## Rule Placement

### Auth rules

- Auth continues to decide whether login or protected session context succeeds.
- Auth-10 continues to own login failure audit behavior and
  `loginFailureReferenceId`.
- Reference-core may serialize the auth-owned opaque reference only for
  `LOGIN_REJECTED`.
- Reference-core must not inspect or expose login failure reason, credential
  state, registered-user state, account state, or session state.

### Organization rules

- Organization continues to decide domain and application rejection behavior.
- Reference-core must not re-evaluate organization rules.
- Existing organization HTTP status semantics remain unchanged.
- Organization exception messages are not automatically safe client messages
  and must not be copied directly into the stable response.

### Web adapter rules

- Normalize covered API failures into the stable response shape.
- Select status and coarse code from explicit safe mappings.
- Supply a safe message that does not reveal internal details.
- Normalize framework-level malformed body, not-found API route, unsupported
  method, and unexpected exception behavior.
- Preserve `loginFailureReferenceId` only for login rejection.
- Do not add undefined `path`, `timestamp`, correlation id, or generic reference
  id fields.

### Infrastructure rules

- No persistence or schema change is derived.
- No logging or diagnostic backend is derived.
- Unexpected exception logging is not required by this slice.

## HTTP Exception Boundary

The implementation should use the Spring MVC exception-resolution boundary,
such as global controller advice and framework exception handler extension
points.

This boundary is responsible for exceptions raised:

- before controller method invocation, including request body parsing
- during controller and application invocation
- while resolving supported HTTP methods and API routes
- by unexpected failures reaching the web boundary

Context-specific exception categories may be mapped at this shared boundary,
but the boundary must not interpret exception messages as business rules or
return those messages directly.

### Existing compatibility debt

Unknown-group failures currently arrive at the web boundary as
`IllegalArgumentException` and are distinguished from other illegal arguments
by the existing `"Group not found"` message prefix. The implemented global
handler preserves that classifier only to retain the existing `404` endpoint
semantics without changing organization application behavior in this slice.

The exception message is never returned to the client. Replacing this legacy
classifier with a typed organization application failure requires separate
specification and is not derived from `reference-core-01`.

## Why General AOP Is Not The HTTP Error Boundary

A general method-execution aspect is not selected as the primary implementation
boundary for this slice.

Rationale:

- malformed request bodies and unsupported methods may fail before a controller
  method executes
- the stable contract is an HTTP adapter concern, not application or domain
  invocation behavior
- intercepting use cases or domain methods would couple HTTP response semantics
  to non-HTTP layers
- broad exception interception encourages unstable classification by exception
  message

Spring MVC global exception handling is still a cross-cutting mechanism, but it
operates at the HTTP lifecycle extension point that owns response status,
headers, and body mapping.

General AOP may be evaluated by a future logging, timing, diagnostics, or
correlation slice after those workflows are specified.

## Safe Message Policy

The stable response must not directly serialize:

- `Throwable#getMessage()`
- exception class names
- stack traces
- parser or binding implementation details
- persistence or database details
- sensitive auth, user, role, organization, profile, or session state

Messages are client-visible safe descriptions. Error codes, not exact message
wording, are the machine-readable contract.

## Compatibility Policy

This slice intentionally preserves:

- existing `400` validation and generic rejection behavior
- existing `401` unauthenticated and login rejection behavior
- existing `403` organization permission mismatch behavior
- existing `404` unknown group and invitation behavior
- existing `405` unsupported method behavior
- auth-10 `loginFailureReferenceId`

Introducing `409 Conflict` for an existing rejection requires an authoritative
endpoint behavior change and is not derived from this slice.

## Test Layer Placement

This slice is primarily verified through web integration tests.

Required contract coverage:

- validation error shape and safe message
- malformed request body shape
- unauthenticated shape remains indistinguishable across session failures
- existing unauthorized, not-found, and application rejection mappings
- login rejection includes opaque `loginFailureReferenceId`
- login rejection does not expose failure reason or sensitive state
- unsupported HTTP method shape
- unmapped API route shape
- unexpected exception returns safe `500 INTERNAL_ERROR`
- every response body status equals its HTTP status
- no internal exception message or stack trace is returned

Auth domain/application tests and organization domain/application tests remain
unchanged unless a defect is discovered. This slice must not require new domain
tests because it introduces no domain behavior.

## Implemented Boundary

The implementation uses:

- shared `referencecore/web` error response representation
- shared global HTTP exception mapping
- replacement of the previous auth and organization web exception response
  construction
- web integration tests that lock the stable response contract

`referencecore/web` is a Java package-safe name for the `reference-core`
support scope. It is not a domain bounded-context module and must not gain
domain or application layers.

The implementation must not change auth or organization use-case decisions.

## Unknown / To Be Discovered

- localization
- complete public error taxonomy
- correlation or request id
- generic troubleshooting reference
- response timestamp and path fields
- structured logging and logging backend
- operator diagnostic workflow
- frontend presentation behavior

## Non-Goals

- A reference-core domain model or aggregate.
- Shared domain exceptions across bounded contexts.
- General AOP logging or diagnostics.
- New auth or organization business rules.
- Persistence or schema changes.
- Full error taxonomy or field-level validation detail.
