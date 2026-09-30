# Spec 03 - Reusable Audit Recording Boundary

Status: Baseline Approved / Implemented; public artifact audit amendment SDD and DDD complete / TDD pending.

## Public Artifact Audit Amendment (2026-09-29)

Planning path: `continue current capability`.

Intake was approved by the user after revalidation at local main
`ee169174615e12844776237ba42727c302ecbb3d`. This amendment and deployment-03's
matching repair define exactly one outcome: an application developer in an
independent Spring Boot consumer creates a valid audit record through the
supported contract of a versioned Orca artifact and submits it to a
consumer-provided recorder, without copied Orca source or unsupported imports.

This is the owner-side public-boundary amendment required before deployment
may claim audit consumption. Deployment-03 owns artifact delivery and its
independent proof; it must not define audit rules. No new slice number, bounded
context, package move, production recorder, or workflow adoption is introduced.
The baseline behavior below remains authoritative except for the previous
statement that this spec does not name Java types: the supported public
artifact contract is now explicitly enumerated here.

### Current-capability Evidence and Intake

- The Core V1 Release Outcome requires product-neutral audit consumption.
- The baseline has seven public audit types in `referencecore.application`,
  validated record construction, and a replaceable recording port. The nine
  existing `AuditRecordTest` / `AuditRecorderTest` cases pass on the observation
  baseline (Java 22.0.2). This is repository-local evidence only.
- Deployment-03's previous three-auth-type allowlist and its shell import guard
  exclude these audit imports. Its standalone fixture tests auth and migration,
  not reusable audit. Java visibility and JAR presence do not grant support.
- No missing credential, session renewal, logging, storage, or workflow-adoption
  predecessor is needed. The missing owner-approved external boundary is
  addressed by this amendment itself before downstream delivery work.
- `ORCA-DELIVERY-01`, `ORCA-ARCH-01`, and `ORCA-DOC-01` are included only for
  this outcome's artifact proof, exact public boundary, and document alignment.
  Their unrelated remainders stay active. `ORCA-AUDIT-01` workflow adoption and
  `ORCA-OPS-01` logging remain deferred, as do the other active items recorded
  by the approved intake. No entire problem cluster is closed by this SDD.

### Ownership and Allowed Dependencies

| Mechanism | Owner | Authoritative predecessor | Allowed contract | Completion state |
| --- | --- | --- | --- | --- |
| envelope and common structural validation | reference-core support scope | this spec's baseline Validation and Ownership Boundary | exact audit value types below | baseline implemented; external contract verification pending |
| replaceable recording and observable failure | reference-core support scope | this spec's baseline Failure Policy Boundary | `AuditRecorder.record(AuditRecord)` | baseline implemented; standalone proof pending |
| event meaning and sensitive-data exclusion | consuming workflow; deployment owns only the no-domain verification fixture | this spec's consuming-workflow obligations | consumer-owned typed mapper, allowlisted fields, consumer-provided recorder | fixture proof must be added after TDD authorization |
| versioned artifact and source dependency guard | deployment support scope | deployment-03 | exact Maven coordinate and explicit type allowlist | auth delivery tooling implemented; audit amendment and publication proof pending |
| existing host auth behavior | auth | auth-12 and its completed predecessors | three existing auth API types and existing HTTP contracts | implemented; regression only, no audit adoption |

The reusable audit API does not require embedded auth enablement, a database,
Spring, or a logging framework. The independent proof runs inside a consumer
Spring Boot host to prove artifact integration; that host does not turn those
technologies into audit API dependencies. Audit actor identifiers do not prove
authentication or product authorization.

### Exact Supported Audit API

The following seven existing types, and only these audit types, are supported
for direct consumer use under
`io.github.oneofwolvesbilly.orca.referencecore.application`:

