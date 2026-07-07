# DDD - Deployment 01 - Local Runtime Build Plan

Status: Draft / derived from
`docs/specs/deployment/01-secure-local-runtime-boundary.md`.

## Purpose

This note derives the model boundary and rule placement for `deployment-01`.
It explains how the deployment support scope should be understood while the
first local runtime plan is still a draft.

It must not introduce behavior beyond the deployment spec.

## Scope Classification

`deployment` is a delivery/runtime support scope.

It is not a domain bounded context because it does not own business concepts,
domain invariants, aggregates, repositories, or application commands.

The slice describes planning boundaries around existing behavior:

- clean-machine local runtime build plan
- local environment preflight gate
- installation and upgrade decision gates
- runtime component boundary
- external runtime configuration
- local secret handling boundary
- internal database endpoint

## Model Boundary

No domain aggregate is introduced.

The useful modeling terms are support-scope terms:

- Local Runtime Build Plan
- Local Environment Preflight
- Build Strategy Decision
- Installation Gate
- Runtime Asset Gate
- Runtime Execution Gate
- Runtime Component
- Runtime Configuration
- Sensitive Runtime Value
- Local Secret Mechanism
- Internal Database Endpoint
- Stop Condition

These terms help prevent misplaced business rules. They are not new domain
entities and should not be implemented as domain model objects.

Local Environment Preflight is modeled as an operational gate, not as an Orca
application use case. It may later be expressed as documented commands,
checklists, or tooling, but it should not become domain code or application
business behavior.

## Rule Placement

### Deployment Support Rules

Deployment support owns rules about:

- which machine state must be inspected before installation, upgrade, runtime
  asset creation, or runtime execution
- why preflight is a gate for deployment support work rather than an app
  behavior slice
- which build-strategy decisions must be made after preflight
- which installation and upgrade decisions require explicit approval
- which runtime components are part of the first local boundary
- which values are sensitive runtime values
- which runtime values must stay out of Git
- which future work must choose an explicit secret mechanism
- which future work must keep MariaDB backend-internal
- which future work must preserve browser cookie behavior

These rules belong in deployment documentation first. A later implementation
slice may translate them into commands, manifests, scripts, ignored local
files, or verification checks only after its own spec approves those assets.

### Auth Rules

Auth remains authoritative for:

- password login
- `ORCA_SESSION`
- server-side session state
- protected HTTP session context
- `loginFailureReferenceId`

Deployment must not parse, encode, generate, reinterpret, or expose these
concepts beyond providing runtime configuration that lets existing auth
behavior run.

### Reference-Core Rules

Reference-core remains authoritative for:

- stable API error contract
- client diagnostic records
- client diagnostic lookup
- `clientFailureReferenceId`

Deployment must not define diagnostic fields, lookup permissions, retention, or
public error shape.

### Frontend Rules

Frontend remains authoritative for:

- login shell behavior
- client runtime failure classification
- safe presentation of support references
- framework parity expectations

Deployment may support hosting or routing in a future slice, but it must not
change frontend result behavior or reimplement backend rules in the client.

### Organization Rules

Organization remains authoritative for:

- group creation
- invitation lifecycle
- organization permission checks
- organization persistence behavior

Deployment must not add organization commands, roles, read models, or workflow
behavior.

## Test Layer Placement

This slice creates no production code and therefore requires no domain,
application, infrastructure, or frontend tests.

Future implementation slices should place tests according to the asset they
introduce:

- documentation-only changes need documentation review and link verification
- local environment preflight work needs safe command review and sample output
  review that confirms no secret values are printed
- installation or upgrade instructions need version-policy verification and
  stop-condition review
- runtime asset generation may need static file checks
- backend runtime configuration changes may need integration tests
- frontend routing changes may need frontend or browser-level verification

No future deployment test should assert auth, organization, reference-core, or
frontend business behavior by duplicating those rules. It should verify that
the runtime wiring preserves the behavior owned by those specs.

## Design Decisions

### Decision: Keep deployment outside bounded contexts

Deployment is an adapter/support concern. Treating it as a bounded context
would imply ownership over behavior that belongs to auth, organization,
reference-core, or frontend.

### Decision: Mark deployment-01 as draft

The first deployment slice needs to describe a clean-machine path, not only a
runtime boundary. Draft status prevents future work from treating the current
plan as executable deployment approval before preflight commands, build
strategy, installation policy, and runtime execution are reviewed.

### Decision: Start from local environment preflight

Installation, upgrade, and runtime execution depend on the developer machine's
current state. A future deployment slice must inspect existing tools, versions,
ports, volumes, profiles, and local secret storage before changing anything.
This prevents accidental upgrades, conflicting installations, port collisions,
or unsafe secret handling.

The preflight step is not split out as a normal behavior slice because it does
not describe a user-visible Orca workflow. It is the entry condition for any
deployment support work that can change or rely on local machine state.

### Decision: Separate build strategy from execution

A clean open-source user may have no local runtime tooling installed. The
build strategy must therefore be chosen after preflight and before concrete
runtime assets or execution commands exist.

### Decision: Gate installation and upgrade work

Installing or upgrading Java, Node.js, Docker, Kubernetes, MariaDB, or any
other runtime tool changes the user's machine. Deployment work must name the
tool, reason, version policy, verification command, and stop condition before
making that change.

### Decision: Keep MariaDB internal to backend runtime access

The local database is a backend dependency for runtime practice. Publishing it
as a user-facing endpoint would expand the runtime attack surface and confuse
database access with product behavior.

### Decision: Preserve standalone MariaDB as the first topology

Standalone local MariaDB is enough to practice service wiring, external
configuration, Flyway schema ownership, and persistent runtime state. Replica
topologies require separate database operational decisions and should remain a
future slice.

### Decision: Defer concrete local routing

Port-forwarding, local ingress, and reverse proxy routing each affect browser
cookie behavior differently. The deployment spec preserves the cookie behavior
requirement while leaving the concrete routing choice to a future
implementation slice.

## Risk Notes

- Skipping local environment preflight could overwrite working tools, hide
  incompatible versions, collide with existing ports, or run unsafe commands
  before the developer understands the machine state.
- Treating draft planning as executable approval could create manifests,
  scripts, or secrets before the local strategy is reviewed.
- Hard-coded credentials would turn runtime setup into source-controlled
  secrets.
- Committed local manifests with real values would make the repository unsafe
  for sharing.
- Exposing MariaDB directly would bypass the backend boundary.
- Treating local runtime topology as production-ready would hide production
  operations decisions such as backup, restore, TLS, monitoring, and database
  ownership.
- Duplicating auth or frontend rules inside deployment would make future
  behavior changes inconsistent.

## Non-Goals Confirmed

- No Kubernetes manifests.
- No Kubernetes Secrets.
- No ConfigMaps.
- No Dockerfiles or compose files.
- No ignored local env files.
- No runtime execution.
- No tool installation or upgrade.
- No production deployment baseline.
- No local seed data workflow.
- No health/readiness/liveness behavior.
- No changes to auth, organization, reference-core, or frontend behavior.
