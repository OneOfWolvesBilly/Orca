# DDD Derivation - 03 Reusable Audit Recording Boundary

Status: Proposed.

This note is **derived from**
`docs/specs/reference-core/03-reusable-audit-recording-boundary.md`.
It does not introduce new behavior.

## Scope Ownership

**reference-core cross-cutting support scope**

Rationale:

- The audit recording boundary supports auth, organization, and future
  consuming products without owning their domain semantics.
- Auth remains authoritative for login failure audit behavior from `auth-10`.
- Organization remains authoritative for organization command behavior.
- Consuming products own their own typed events and mappings.

`reference-core` remains a support scope, not a domain bounded context.

## Consistency Boundary

**AuditRecord**

This is a support contract value rather than a domain aggregate. It is modeled
as one validated envelope because:

- one record describes one auditable occurrence
- the minimum envelope must be validated before recording
- sensitive metadata must be rejected before any adapter receives the record
- storage and transport are replaceable adapters

The model must not grow into a shared business event hierarchy or a generic
domain event platform.

## Minimum Model

### Support model

- `AuditRecord`
  - event type
  - actor id
  - occurrence timestamp
  - outcome
  - optional tenant id
  - optional resource type
  - optional resource id
  - optional bounded metadata

- `AuditOutcome`
  - minimum bounded outcome values derived during implementation
  - should remain product-neutral

- `AuditMetadata`
  - bounded serializable details
  - rejects forbidden sensitive values
  - must not be the only type-safety mechanism for consuming-product events

### Application ports

- `AuditRecorder`
  - records one validated audit record
  - has no dependency on Spring, database, logging framework, Kafka, OpenSearch,
    or cloud services

- test recorder / assertion utility
  - supports verifying that a workflow emitted an expected audit record
  - must not expose forbidden sensitive values

## Rule Placement

### Reference-core support rules

- Validate required audit envelope fields.
- Reject blank event type and actor id.
- Reject forbidden sensitive values before recording.
- Keep metadata bounded and serializable.
- Keep audit recording separate from application logging.

### Consuming-product rules

- Define typed product events.
- Decide which product actions require audit.
- Map typed product events to the Orca audit envelope.
- Choose storage adapter and failure policy where appropriate.

### Auth rules

- Auth-owned login failure audit remains governed by `auth-10`.
- A later slice may decide whether auth-10 maps into the reusable audit
  boundary.
- This slice does not change login behavior, session behavior, or
  login-failure troubleshooting references.

### Organization rules

- Organization command behavior remains governed by organization specs.
- A later slice may decide whether existing organization auditable events use
  the reusable audit boundary.
- This slice does not reopen organization domain behavior.

### Infrastructure rules

- No production storage adapter is derived in this slice.
- No centralized audit table is required.
- Future adapters may be implemented only after an authoritative spec or
  workflow requires them.

## Package Placement

Recommended placement:

```text
io.github.oneofwolvesbilly.orca.referencecore.application
```

Rationale:

- The boundary is a reusable application-facing support port.
- It is not a web contract.
- It is not infrastructure.
- It is not an auth or organization domain model.

Implementation may introduce subpackages if the existing codebase pattern
requires it, but the boundary must remain under `referencecore`, not a new
bounded context.

## Failure Policy

No global failure behavior is derived.

The core port should allow future workflows to choose:

- best effort
- fail open
- fail closed
- buffer and retry

This slice only requires the API shape to avoid preventing those policies.

## Sensitive Data Design

Use validation and allowlisting before recording rather than redaction after
recording.

Rationale:

- Redaction requires first accepting sensitive data.
- Arbitrary object metadata makes leakage difficult to review.
- Product-specific typed events provide stronger modeling than unrestricted
  metadata bags.

Forbidden values include passwords, raw session values, credential secrets,
TOTP secrets, recovery codes, private keys, full authentication tokens, raw
headers, raw bodies, and unrestricted sensitive objects.

## Test Layer Placement

Support model tests:

- required fields are enforced
- blank event type is rejected
- blank actor id is rejected
- outcome is required
- metadata remains bounded and serializable
- forbidden sensitive metadata is rejected

Application tests:

- a caller can submit one product-neutral audit record
- recorder implementation is replaceable
- consuming-product typed events can be mapped outside Orca
- product-specific event classes are not required in Orca production code

Dependency tests:

- core audit API does not require Spring
- core audit API does not require a database
- core audit API does not require a logging framework

Regression tests:

- auth-10 login failure behavior remains unchanged
- organization command behavior remains unchanged

Infrastructure tests:

- none for the first slice unless a no-op or in-memory adapter is explicitly
  specified.

## Future Adapter Candidates

Future slices may define adapters such as:

- no-op recorder
- in-memory test recorder
- SLF4J adapter
- JDBC adapter
- Kafka adapter
- OpenSearch adapter
- customer-provided implementation

These are candidates only. This DDD note does not specify or require them.

## Non-Goals

- Centralized audit storage.
- Audit lookup or search.
- Retention management.
- Product-specific event catalog.
- Generic domain event bus.
- Event sourcing.
- Auth-10 migration.
- Organization audit migration.
- Production adapter implementation.
