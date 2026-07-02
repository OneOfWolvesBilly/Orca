# DDD Derivation - 02 Client Diagnostics Foundation

Status: Approved / implementation pending.

This note is **derived from**
`docs/specs/reference-core/02-client-diagnostics-foundation.md`.
It does not introduce new behavior.

## Scope Ownership

**reference-core cross-cutting support scope**

Rationale:

- Client diagnostic records support frontend and operational workflows without
  owning frontend presentation or auth behavior.
- Auth owns authenticated current user context and the `IT_ADMIN` system role.
- Frontend delivery owns framework runtime failure observation and reporting
  decisions.
- Organization does not own client runtime diagnostics.

`reference-core` remains a support scope, not a domain bounded context.

## Consistency Boundary

**ClientDiagnosticRecord**

This is persisted support state rather than a domain aggregate belonging to a
business bounded context. It is modeled as one consistency boundary because:

- one server-generated opaque reference identifies one record
- the record and reference must be persisted atomically
- the reference must not be returned before persistence succeeds
- the stored fields are restricted to one allowlisted diagnostic model

The model must not grow into a shared business-domain error hierarchy.

## Minimum Model

### Support model

- `ClientFailureReferenceId`
  - UUID-backed opaque value
  - contains no encoded diagnostic or sensitive meaning

- `ClientDiagnosticRecord`
  - client failure reference id
  - server-assigned occurrence timestamp
  - diagnostic category
  - client operation
  - client application
  - optional HTTP response status

- `ClientDiagnosticCategory`
  - `TRANSPORT_FAILURE`
  - `MALFORMED_RESPONSE`
  - `UNEXPECTED_RESPONSE`

- `ClientOperation`
  - `PASSWORD_LOGIN`

- `ClientApplication`
  - `REACT`
  - `VUE`
  - `ANGULAR`

These are support contract values, not auth or organization domain concepts.

### Application ports

- `ClientDiagnosticRecordRepository`
  - persists one diagnostic record
  - loads one record by opaque reference

- `ClientFailureReferenceIdGenerator`
  - creates opaque UUID references

- time source
  - assigns the server occurrence timestamp

- auth role query
  - reuses the auth-owned `AuthSystemRoleDirectory`
  - checks `IT_ADMIN` for protected lookup

### Application behavior

- record client diagnostic
  - validates the allowlisted command
  - generates reference and server timestamp
  - persists the complete record
  - returns the reference only after successful persistence

- look up client diagnostic
  - requires current user context
  - checks the actor has `IT_ADMIN`
  - loads by opaque reference
  - exposes only allowlisted stored fields

## Rule Placement

### Reference-core support rules

- Diagnostic requests contain only allowlisted fields.
- References are opaque UUID values.
- Occurrence timestamps are server-assigned.
- A reference is returned only after successful persistence.
- Malformed and unknown lookup references are exposed as the same not-found
  result.

### Auth rules

- Auth establishes current user context for protected lookup.
- Auth owns `IT_ADMIN`.
- Reference-core asks whether the authenticated actor has `IT_ADMIN`; it does
  not create or assign roles.
- Ingestion remains public because the login shell may not have an authenticated
  session.

### Frontend rules

- A later frontend slice decides which runtime failures are reportable.
- Each framework implements its own reporting adapter.
- Framework implementations share the HTTP behavior contract, not runtime or UI
  components.
- If diagnostic ingestion fails, the frontend must not present a
  `clientFailureReferenceId`.

### Web adapter rules

- Strictly deserialize the ingestion allowlist and reject unknown properties.
- Do not accept arbitrary maps, messages, stack traces, bodies, headers, or
  metadata bags.
- Map unsuccessful behavior through the stable API error contract.
- Protect exact-reference lookup with current user context and `IT_ADMIN`.
- Do not expose persistence exceptions or internal details.

### Infrastructure rules

- Persist client diagnostics in reference-core-owned storage.
- Flyway owns the schema.
- Generate references using UUIDs without encoded meaning.
- Infrastructure must not add diagnostic fields not defined by the spec.
- Retention cleanup is not implemented until a policy is specified.

## HTTP Boundary

### Ingestion

```text
POST /api/client-diagnostics
```

Recommended request DTO:

