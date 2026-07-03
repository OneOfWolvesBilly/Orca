# Spec 02 - Client Diagnostics Foundation

Status: Approved / Implemented.

## Goal

Provide a safe, server-side diagnostics destination for client application
failures so an authorized IT admin can look up a persisted diagnostic record by
an opaque client failure reference id.

This slice establishes client diagnostic ingestion, persistence, and protected
lookup. It does not decide which React, Vue, or Angular runtime failures should
be reported. That client-side classification and submission behavior belongs to
a later frontend delivery slice.

## Workflow Traceability

- Workflows:
  - Logging, Observability, and Operations
  - Error and Exception Handling
  - Frontend Reference Shell
- Workflow gap:
  - frontend-01 safely collapses transport and unexpected client failures into
    `REQUEST_UNAVAILABLE`
  - no queryable server-side destination exists for safe client diagnostics
  - a client failure reference would have no support value without persisted
    diagnostic state and an authorized lookup path
- Primary actor:
  - `IT_ADMIN`
- Supporting actors:
  - client application
  - reference-core HTTP boundary
- Predecessor slices:
  - `auth-06` admin-managed user provisioning and the `IT_ADMIN` role boundary
  - `auth-09` protected HTTP session context
  - `reference-core-01` stable API error contract
  - `frontend-01` login result shell

## Reference-Core Scope

`reference-core` is a cross-cutting support scope, not a domain bounded context.

This slice owns the safe client diagnostic record contract and the HTTP
boundaries for accepting and looking up those records. It does not own auth
roles, login behavior, frontend failure classification, organization behavior,
or a general-purpose telemetry platform.

Auth remains authoritative for:

- authenticated session context
- registered user identity
- the `IT_ADMIN` system role
- `loginFailureReferenceId`

Frontend delivery remains responsible for deciding when a framework application
has observed a reportable client failure.

## Contract Terms

- Client Diagnostic Record
  Server-side support state containing a small allowlisted description of one
  client-observed failure.

- Client Failure Reference Id
  An opaque server-issued UUID used to locate one client diagnostic record. It
  carries no encoded failure, user, credential, session, role, organization, or
  profile information.

- Diagnostic Category
  A coarse allowlisted client failure category. The minimum categories are:
  - `TRANSPORT_FAILURE`
  - `MALFORMED_RESPONSE`
  - `UNEXPECTED_RESPONSE`

- Client Operation
  A stable allowlisted identifier for the client operation that failed. This
  slice initially accepts:
  - `PASSWORD_LOGIN`

- Client Application
  A stable allowlisted identifier for the independently deployed framework
  application:
  - `REACT`
  - `VUE`
  - `ANGULAR`

## Client Diagnostic Ingestion Contract

The server accepts:

```text
POST /api/client-diagnostics
```

The request body contains only:

```json
{
  "category": "MALFORMED_RESPONSE",
  "operation": "PASSWORD_LOGIN",
  "clientApplication": "REACT",
  "responseStatus": 500
}
```

Required fields:

- `category`
- `operation`
- `clientApplication`

Optional fields:

- `responseStatus`
  - may be supplied only when an HTTP response was received
  - must be an integer from `100` through `599`
  - must not be required for `TRANSPORT_FAILURE`

The server, not the client, assigns:

- `clientFailureReferenceId`
- `occurredAt`

After the diagnostic record is persisted successfully, the response is:

- HTTP status `201`
- response body:

```json
{
  "clientFailureReferenceId": "opaque-uuid"
}
```

The endpoint does not require an authenticated session because the existing
login shell must be able to submit a safe diagnostic before login succeeds.
The endpoint accepts only the allowlisted fields defined above and must reject
additional client-supplied diagnostic fields.

If persistence fails, the endpoint must not return a
`clientFailureReferenceId`. The failure uses the stable API error contract from
`reference-core-01`.

## Protected Diagnostic Lookup Contract

An authenticated `IT_ADMIN` may request:

```text
POST /api/client-diagnostics/lookup
```