| Type | Supported operations and value shape |
| --- | --- |
| `AuditEventType` | public constructor and `of(String)`; `value()` |
| `AuditActorId` | public constructor and `of(String)`; `value()` |
| `AuditOutcome` | public constructor and `of(String)`; `value()` |
| `AuditMetadataEntry` | public constructor and `of(String, String)`; `key()`, `value()` |
| `AuditMetadata` | public constructor taking `List<AuditMetadataEntry>`, `of(Collection<AuditMetadataEntry>)`, `empty()`, immutable `entries()` |
| `AuditRecord` | public canonical constructor; both `create` overloads described below; all eight component accessors |
| `AuditRecorder` | consumer-implemented functional interface: `void record(AuditRecord record)` |

`AuditRecord` components, in order, are `AuditEventType eventType`,
`AuditActorId actorId`, `Instant occurredAt`, `AuditOutcome outcome`,
`String tenantId`, `String resourceType`, `String resourceId`, and
`AuditMetadata metadata`. The full factory accepts those eight arguments. The
four-argument factory accepts the first four and supplies absent optional
identifiers and empty metadata. Canonical constructors and factories enforce
the same structural rules; neither is a validation bypass. The six record
value types retain Java record value equality and hash-code semantics; their
string rendering is not a safe logging, wire, or persistence contract.

This is an explicit support decision based on the existing minimal boundary,
not a declaration that all public classes in `application` packages are APIs.
It does not expose client diagnostics, auth internals, organization audit
ports, test recorders, or any other sibling type. A future package rename or
signature change must follow deployment-03's compatibility policy; changing
this allowlist is a contract decision, not a guard-only edit.

### Construction, Submission, and Responsibility Contract

- Required values and their strings reject null; required strings reject blank.
  `occurredAt` is a caller-supplied `java.time.Instant`. A local date-time or
  display string is not an accepted substitute.
- Optional identifier null means absent. A supplied optional identifier must
  be non-blank. Omitted metadata uses `AuditMetadata.empty()`; explicit null
  metadata, collection, or entry is invalid, not coerced to empty.
- Metadata entries have only non-null, non-blank string keys and values. Exact
  duplicate keys are rejected. Input collection mutation and mutation through
  returned entries cannot alter a created record's metadata.
- Null rejection uses `NullPointerException`; blank identifiers/entries and
  duplicate keys use `IllegalArgumentException`, preserving the baseline Java
  behavior. Exception message text is not a public contract or safe diagnostic.
- A valid non-null record is a caller precondition of `record`. The functional
  interface does not centrally intercept arbitrary consumer implementations.
  The consumer proof must not pass null to a recorder and must demonstrate
  zero recorder invocations when record construction fails. It must not claim
  that Orca validates every arbitrary direct call to consumer code.
- The consumer explicitly supplies the recorder. No Orca default recorder,
  Spring discovery convention, storage adapter, retry, buffering, transaction,
  or global fail-open/fail-closed policy is added. Missing recorder wiring
  cannot count as successful consumer integration.
- The configured recorder receives one record for one explicit valid
  submission and returns `void`. A throwing recorder's failure remains
  observable to the caller; no Orca retry, suppression, fallback, or synthetic
  success may intervene. This does not promise exactly-once delivery or
  persistence, and does not select a workflow's recovery policy.
- The baseline Sensitive Data Boundary applies to every field, not only
  metadata. Consumer-owned typed mapping and tests enforce semantic safety.
  The core performs structural checks, not secret-string classification.

### Public Failure Set and Verification Obligations

The proof identifiers below are future required evidence, not implemented tests.
All constructor/factory paths that can admit the listed values must be covered.

