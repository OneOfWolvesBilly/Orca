# DDD Derivation - 03 Reusable Audit Recording Boundary

Status: Approved / Implemented.

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
- common structural validation happens before any adapter receives the record
- semantic field safety remains with the workflow-owned mapper
- storage and transport are replaceable adapters

The model must not grow into a shared business event hierarchy or a generic
domain event platform.

## Minimum Model

### Support model

- `AuditRecord`
  - non-blank workflow-owned event type
  - non-blank workflow-owned actor id
  - caller-supplied instant-based occurrence time
  - non-blank workflow-owned outcome
  - optional non-blank tenant id
  - optional non-blank resource type
  - optional non-blank resource id
  - optional immutable audit metadata

- `AuditEventType`
  - non-blank stable identifier supplied by the consuming workflow
  - reference-core does not define the event catalog

- `AuditActorId`
  - non-blank audit identifier supplied by the consuming workflow
  - actor meaning is not inferred by reference-core

- `AuditOutcome`
  - non-blank stable identifier supplied by the consuming workflow
  - reference-core does not define the outcome catalog

- `AuditMetadata`
  - immutable collection of `AuditMetadataEntry` values
  - contains at most one entry for each key
  - exposes no arbitrary-object metadata API

- `AuditMetadataEntry`
  - non-blank string key
  - non-blank string value
  - key allowlist and value meaning belong to the consuming workflow

- optional audit reference values
  - tenant id, resource type, and resource id are non-blank when present
  - their meanings belong to the consuming workflow

### Application ports

- `AuditRecorder`
  - records one validated audit record
  - returns no storage- or transport-specific identifier
  - has no dependency on Spring, database, logging framework, Kafka, OpenSearch,
    or cloud services

- test recorder / assertion utility
  - supports verifying that a workflow emitted an expected audit record
  - stores only the already-validated common audit record for assertions
  - exists in test sources only and is not a production recorder adapter

## Rule Placement

### Reference-core support rules

- Validate required audit envelope fields.
- Reject blank event type, actor id, and outcome.
- Reject blank optional identifiers when present.
- Accept metadata only through the immutable string-entry structure defined by
  the spec.
- Reject blank metadata keys and values before recording.
- Reject duplicate metadata keys before recording.
- Keep audit recording separate from application logging.

### Consuming-product rules

- Define typed product events.
- Decide which product actions require audit.
- Map typed product events to the Orca audit envelope.
- Define event and outcome identifiers, actor representation, resource meaning,
  and the exact metadata key allowlist.
- Exclude forbidden sensitive values through the typed mapper.
- Test the exact mapped record against the workflow specification.
- Choose the recorder failure policy for each auditable workflow.

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

No global recovery behavior is derived.

The core port should allow future workflows to choose:

- best effort
- fail open
- fail closed
- buffer and retry

Recorder failure remains observable to the calling workflow. Reference-core
does not retry, suppress, or translate the failure into a shared outcome. This
keeps workflow-specific policies possible without selecting one in this slice.

## Sensitive Data Design

Use workflow-owned typed mapping and allowlisting before common record
construction rather than redaction after recording.

Rationale:

- Redaction requires first accepting sensitive data.
- Arbitrary object metadata makes leakage difficult to review.
- Product-specific typed events provide stronger modeling than unrestricted
  metadata bags.
- Generic content inspection cannot prove that an opaque string is not a raw
  credential or session value.

Forbidden values include passwords, raw session values, credential secrets,
TOTP secrets, recovery codes, private keys, full authentication tokens, raw
headers, raw bodies, and unrestricted sensitive objects.

Reference-core prevents null, nested, exception, and arbitrary object metadata
by exposing only immutable non-blank string entries. The consuming mapper owns
semantic value safety because only the workflow knows what each string means.
Reference-core does not determine semantic sensitivity from string content.

## Test Layer Placement

Support model tests:

- required fields are enforced
- blank event type, actor id, and outcome are rejected
- blank optional identifiers are rejected when present
- metadata is immutable after record construction
- blank metadata keys and values are rejected
- duplicate metadata keys are rejected
- null, binary, nested, exception, and arbitrary object metadata cannot enter
  the common metadata model

Application tests:

- a caller can submit one product-neutral audit record
- recorder implementation is replaceable
- consuming-product typed events can be mapped outside Orca
- product-specific event classes are not required in Orca production code
- the test recorder captures the exact validated record for assertions

Dependency tests:

- core audit API does not require Spring
- core audit API does not require a database
- core audit API does not require a logging framework

Regression tests:

- auth-10 login failure behavior remains unchanged
- organization command behavior remains unchanged

Infrastructure tests:

- none; this slice defines no production infrastructure adapter

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
- Transactional outbox, outbox table, or outbox dispatcher.