Request body:

```json
{
  "clientFailureReferenceId": "opaque-uuid"
}
```

POST is required because this operation depends on authenticated current user
context. The response contains only the persisted allowlisted diagnostic
fields:

```json
{
  "clientFailureReferenceId": "opaque-uuid",
  "occurredAt": "server-assigned timestamp",
  "category": "MALFORMED_RESPONSE",
  "operation": "PASSWORD_LOGIN",
  "clientApplication": "REACT",
  "responseStatus": 500
}
```

Lookup rules:

- no authenticated session -> `401 UNAUTHENTICATED`
- authenticated actor without `IT_ADMIN` -> `403 FORBIDDEN`
- unknown or malformed reference -> `404 NOT_FOUND`
- known reference and `IT_ADMIN` -> return the diagnostic record

The lookup response must not expose client-supplied raw technical details or
any auth, user, session, role, organization, or profile state.

## Scenarios

### Scenario: Client stores a safe diagnostic record

**Given**
- A client application has classified a reportable client failure.
- The diagnostic request contains only allowlisted fields and valid values.

**When**
- The client submits the diagnostic request.

**Then**
- Reference-core assigns one opaque client failure reference id.
- Reference-core assigns the occurrence timestamp from server time.
- One client diagnostic record is persisted.
- The response returns the reference only after persistence succeeds.
- The response does not echo diagnostic details.

### Scenario: Unauthenticated login shell stores a safe diagnostic

**Given**
- The React, Vue, or Angular login shell has no authenticated session.
- The client has an allowlisted diagnostic request.

**When**
- The client submits the diagnostic request.

**Then**
- Absence of an authenticated session does not by itself reject ingestion.
- The same strict allowlist and validation rules apply.
- No user identity or login state is inferred or stored.

### Scenario: Client attempts to submit forbidden diagnostic details

**Given**
- A diagnostic request includes an undefined field such as raw response body,
  exception message, stack trace, password, cookie, session, user, role,
  organization, or profile data.

**When**
- The server validates the request.

**Then**
- The request is rejected with `400 VALIDATION_ERROR`.
- No diagnostic record is persisted.
- No client failure reference id is returned.
- The forbidden value is not copied into a diagnostic response.

### Scenario: IT admin looks up a client diagnostic

**Given**
- A client diagnostic record exists.
- The request has an authenticated current user context.
- The actor has the auth-owned `IT_ADMIN` role.

**When**
- The actor looks up the record by client failure reference id.

**Then**
- The persisted allowlisted diagnostic record is returned.
- The lookup does not expose raw technical details or sensitive state.

### Scenario: Non-admin cannot look up client diagnostics

**Given**
- A client diagnostic record exists.
- The request has an authenticated current user context.
- The actor does not have the auth-owned `IT_ADMIN` role.

**When**
- The actor attempts to look up the record.

**Then**
- The request is rejected with `403 FORBIDDEN`.
- No diagnostic fields are returned.

### Scenario: Diagnostic persistence is unavailable

**Given**
- A client submits a valid diagnostic request.
- The server cannot persist the diagnostic record.

**When**
- The ingestion request fails.

**Then**
- No client failure reference id is returned.
- The error uses the stable API error contract.
- The server does not claim the diagnostic is queryable.

## Acceptance Criteria

- Reference-core MUST provide a server-side destination for safe client
  diagnostic records.
- A diagnostic record MUST receive a server-generated opaque UUID reference.
- A diagnostic record MUST receive a server-generated occurrence timestamp.
- The reference MUST be returned only after the record is persisted.
- Ingestion MUST accept only the fields defined by this specification.
- Ingestion MUST reject unknown fields.
- Ingestion MUST NOT require an authenticated session.
- Ingestion MUST NOT infer or persist user identity from session or request
  context.
- Lookup MUST require an authenticated current user context.
- Lookup MUST allow only an actor with the auth-owned `IT_ADMIN` role.
- Lookup MUST use the existing auth role boundary and MUST NOT introduce
  `MAIN_ADMIN`, generic `ADMIN`, or another system role.