| Class | Applicable input / normative outcome | Required proof |
| --- | --- | --- |
| absent | missing required envelope value cannot construct a record; omitted optional identifiers and metadata are valid through the documented default path; missing consumer recorder cannot satisfy integration | A2 required-field/default cases; A1 explicit wiring and missing-recorder negative case |
| null | null required wrapper/string/time, metadata object, collection, entry, key or value rejects before recorder invocation; null optional identifiers mean absent | A2 parameterized construction cases and zero-invocation assertion; null direct-port calls are outside the valid-record precondition |
| blank | empty/whitespace required strings, present optional identifiers, metadata key/value reject before recording | A2 tests for each field and constructor/factory path |
| malformed | local date-time/display string cannot replace `Instant`; binary, nested, exception or arbitrary-object metadata cannot enter the string-entry contract | A3 negative Java compilation, plus runtime cases where type erasure permits entry |
| duplicate | duplicate metadata keys reject before recording, including equal values; repeated independent records are not deduplicated by core | A2 duplicate-key cases; A4 two explicit calls remain two submissions, with no exactly-once claim |
| unsupported | unsupported types/signatures/imports cannot satisfy approved integration; unknown non-blank workflow event/outcome strings remain allowed because core has no catalog | A3 compilation/dependency guard; A1 consumer-defined identifiers; deployment-03 runtime matrix |
| untyped | raw collections containing wrong objects, nested values, binary values or exceptions reject during metadata construction before recorder invocation; incompatible scalar Java arguments fail compilation | A3 raw-collection runtime cases for constructor and factory, plus negative compilation; do not dismiss erasure solely because generics exist |
| stale | obsolete artifact/API/metadata cannot count as current release proof; core adds no age restriction to a valid caller-supplied instant | deployment-03 provenance/version proof; A1 preserves supplied instant without inventing freshness policy |
| unauthorized | unsupported Orca dependencies fail the consumer guard; forbidden sensitive values must be excluded by the workflow mapper; an actor string confers no authorization | A3 forbidden-dependency proof; A5 exact-field/allowlist and sensitive-input exclusion proof; no new reader or role policy |
| unexpected | consumer recorder exception remains observable with no Orca retry or fallback; resolution, linkage or startup failure invalidates artifact proof | A4 throwing recorder and invocation-count assertions; deployment-03 failure evidence |

Raw collection rejection need not standardize the JVM exception message or
promise one cross-language deserialization API. Its invariant is rejection
before recorder invocation. Reflection/unsafe memory manipulation is not a
supported bypass contract; no HTTP/JSON audit endpoint is introduced.

| Proof | Normative coverage | Future verification |
| --- | --- | --- |
| A1 | public construction, exact values/defaults, record value equality/hash semantics, consumer identifiers, recorder replacement, supplied instant, explicit recorder wiring | standalone Spring Boot consumer against exact artifact; two consumer recorders receive the expected record; missing-recorder configuration fails the consumer proof without inventing core auto-wiring |
| A2 | complete required/optional/null/blank/duplicate structure, immutable metadata, same constructor/factory validation | plain audit contract tests plus standalone boundary cases; recorder count remains zero on rejection |
| A3 | complete public type closure, forbidden dependencies, malformed/untyped metadata | independent compilation of allowed API usage and rejected invalid signatures; dependency guard includes ordinary/static imports and fully qualified source references; raw-collection runtime tests |
| A4 | normal void completion, repeated explicit calls, observable recorder failure, no core retry/fallback | consumer-owned success/throwing recorders with exact invocation counts and caller-observed failure |
| A5 | typed mapping and semantic safety without core content inspection | fixture mapper tests assert the entire envelope and exact metadata allowlist; typed fixture input carrying forbidden sentinel data cannot leak it into any record field |

A3 also verifies that the audit public signatures and their construction path
require only the seven audit types and JDK types, with no Spring, persistence,
logging or transport API dependency. Package contents elsewhere in the backend
artifact do not establish such an audit API dependency.

No audit behavior proof is waived as manual-only. Source-level negative
compilation is reproducible automated evidence for values Java cannot call
with; erased collection inputs require runtime tests. External distribution
proof and its limited manual exceptions remain owned by deployment-03.

### Amendment Acceptance and Closeout

The amendment requires A1-A5 and the deployment-03 standalone artifact proof.
The original Acceptance Criteria, Sensitive Data Boundary, Failure Policy
Boundary, and Non-Goals remain in force. There is no auth/organization workflow
adoption, audit database, query, retention, export, outbox, logging/correlation,
credential migration, React publication, or CogniRig integration.