- category
- operation
- clientApplication
- optional responseStatus

Recommended response DTO:

- clientFailureReferenceId

The controller delegates validation and persistence behavior. It contains no
diagnostic policy beyond transport mapping.

### Lookup

```text
POST /api/client-diagnostics/lookup
```

The request body contains only `clientFailureReferenceId`. POST is used because
the lookup depends on authenticated current user context, as required by the
repository API constraints.

The lookup adapter consumes the established `CurrentUserContext`, checks
`IT_ADMIN` through the application boundary, and returns the safe diagnostic
view.

## Persistence Boundary

Recommended table ownership:

```text
reference_core_client_diagnostics
```

Minimum columns:

- client_failure_reference_id
- occurred_at
- category
- operation
- client_application
- response_status nullable

The table must not include generic payload, metadata JSON, exception, stack
trace, request body, response body, user, credential, cookie, session, role,
organization, profile, or login failure reference columns.

## Authorization Boundary

No new role is derived.

- `IT_ADMIN` may look up a record by exact reference.
- Authenticated actors without `IT_ADMIN` are forbidden.
- Unauthenticated lookup is rejected.
- Public ingestion does not grant lookup access and does not establish a user
  identity.

The existing `AuthSystemRoleDirectory` supports multiple role assignments per
user and currently defines `IT_ADMIN`. This slice consumes that boundary without
changing its model.

## Sensitive Data Design

Use a positive allowlist instead of redaction.

Rationale:

- Redaction requires first accepting potentially sensitive material.
- Arbitrary metadata bags make future leakage difficult to review.
- Coarse enums and bounded numeric status provide deterministic storage and test
  coverage.

The implementation should not log rejected request bodies or thrown validation
messages as part of this slice.

## Failure Policy

- Invalid diagnostic input produces a stable validation error and no record.
- Persistence failure produces a safe internal error and no returned reference.
- Invalid or unknown lookup reference produces the same not-found result.
- Unauthorized lookup returns no diagnostic fields.
- Backend-unreachable client failure cannot be guaranteed a reference by this
  server-side slice.

## Test Layer Placement

Support model tests:

- opaque UUID reference validation
- diagnostic record requires server reference and timestamp
- optional response status range
- only defined enum values are accepted through the application command

Application tests:

- valid ingestion persists one record and returns its reference
- reference is not returned when persistence fails
- server assigns timestamp rather than accepting client time
- `IT_ADMIN` can look up a known record
- non-`IT_ADMIN` lookup is forbidden
- unknown reference is not found

Infrastructure tests:

- diagnostic persistence round trip
- nullable response status round trip
- generated reference remains opaque
- schema contains no generic diagnostic payload column

Web integration tests:

- unauthenticated client can submit valid diagnostic
- response returns `201` and one opaque reference
- unknown fields and invalid enums are rejected
- forbidden values are not reflected in responses
- lookup without session is unauthenticated
- lookup by non-`IT_ADMIN` is forbidden
- lookup by `IT_ADMIN` returns allowlisted fields
- unknown and malformed reference return the same safe not-found category
- persistence failure returns no reference

Frontend tests are not part of this slice.

## Future Frontend Seam

`frontend-02` may consume this foundation by:

- mapping framework runtime failures to the three coarse categories
- submitting only the defined allowlisted diagnostic fields
- displaying `clientFailureReferenceId` only after successful ingestion
- preserving `REQUEST_UNAVAILABLE` without a reference when ingestion is
  unavailable

React is expected to remain the first reference implementation. Vue and Angular
remain planned ports and must implement equivalent behavior inside their own
applications.

## Unknown / To Be Discovered

- retention duration and cleanup mechanism
- production database sizing
- ingestion abuse protection and rate limiting
- correlation/request id propagation
- external telemetry integration
- listing, filtering, dashboards, and export

## Non-Goals

- Reference-core business domain.
- Frontend runtime implementation.
- Shared framework runtime or UI package.
- New auth role or role lifecycle.
- Login failure audit changes.
- General logging, tracing, metrics, or alerting platform.
- Offline client diagnostic queue.
- Guaranteed reporting while the backend is unreachable.
- Diagnostic search, listing, export, or dashboard.
- Retention cleanup.
- Auth or organization behavior changes.
