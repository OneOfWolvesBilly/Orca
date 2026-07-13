# Spec 03 - Reusable Audit Recording Boundary

Status: Proposed.

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

- An audit record containing forbidden sensitive data is rejected before it is
  recorded.
- An incomplete or unbounded audit record is rejected before it is recorded.
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
- generic domain event bus
- event sourcing
- product-specific event definitions
- changing auth-10 login failure audit behavior
- changing organization command behavior

Decision: enter SDD.

## Goal

Define a reusable audit recording boundary for Orca and consuming products.

This slice establishes a product-neutral audit recording port, a stable minimum
audit record envelope, and shared safety rules for rejecting forbidden sensitive
values. It enables a caller to provide a recorder implementation without Orca
requiring a centralized audit database or owning consuming-product business
event semantics.

This slice does not implement production storage, retention, search, or a
specific logging or messaging adapter.

## Reference-Core Scope

`reference-core` is a cross-cutting support scope, not a domain bounded
context.

This slice owns:

- the product-neutral audit recording port
- the common audit record envelope
- shared validation and sensitive-data restrictions
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

- Audit Event Type
  A stable product- or workflow-owned event name mapped into the audit envelope.
  Orca reference-core validates its shape but does not define consuming-product
  event catalogs.

- Audit Outcome
  A bounded result classification such as success or rejection. The first slice
  defines only the minimum values needed by the envelope.

- Audit Metadata
  Optional bounded key/value details that must be serializable and must not
  contain forbidden sensitive data or unrestricted objects.

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
- bounded `metadata`

The exact implementation type is derived in DDD and code. This specification
defines behavior and safety requirements, not Java class names.

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

### Scenario: Forbidden sensitive metadata is rejected

**Given**
- An audit record contains a password, raw session value, credential secret,
  recovery code, private key, full authentication token, or unrestricted
  sensitive object in metadata.

**When**
- The audit record is validated.

**Then**
- The record is rejected before recording.
- The forbidden value is not exposed through an audit response, test helper, or
  recorder implementation contract.

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
- The core audit API MUST NOT depend on Spring, JPA, JDBC, Kafka, OpenSearch, a
  cloud service, or a specific logging framework.
- The audit envelope MUST include event type, actor id, occurrence time, and
  outcome.
- Optional tenant, resource, and metadata fields MUST remain bounded.
- The audit boundary MUST allow consuming products to define typed product
  events outside Orca and map them to the audit envelope.
- Orca MUST NOT define consuming-product event types such as alarms, evidence
  cases, permission discovery, or offboarding events.
- Audit metadata MUST NOT accept passwords, raw session values, credential
  secrets, recovery codes, private keys, full authentication tokens, or
  unrestricted sensitive objects.
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

Audit metadata must be allowlisted, bounded, and serializable. Arbitrary
`Map<String, Object>` usage must not be the only type-safety mechanism for
product-specific events.

## Failure Policy Boundary

This slice must not hardcode one global audit failure policy.

Future workflows may choose policies such as:

- best effort
- fail open
- fail closed
- buffer and retry

This slice only requires that the core API design does not prevent those future
policies. Event-specific policy selection remains future work.

## Invariants

- Reference-core owns the reusable audit boundary, not consuming-product
  business event semantics.
- Consuming products own product-specific event definitions and mappings.
- The audit envelope is product-neutral.
- Application logs are not audit records.
- Audit records must reject forbidden sensitive values before recording.
- Storage and transport are adapters, not core API requirements.

## Error Cases

- Missing event type -> rejected before recording.
- Blank event type -> rejected before recording.
- Missing actor id -> rejected before recording.
- Missing occurrence time -> rejected before recording.
- Missing outcome -> rejected before recording.
- Unbounded metadata -> rejected before recording.
- Forbidden sensitive metadata -> rejected before recording.
- Recorder implementation failure -> handled by the supplied implementation or
  future workflow-specific failure policy.

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
- Kafka adapter.
- OpenSearch adapter.
- SIEM integration.
- Cloud audit provider integration.
- TOTP implementation.
- QR authentication implementation.
- Passkey / WebAuthn implementation.
- Product-specific event definitions.
- Generic domain event bus.
- Event sourcing.
- Changing auth-10 login failure audit.
- Changing organization command behavior.