SDD closeout (2026-09-29): the approved overlap dispositions map to the exact
public contract, full failure set, A1-A5, deployment evidence levels, and the
non-goals above. No normative success or failure is left without a planned
proof or stated contract boundary. The subsequent user-authorized DDD closeout
(2026-09-29) derives these obligations in the matching notes. SDD and DDD are
complete; TDD, implementation, staged and published-version proof remain pending.
The baseline nine passing tests do not close the amendment's expanded matrix.

Affected records are this spec, deployment-03, README, product baseline,
workflow map, capability map, slice map, and the canonical ignored handoff.
Both matching DDD notes now derive the amendment model, dependency and test
placement. Their design completion is not implementation or release evidence.
No auth or organization spec is superseded. Deployment-03's former auth-only
consumer restriction is superseded only for these seven audit types. Broad
active problem records retain their unselected remainders and next steps.

## Baseline Slice Intake

Slice candidate: `reference-core-03` reusable audit recording boundary.

Workflow:

- Logging, Observability, and Operations.
- Login Failure Support / Audit.
- Existing and future protected command workflows.

Workflow gap:

- Orca has auth-owned login failure audit state from `auth-10`.
- Orca has safe client diagnostics from `reference-core-02`.
- Orca does not yet define a product-neutral audit recording boundary that
  consuming products or Orca application workflows can use without sharing a
  database, logging framework, or product-specific event model.

Primary actor:

- Application developer integrating a consuming product or Orca workflow with
  an audit recorder.

Successful outcome:

- A caller can submit one validated product-neutral audit record through a
  replaceable recording port.
- The audit boundary does not require Spring, JDBC, Kafka, OpenSearch, a shared
  database, or a logging framework.
- A consuming product can define its own typed product event and map that event
  into the Orca audit envelope outside Orca core.

Failure flows:

- A structurally invalid common audit record is rejected before it reaches the
  recorder.
- Recorder implementation failure policy is not globally fixed by this slice.

Existing supported slices:

- `auth-10` login failure audit.
- `reference-core-01` stable API error contract.
- `reference-core-02` client diagnostics foundation.
- Existing protected command workflows in auth and organization.

Planned predecessor slices:

- None.

Unknowns:

- production retention policy
- audit reader actor
- audit lookup workflow
- audit storage adapter choice
- event-specific failure policy

Non-goals:

- centralized audit database
- audit lookup, search, dashboard, export, or retention management
- Kafka, OpenSearch, SIEM, or cloud audit integration
- transactional outbox, outbox table, or outbox dispatcher
- generic domain event bus
- event sourcing
- product-specific event definitions
- changing auth-10 login failure audit behavior
- changing organization command behavior

Decision: enter SDD.

## Goal

Define a reusable audit recording boundary for Orca and consuming products.

This slice establishes a product-neutral audit recording port, a stable minimum
audit record envelope, common structural validation, and the ownership boundary
for workflow-specific audit mapping and sensitive-data safety. It enables a
caller to provide a recorder implementation without Orca requiring a
centralized audit database or owning consuming-product business event semantics.

This slice does not implement production storage, retention, search, or a
specific logging or messaging adapter.

## Reference-Core Scope

`reference-core` is a cross-cutting support scope, not a domain bounded
context.

This slice owns:

- the product-neutral audit recording port
- the common audit record envelope
- common structural validation
- the boundary between common validation and workflow-owned semantic safety
- test utility expectations for verifying emitted audit records

Auth remains authoritative for auth-owned login failure audit behavior.
Organization remains authoritative for organization command behavior.
Consuming products remain authoritative for their own business event names,
typed event models, metadata, and storage choices.

## Contract Terms

- Audit Recorder
  A product-neutral port that accepts one validated audit record.

- Audit Record
  A product-neutral envelope describing who acted, what action occurred, when it
  occurred, which resource was affected, and what outcome was recorded.

- Audit Occurrence Time
  One unambiguous instant supplied by the caller. It is not a local date-time or
  a client-formatted display value.