- Unknown or malformed references MUST return the same safe `404 NOT_FOUND`
  category.
- Persistence failure MUST NOT return a client failure reference.
- All unsuccessful API responses MUST follow `reference-core-01`.
- The diagnostic model MUST remain framework-neutral.
- React, Vue, and Angular MUST remain independently implemented clients.
- This slice MUST NOT add frontend runtime reporting behavior.
- This slice MUST NOT change auth or organization business behavior.

## Sensitive Data Boundary

Client diagnostic requests and persisted diagnostic records MUST NOT contain:

- password or credential secret
- login identifier
- credential or registered-user state
- account state
- raw request or response body
- request headers
- cookie or session value
- authenticated user id
- employee or personnel id
- name, email, department, supervisor status, or profile data
- auth system role
- organization role, membership, or aggregate details
- raw exception message
- exception type or stack trace
- source file path or source code location
- browser storage contents
- IP address or device fingerprint
- `loginFailureReferenceId`

`loginFailureReferenceId` and `clientFailureReferenceId` remain separate:

- `loginFailureReferenceId` is auth-owned and identifies a rejected login audit
  record.
- `clientFailureReferenceId` is reference-core-owned and identifies a client
  diagnostic record.
- Neither identifier may replace, contain, or derive from the other.

## Invariants

- A client diagnostic record has exactly one opaque client failure reference id.
- A client failure reference id identifies at most one diagnostic record.
- Client occurrence time is server-assigned.
- Only allowlisted diagnostic values are persisted.
- A reference is client-visible only after successful persistence.
- Diagnostic lookup is limited to authenticated `IT_ADMIN` actors.
- Client diagnostics do not establish authentication, authorization, or
  business state.

## Error Cases

- Missing required diagnostic field -> `400 VALIDATION_ERROR`.
- Unknown diagnostic field -> `400 VALIDATION_ERROR`.
- Unsupported category, operation, or client application ->
  `400 VALIDATION_ERROR`.
- Invalid response status -> `400 VALIDATION_ERROR`.
- Diagnostic persistence failure -> safe `500 INTERNAL_ERROR` without
  reference.
- Lookup without authenticated session -> `401 UNAUTHENTICATED`.
- Lookup by authenticated non-`IT_ADMIN` -> `403 FORBIDDEN`.
- Lookup with malformed or unknown reference -> `404 NOT_FOUND`.

## Retention and Operational Boundary

The diagnostic record destination for this slice is the backend database.

Retention duration, automated deletion, export, alerting, aggregation, and
external telemetry integration remain unknown / to be discovered. They must
not be inferred from the presence of persisted diagnostic records.

This slice provides lookup by exact opaque reference only. It does not provide
listing, searching, filtering, dashboards, or bulk export.

## Unknown / To Be Discovered

- diagnostic retention duration
- automated deletion policy
- production database sizing
- ingestion rate limiting and abuse protection
- external logging or telemetry provider
- correlation or request id propagation
- diagnostics listing, filtering, or dashboard workflow
- whether additional client operations or applications are needed

## Non-Goals

- Frontend failure detection or classification implementation.
- React, Vue, or Angular production-code changes.
- Generating a client failure reference when the diagnostics endpoint is
  unreachable.
- Offline diagnostic queue or retry behavior.
- General application logging framework.
- Metrics, tracing, alerting, health, readiness, or liveness.
- Correlation or request id propagation.
- Diagnostic listing, search, filter, export, or dashboard.
- Retention cleanup implementation.
- Adding `MAIN_ADMIN`, generic `ADMIN`, support, or operations roles.
- Changing `IT_ADMIN` assignment or lifecycle.
- Changing auth login, credential verification, session, or login failure audit
  behavior.
- Removing, renaming, or generalizing `loginFailureReferenceId`.
- Changing organization behavior.
- Storing raw technical or sensitive client data.
