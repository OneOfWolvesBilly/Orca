# Spec 03 - Reusable Audit Recording Boundary

Status: Approved / Implemented.

## Slice Intake

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

The exact implementation type is derived in DDD and code. This specification
defines behavior and safety requirements, not Java class names.

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