- Audit Event Type
  A stable product- or workflow-owned event name mapped into the audit envelope.
  Orca reference-core validates that it is non-blank but does not define
  consuming-product event catalogs.

- Audit Outcome
  A stable non-blank workflow-owned result identifier mapped into the common
  envelope. Reference-core validates its presence but does not define a shared
  outcome catalog.

- Audit Metadata
  An optional immutable collection of non-blank string key/value entries.
  Reference-core owns this structural representation. The consuming workflow
  owns the allowed keys, their meanings, and the safety of their values.

## Minimum Audit Record Envelope

The audit record envelope must contain:

- `eventType`
- `actorId`
- `occurredAt`
- `outcome`

The audit record envelope may contain:

- `tenantId`
- `resourceType`
- `resourceId`
- `metadata`

The baseline envelope behavior below is unchanged. The Public Artifact Audit
Amendment now names the exact supported Java API for versioned consumers;
implementation design beyond that public contract remains a DDD concern.

## Validation and Ownership Boundary

Reference-core performs only validation that can be decided from the common
record structure:

- `eventType`, `actorId`, and `outcome` must be non-blank
- `occurredAt` must be present and represent one unambiguous instant
- `tenantId`, `resourceType`, and `resourceId` must be non-blank when present
- metadata keys and values must be non-blank strings
- metadata keys must be unique within one audit record
- metadata must be immutable after the audit record is created
- metadata must not accept null values, binary values, nested structures,
  exception objects, or arbitrary objects
- the complete common structure is validated before the recorder receives it

The consuming workflow owns every rule that requires product or bounded-context
meaning:

- its typed event or command-result model
- its event-type and outcome identifiers
- its actor representation, including activity for which no authenticated actor
  can be established
- its resource and tenant meanings
- its exact metadata key allowlist
- the mapping from typed workflow data into the common envelope
- exclusion of passwords, credentials, raw session values, tokens, raw
  requests, raw responses, and other forbidden sensitive values
- tests proving that its mapper emits only the fields authorized by that
  workflow's specification

A consuming workflow must use a typed mapper whose inputs and output fields are
defined by that workflow. It must not use an unrestricted request, session,
domain object, exception, or generic object map as the audit mapping contract.

Reference-core does not determine semantic sensitivity from string content,
invent actor identifiers, or define product-specific metadata. Storage,
transport, and recorder recovery policy remain outside this slice. Recorder
failure must remain observable to the calling workflow; reference-core does not
retry, suppress, or convert it into a global outcome.

## Scenarios

### Scenario: Caller records a product-neutral audit record

**Given**
- A caller has a complete audit record with event type, actor id, occurrence
  time, and outcome.
- The record contains no forbidden sensitive data.

**When**
- The caller submits the record to the audit recorder.

**Then**
- The recorder accepts one product-neutral audit record.
- The caller does not need Spring, a database, Kafka, OpenSearch, or a logging
  framework to use the boundary.

### Scenario: Consuming product maps its own typed event

**Given**
- A consuming product defines a typed product event outside Orca core.
- The consuming product maps that event to the Orca audit envelope.

**When**
- The mapped audit record is submitted.

**Then**
- Orca accepts the product-neutral audit record if it satisfies the envelope and
  safety rules.
- Orca does not define or depend on the consuming product's typed event class.

### Scenario: Recorder implementation is replaceable

**Given**
- A caller depends only on the audit recording port.

**When**
- The caller is supplied a different recorder implementation.

**Then**
- The caller can submit the same validated audit record.
- The core audit API does not depend on the storage or transport technology.

## Acceptance Criteria

- Reference-core MUST define a product-neutral audit recording boundary.
- A caller MUST be able to submit one audit record through a replaceable
  recorder port.
- Successful recording MUST complete without returning a storage- or
  transport-specific identifier.
- The core audit API MUST NOT depend on Spring, JPA, JDBC, Kafka, OpenSearch, a
  cloud service, or a specific logging framework.
- The audit envelope MUST include event type, actor id, occurrence time, and
  outcome.
