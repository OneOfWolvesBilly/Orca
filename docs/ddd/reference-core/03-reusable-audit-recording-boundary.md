# DDD Derivation - 03 Reusable Audit Recording Boundary

Status: Approved / Implemented; public artifact amendment locally verified; committed staged and published proof pending.

This note is **derived from**
`docs/specs/reference-core/03-reusable-audit-recording-boundary.md`.
It does not introduce new behavior.

## Implementation Checkpoint (2026-09-30)

The user authorized implementation after SDD/DDD commit `ff43aa8`. The existing
seven core value/port types already satisfy the contract; no runtime behavior
or global failure policy changed. Fifteen backend audit tests pass, and the
independent artifact consumer passes six audit and three compilation/signature
tests, including typed safe mapping and explicit Spring test wiring.

The [deployment implementation evidence](../deployment/03-versioned-backend-artifact-delivery.md#public-audit-implementation-evidence-2026-09-30)
records all failure mappings, component/source guards, test counts and limits.
The implementation is locally verified, but the development artifact comes
from an uncommitted tree. Committed staged, exact runtime-matrix and published
proof remain pending. The user authorized the joint tests-plus-implementation
commit on 2026-10-01; this does not upgrade the prior development evidence.

## Public Artifact Amendment Derivation (2026-09-29)

Authority: the Public Artifact Audit Amendment in
[reference-core-03](../../specs/reference-core/03-reusable-audit-recording-boundary.md)
and the single consumer proof in
[deployment-03](../../specs/deployment/03-versioned-backend-artifact-delivery.md).
The user authorized DDD after SDD closeout. This derivation does not authorize
TDD, implementation, publication or Git delivery by itself. The user
subsequently authorized the SDD/DDD documentation commit, TDD, and then
implementation. The implementation commit was subsequently authorized on 2026-10-01; delivery
remains separately gated.

The single outcome remains independent artifact consumption of a valid audit
record through an exact supported API and a consumer-provided recorder.
Reference-core owns the support value and port. Deployment owns proving that
contract across a versioned artifact boundary. No new aggregate, bounded
context, shared event catalog, audit endpoint or persistence model is derived.

### Model and Public Type Closure

Retain the seven existing types in
`io.github.oneofwolvesbilly.orca.referencecore.application` because the amended
spec explicitly selects their public signatures. A new facade or package move
would add compatibility work without closing a different requirement.

| Type | Model role | Rule placement and allowed dependencies |
| --- | --- | --- |
| `AuditEventType` | workflow-owned identifier value | non-null/non-blank string in its canonical constructor; factory delegates; JDK only |
| `AuditActorId` | workflow-owned actor identifier value | same structural rule; no auth lookup, role or identity inference |
| `AuditOutcome` | workflow-owned result identifier value | same structural rule; no central outcome catalog |
| `AuditMetadataEntry` | immutable string pair | non-null/non-blank key/value in constructor; no generic object value |
| `AuditMetadata` | immutable collection value | defensive copy, reject null collection/entries and exact duplicate keys; expose immutable entries |
| `AuditRecord` | complete immutable support envelope | require typed fields, caller `Instant`, metadata and non-blank optional identifiers when present; no external state |
| `AuditRecorder` | application-facing outbound port implemented by consumer | `void record(AuditRecord)`; signature depends only on the support record, with no adapter technology |

`AuditRecord` is a consistency boundary for construction, not a domain
aggregate or transaction boundary. Its full constructor and both factories
converge on the same invariant checks. The short factory represents omitted
optional identifiers as null and omitted metadata as the empty value. Explicit
null metadata is not equivalent to omission. Java record equality/hash-code
semantics remain value semantics, not serialization or logging contracts.

The public type graph closes over these seven types and JDK values such as
`String`, `Instant`, `List` and `Collection`. Public constructors, factories,
component accessors and port signatures must not force callers to name sibling
application classes, framework annotations, internal exceptions or adapters.
Only the seven named types are supported; the enclosing package is not public
as a whole. Changes to their signatures follow deployment-03 compatibility
rules rather than a package-placement recommendation.

### Call Flow and Failure Placement

```text
Consumer typed fixture input
  -> consumer-owned allowlisting mapper
  -> public audit value construction (complete structural validation)
  -> one explicit call to consumer-provided AuditRecorder
  -> normal void return OR caller-observable recorder failure
```

Construction completes before the recorder is called. Failed construction
therefore leaves invocation count at zero. The recorder interface is a port,
not a validation proxy: a non-null valid record is the caller precondition,
and an arbitrary consumer implementation cannot be claimed to be intercepted
by Orca. Do not introduce a central use case, Spring auto-configuration,
default recorder or decorator merely to enforce unsupported direct null calls.

Preserve the specified Java failure categories: null structure rejects with
`NullPointerException`; blank strings and duplicate metadata keys reject with
`IllegalArgumentException`. Message wording is not a contract. Erased raw
collection misuse is rejected during construction; its runtime exception
message/type is not standardized beyond the spec's rejection invariant.

One submission invokes the selected recorder once. Two explicit submissions
remain two calls. This is not a delivery idempotency guarantee. A throwing
recorder is observed by the consumer caller without core catching, retrying,
buffering or converting it to a shared outcome. No choice is made about the
success/failure of a future product operation when recording fails.

### Consumer Mapping and Fixture Ownership

The consumer mapper and recorder are outside the backend artifact. The
standalone fixture is the deployment-owned verification host, not a new Orca
business context. Its event identifier, constant outcome, synthetic actor,
occurrence time and exact metadata allowlist are defined by deployment-03;
this note does not invent alternatives to that mapping.

The mapper receives a typed fixture input, builds only the authorized fields,
and omits the confidential synthetic test detail. Assert the entire resulting
record, not only absence of a suspicious key. Test each forbidden-data category
using sentinel values and verify no value is copied into any output field.
Real secrets, raw request/session objects and unrestricted maps are not mapper
inputs. These tests prove this fixture's mapping, not every future consumer's
semantic safety. Future consuming workflows still own their own mapper tests.

Test recorders capture already-validated records or deliberately throw a
sentinel failure. They remain fixture/test assets and are never added to Orca's
published component. No auth/organization emission path, centralized store,
transport or production adapter is derived.

### Failure Set to Test Placement

| Class | Detection and placement | Proof |
| --- | --- | --- |
| absent | public construction/default paths; consumer configuration requires explicit recorder | A1, A2; standalone wiring positive/negative cases |
| null | each reachable public constructor/factory and metadata collection entry path; optional identifier absence remains valid | A2 plain tests and standalone zero-recorder-call cases |
| blank | every required string, optional supplied identifier and metadata key/value | A2 constructor/factory matrix |
| malformed | incompatible scalar/time arguments at Java compile boundary; erased metadata at runtime | A3 negative compilation and runtime rejection |
| duplicate | metadata collection consistency checks; repeated record submissions remain separate | A2 duplicate keys with equal/different values; A4 invocation counts |
| unsupported | exact public type/signature guard; arbitrary non-blank event/outcome identifiers remain supported | A1 positive custom identifiers; A3 supported/forbidden compilation |
| untyped | raw collection constructor/factory paths with wrong, binary, nested or exception objects | A3 runtime tests; no generics-only waiver |
| stale | artifact/version evidence belongs to deployment; supplied instant is preserved without age policy | A1 time preservation; D-AUDIT-4 |
| unauthorized | consumer typed mapper excludes forbidden semantics; dependency guard rejects unsupported Orca types | A5 full-envelope assertions; A3 / D-AUDIT-1; no actor authorization rule |
| unexpected | consumer recorder call exposes failure without core recovery; artifact linkage/startup failures belong to deployment | A4 exception and exact invocation count; D-AUDIT-3 / D-AUDIT-4 |

### Verification Design and Next Layer

| Spec proof | Derived test placement | Observable evidence |
| --- | --- | --- |
| A1 | standalone Spring consumer contract tests plus plain value tests | exact record/defaults, equality/hash values, custom identifiers, supplied instant, two interchangeable recorder implementations and missing-recorder rejection |
| A2 | expand existing `AuditRecordTest`; repeat public consumption cases against artifact | every null/blank/duplicate path, optional defaults, immutable metadata despite input/output mutation, zero recorder calls on invalid construction |
| A3 | plain raw-collection tests and standalone compile/dependency contract harness | allowed public signature graph uses only seven audit/JDK types; invalid scalar signatures fail compilation, erased invalid collections fail at runtime, forbidden dependencies fail guard |
| A4 | existing `AuditRecorderTest` and standalone consumer recorder tests | void completion, one/two explicit submissions, observed sentinel exception and no extra calls |
| A5 | standalone fixture typed-mapper tests | exact full envelope and single metadata allowlist; forbidden sentinel categories excluded from all fields |

Support model and port tests stay plain JUnit without Spring. No artificial
domain model or domain service is introduced to satisfy a layer label.
Standalone host wiring uses Spring only at the fixture boundary; the same
mapper/port path is directly testable without HTTP, credentials or a database.
Deployment's derived note places artifact assembly, guard enforcement and
staged/published verification. Backend unit tests alone never satisfy A1-A5's
independent artifact requirement.

DDD closeout: every amendment A1-A5 requirement and all ten failure classes
have model, owner, rule and test placement. The seven-type API matches the
spec; no global failure policy, semantic secret scanner, production storage or
workflow adoption is added. The selected DELIVERY/ARCH/DOC portions continue
with locally verified implementation/TDD and remaining release proof; all other
approved dispositions remain.
DDD completion is not a claim that expanded tests, implementation, staged
artifact proof or published-version proof have passed.

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

Spec-defined public placement:

```text
io.github.oneofwolvesbilly.orca.referencecore.application
```

Rationale:

- The boundary is a reusable application-facing support port.
- It is not a web contract.
- It is not infrastructure.
- It is not an auth or organization domain model.

The seven public types retain these exact qualified names. The former
sub-package flexibility is superseded by the public artifact amendment; a
public package/signature change requires an authoritative compatibility
decision. Internal types remain outside the supported consumer surface.

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