- Required string fields MUST be non-blank.
- Optional identifier fields MUST be non-blank when present.
- Audit metadata MUST be an immutable collection of non-blank string key/value
  entries.
- Audit metadata keys MUST be unique within one audit record.
- Audit metadata MUST NOT accept null, binary, nested, exception, or arbitrary
  object values.
- The audit boundary MUST allow consuming products to define typed product
  events outside Orca and map them to the audit envelope.
- Orca MUST NOT define consuming-product event types such as alarms, evidence
  cases, permission discovery, or offboarding events.
- Each consuming workflow MUST define its event and outcome identifiers, actor
  representation, metadata allowlist, sensitive-data exclusions, and typed
  mapper before it emits a common audit record.
- A consuming workflow's mapper MUST NOT include passwords, raw session values,
  credential secrets, recovery codes, private keys, full authentication tokens,
  raw requests, raw responses, or unrestricted sensitive objects.
- Reference-core MUST NOT determine semantic sensitivity from metadata string
  content.
- Application logging and audit recording MUST remain separate concerns.
- This slice MUST NOT require centralized Orca audit storage.
- This slice MUST NOT change auth-10 login failure audit behavior.
- This slice MUST NOT change organization behavior.

## Sensitive Data Boundary

Audit records and metadata MUST NOT contain:

- password or credential secret
- raw session cookie value or raw session id
- TOTP secret
- recovery code
- private key
- full authentication token
- raw request or response body
- request headers
- unrestricted exception object or stack trace
- unrestricted user, credential, session, role, organization, or profile object

The consuming workflow enforces this boundary through a workflow-owned typed
mapper and mapper tests. Reference-core prevents unrestricted object metadata
through its structural API, but it cannot infer the semantic meaning of an
arbitrary string. That limitation does not permit a consuming workflow to place
a forbidden value under a different key.

## Failure Policy Boundary

This slice must not hardcode one global audit failure policy.

Future workflows may choose policies such as:

- best effort
- fail open
- fail closed
- buffer and retry

The recording port must keep recorder failure observable to the calling
workflow. Reference-core does not retry, suppress, or convert recorder failure
into one global outcome. Event-specific policy selection remains future work.

## Invariants

- Reference-core owns the reusable audit boundary, not consuming-product
  business event semantics.
- Consuming products own product-specific event definitions and mappings.
- The audit envelope is product-neutral.
- Reference-core owns common structural validation.
- Consuming workflows own semantic field allowlists and sensitive-data safety.
- Application logs are not audit records.
- Storage and transport are adapters, not core API requirements.

## Error Cases

- Missing event type -> rejected before recording.
- Blank event type -> rejected before recording.
- Missing actor id -> rejected before recording.
- Blank actor id -> rejected before recording.
- Missing occurrence time -> rejected before recording.
- Local date-time without an unambiguous instant -> not accepted by the common
  occurrence-time contract.
- Missing outcome -> rejected before recording.
- Blank outcome -> rejected before recording.
- Blank optional identifier -> rejected before recording.
- Blank metadata key or value -> rejected before recording.
- Duplicate metadata key -> rejected before recording.
- Null, binary, nested, exception, or arbitrary object metadata value -> not
  accepted by the common metadata contract.
- Recorder implementation failure -> reported to the calling workflow without a
  reference-core retry or fallback policy.

## Unknown / To Be Discovered

- production audit retention period
- audit reader actor
- audit access policy
- audit lookup, search, or export workflow
- storage adapter selection
- event-specific failure policy
- whether auth-10 should later map login failure audit into the reusable
  boundary
- whether organization auditable events should later use the reusable boundary

## Non-Goals

- Centralized Orca audit database.
- Audit search UI.
- Audit lookup endpoint.
- Audit retention management.
- Production storage or transport adapter.
- Transactional outbox, outbox table, or outbox dispatcher.
- External audit platform integration.
- Product-specific event definitions.
- Generic domain event bus.
- Event sourcing.
- Changing auth-10 login failure audit.
- Changing organization command behavior.
