# Deployment 01 - Local Runtime Build Plan

Status: Draft / build plan authority only.

## Goal

Define the first deployment support slice for Orca as a from-zero local runtime
build plan for open-source users who may start with a clean machine.

The immediate purpose is to make the database-backed login runtime supportable:
frontend, backend, and MariaDB must eventually run together without changing
auth, organization, reference-core, or frontend business behavior.

This slice is a planning and safety slice. It defines the required equipment
inspection, build plan, installation decision points, execution gates, and
runtime safety boundaries before any concrete deployment assets are created.

`deployment` is a delivery/runtime support scope, not a bounded context.

The local environment preflight is a deployment support workflow gate. It is
not an Orca application behavior, not a domain behavior slice, and not a user
feature. It exists to protect the developer's machine before installation,
upgrade, runtime asset creation, or runtime execution.

## Workflow Traceability

- Workflows:
  - Authentication and Session
  - Frontend Reference Shell
  - Logging, Observability, and Operations
- Workflow gap:
  - Orca has login and frontend behavior that can be implemented and tested,
    but a clean open-source user has no authoritative path for preparing the
    local database-backed runtime needed to practice login.
  - Installation, upgrade, and runtime execution can change a developer machine
    and must not begin before the current machine state is inspected.
  - Runtime setup can leak credentials into source files, terminal output, or
    Git history unless the plan defines secret handling before execution.
- Primary actor:
  - Developer or open-source contributor
- Supporting actors:
  - local frontend runtime
  - local backend runtime
  - local MariaDB runtime
- Predecessor slices:
  - `auth-08` password login with server-side session
  - `auth-09` protected HTTP session context
  - `auth-10` login failure audit
  - `reference-core-01` stable API error contract
  - `reference-core-02` client diagnostics foundation
  - `frontend-01` login result shell
  - `frontend-02` client failure observability

## Deployment Support Scope

`deployment-01` describes how a clean local machine should be inspected and
planned before Orca's already-specified behavior is run as separated runtime
components.

It does not own auth, organization, reference-core, or frontend behavior.

Auth remains authoritative for:

- password login
- `ORCA_SESSION`
- server-side session state
- protected HTTP session context
- `loginFailureReferenceId`

Reference-core remains authoritative for:

- stable API error responses
- client diagnostic ingestion and lookup
- `clientFailureReferenceId`

Frontend remains authoritative for:

- login shell behavior
- client failure classification
- safe user-visible presentation

Organization remains authoritative for:

- group creation and invitation lifecycle behavior
- organization command authorization and rejection rules

## From-Zero Local Runtime Build Plan

The intended runtime shape is:

```text
Browser
  -> local frontend entry point
  -> frontend runtime component
  -> backend runtime component
  -> internal MariaDB runtime component
```

The plan assumes the developer may have none of the required tools installed.
The first implementation work must therefore proceed in phases.

This clean-machine assumption is a documentation baseline, not a claim about
the actual developer machine. Future work must use preflight results as the
source of truth for the current machine and must reuse compatible existing
tools when the selected build strategy allows it.

### Phase 0: Confirm Authority And Scope

Before executing any local runtime command, the future implementation slice
must confirm:

- the work is still deployment support, not a bounded context
- the target is local runtime support for existing login/frontend/database
  behavior
- no auth, organization, reference-core, or frontend business rule is being
  added, replaced, or reinterpreted
- no production deployment claim is being made

### Phase 1: Inspect The Local Machine

The first executable step of any future installation, upgrade, runtime asset
creation, or runtime execution is a local environment preflight inventory.

Preflight itself is not a standalone product slice. It is the required entry
gate for deployment support work that could change the machine or depend on
local runtime state.

The preflight inventory must happen before:

- installing Java, Node.js, Docker, Kubernetes, MariaDB, package managers, or
  any local runtime tool
- upgrading an existing local runtime tool
- selecting a local Kubernetes target
- creating manifests, scripts, Secrets, ConfigMaps, Dockerfiles, compose files,
  ignored local files, or runtime profiles
- running Docker, Kubernetes, database, backend, or frontend runtime commands
- changing ports, volumes, runtime profiles, or local secret storage

The inventory must record, at minimum:

- operating system name and version
- CPU architecture
- Java version and executable location, if present
- Node.js and package manager versions and executable locations, if present
- Docker CLI presence and version, if present
- Docker runtime or daemon availability, if intentionally checked
- Kubernetes CLI presence and version, if present
- local Kubernetes target candidates, such as Docker Desktop Kubernetes,
  minikube, kind, or another selected target, if present
- MariaDB or MySQL client/server tooling presence and versions, if present
- occupied local ports relevant to frontend, backend, database, and routing
- existing local environment files, profiles, volumes, or secret storage paths
  that could affect the planned runtime

The preflight output must not print secret values. If a command could reveal
secret values, the future slice must use a safer inspection method or document
why the check is skipped.

If the current machine state conflicts with the planned installation, upgrade,
port usage, volume usage, profile usage, or secret-storage path, the future
slice must stop before changing the machine and record the conflict, risk, and
required decision.

### Phase 2: Decide The Local Build Strategy

After preflight, the future implementation slice must choose and document:

- required Java version policy and whether an existing Java installation is
  acceptable
- required Node.js and package manager policy for the frontend runtime
- whether Docker is required for the first local runtime
- whether Kubernetes is required for the first local runtime
- the local database strategy for MariaDB
- the local secret mechanism
- frontend-to-backend routing approach that preserves browser cookie behavior
  for `ORCA_SESSION`
- expected local ports and collision handling
- runtime profile names and environment variable names
- local reset or cleanup boundary

The decision must prefer the smallest runtime shape that lets a clean
open-source user run the login-facing database environment safely.

### Phase 3: Authorize Installation Or Upgrade

Installation and upgrade instructions may be introduced only after Phase 1
preflight and Phase 2 build strategy are documented.

Future installation or upgrade work must:

- state which tool is being installed or upgraded
- state why the tool is required for the selected build strategy
- state the minimum supported version and the accepted existing-version range
- stop before changing the machine when an incompatible existing installation
  is found
- avoid replacing a user's working tools without explicit approval
- provide verification commands that do not print secrets

This draft does not install, upgrade, or execute any tool.

### Phase 4: Authorize Runtime Assets

Concrete runtime assets may be created only after the build strategy is
accepted.

Future runtime assets may include Kubernetes manifests, Dockerfiles, compose
files, scripts, ignored local env files, or secret instructions only when a
future authoritative deployment spec explicitly allows them.

Before any runtime asset is created, the future slice must define:

- file locations
- secret placeholders and secret creation process
- validation steps that confirm real secret values are not staged or committed
- database exposure boundary
- frontend-to-backend routing boundary
- rollback or cleanup expectations

This draft does not create runtime assets.

### Phase 5: Execute The Local Runtime

Runtime execution may begin only after preflight, build strategy, installation
or upgrade decisions, and runtime assets have been approved by a future slice.

Execution must prove:

- the frontend entry point is reachable locally
- backend runtime configuration is external to application source code
- MariaDB is reachable by the backend runtime only
- Flyway remains the schema ownership boundary
- login behavior remains owned by auth specs
- frontend behavior remains owned by frontend specs
- error and diagnostic behavior remains owned by reference-core specs

This draft does not execute Docker, Kubernetes, database, backend, or frontend
runtime commands.

## Configuration Boundary

Runtime-only values must be separated from source-controlled behavior.

Sensitive runtime values include:

- database username
- database password
- root or administrator database password
- generated session or cookie secrets, if introduced later
- any credential, token, certificate private key, or password used only to run
  the local environment

Sensitive runtime values must not be committed to the repository.

Non-sensitive runtime values may be documented or committed only when they do
not expose credentials, user data, session data, or environment-specific secret
material. Examples may include local service names, non-secret ports, profile
names, and component names.

## Local Database Topology Boundary

The first runtime topology is standalone local MariaDB.

```text
backend runtime component
  -> internal MariaDB endpoint
  -> single MariaDB runtime instance
  -> persistent local storage boundary, if the chosen runtime supports it
```

MariaDB must not be exposed through a browser-facing route, public ingress,
host port intended for product users, or NodePort-equivalent user path.

Primary/replica MariaDB, database operators, cloud-managed databases, backup
strategy, restore strategy, and production database ownership are outside this
slice. They may be proposed only as separate future slices.

## Frontend To Backend Boundary

The local frontend must consume the backend through a local arrangement that
preserves browser cookie behavior for `ORCA_SESSION`.

The concrete routing pattern is unknown / to be discovered. Future choices may
include local port-forwarding, local ingress, or a local same-origin reverse
proxy.

Any future routing choice must preserve:

- frontend-01 login request behavior
- frontend-02 client diagnostic submission behavior
- auth-owned cookie opacity
- reference-core stable API error behavior
- no frontend reimplementation of backend business rules

## Scenarios

### Scenario: Clean-machine contributor starts from zero

**Given**
- A contributor wants to run Orca's login-facing local runtime.
- The contributor may not have Java, Node.js, Docker, Kubernetes, or MariaDB
  tooling installed.

**When**
- The contributor starts deployment work.

**Then**
- The first executable step is local environment preflight.
- The current machine state is recorded before install, upgrade, asset
  creation, or runtime execution.
- The plan stops before changing the machine if a conflict is found.

### Scenario: Build strategy is selected after preflight

**Given**
- The local environment preflight has recorded existing tools, versions, ports,
  profiles, volumes, and secret-storage paths.

**When**
- A future deployment slice selects the local runtime strategy.

**Then**
- The slice documents whether Java, Node.js, Docker, Kubernetes, and MariaDB
  tooling must be installed, upgraded, reused, or skipped.
- The slice documents the selected local database and routing strategy.
- The slice documents why the selected strategy is the smallest safe path for
  the login-facing runtime.

### Scenario: Installation is gated by explicit approval

**Given**
- A future deployment slice needs to install or upgrade a local runtime tool.

**When**
- The slice prepares installation instructions.

**Then**
- The slice states the tool, reason, version policy, and verification command.
- The slice stops before replacing an incompatible existing installation.
- The slice does not print secret values during verification.

### Scenario: Sensitive runtime configuration is kept out of Git

**Given**
- A future local runtime requires database credentials or other runtime-only
  secrets.

**When**
- A future implementation slice defines the runtime assets.

**Then**
- The implementation uses an explicit local secret mechanism.
- The secret values are not committed.
- Documentation examples use placeholders rather than real values.
- The implementation includes a verification step for staged or committed
  secret values.

### Scenario: Runtime preserves existing behavior ownership

**Given**
- A future local runtime runs frontend, backend, and MariaDB components.

**When**
- A user performs login, observes login failure, submits client diagnostics, or
  calls protected commands.

**Then**
- Auth remains authoritative for login, sessions, and login failure references.
- Reference-core remains authoritative for stable API errors and client
  diagnostics.
- Frontend remains authoritative for login shell presentation and client
  failure classification.
- Organization remains authoritative for organization command behavior.
- Deployment runtime wiring does not reinterpret or rewrite those rules.

## Acceptance Criteria

- Deployment MUST be treated as a delivery/runtime support scope, not a bounded
  context.
- Local environment preflight MUST be treated as a deployment support workflow
  gate, not as Orca application behavior or a domain behavior slice.
- This slice MUST be treated as Draft until the preflight command set, build
  strategy, installation policy, and runtime execution plan are reviewed.
- This slice MUST define a from-zero local runtime build plan for an
  open-source user who may have no local runtime tools installed.
- This slice MUST NOT create Kubernetes manifests, Secrets, ConfigMaps,
  Docker runtime files, scripts, ignored local env files, or executable runtime
  changes.
- Future deployment implementation, installation, upgrade, or runtime asset
  work MUST begin with a local environment preflight inventory.
- The local environment preflight inventory MUST happen before installing,
  upgrading, creating runtime assets, creating secrets, or executing Docker or
  Kubernetes commands.
- The local environment preflight inventory MUST avoid printing secret values.
- If the local environment preflight finds a version, port, volume, profile, or
  secret-storage conflict, the future slice MUST stop before changing the
  machine and record the required decision.
- Future installation or upgrade instructions MUST name the tool, reason,
  version policy, verification command, and stop condition.
- Future local runtime implementation MUST preserve auth, organization,
  reference-core, and frontend business behavior.
- Future local runtime implementation MUST keep sensitive runtime values out of
  Git.
- Future local runtime implementation MUST choose an explicit local secret
  mechanism before creating runtime assets.
- Future local runtime implementation MUST document how to verify that secret
  values are not staged or committed.
- Runtime configuration MUST be external to application source code.
- MariaDB MUST remain backend-internal in the local runtime boundary.
- MariaDB schema ownership MUST remain with Flyway migrations.
- Frontend-to-backend local routing MUST preserve browser cookie behavior for
  `ORCA_SESSION`.
- Frontend-to-backend local routing MUST NOT require backend business behavior
  changes.
- Production deployment MUST remain a separate future slice.

## Invariants

- Deployment support does not own domain invariants.
- Deployment support does not introduce a domain aggregate.
- Deployment support does not define auth, organization, reference-core, or
  frontend business rules.
- Local environment preflight is an operational gate for deployment work, not
  product behavior exposed by Orca.
- Runtime wiring is an adapter concern around already-specified behavior.
- Sensitive runtime values are not source-controlled behavior.
- Database schema ownership remains separate from runtime wiring.
- Machine-changing work is not allowed before local environment preflight.

## Error Cases

- A proposed deployment change installs or upgrades a tool before preflight ->
  reject before changing the machine.
- A proposed deployment change creates runtime assets before build strategy
  approval -> reject before implementation.
- A proposed deployment change commits a real secret value -> reject before
  implementation.
- A proposed deployment change exposes MariaDB through a user-facing route ->
  reject before implementation.
- A proposed deployment change requires frontend code to parse or store
  `ORCA_SESSION` -> reject as a frontend/auth boundary violation.
- A proposed deployment change changes login, session, login audit, diagnostic,
  or organization command behavior -> reject as a scope violation.
- A proposed deployment change treats local runtime setup as production-ready
  -> reject as a production deployment boundary violation.
- A preflight command would print secret values -> replace it with a safer
  check or skip it with an explicit risk note.

## Unknown / To Be Discovered

- exact preflight command set for each supported developer machine
- accepted Java version range and upgrade policy
- accepted Node.js and package manager version range
- whether the first local runtime should require Docker
- whether the first local runtime should require Kubernetes
- concrete local Kubernetes target, if Kubernetes is selected
- future runtime asset path
- exact local secret creation mechanism
- exact frontend-to-backend routing pattern
- whether local routing uses port-forwarding, local ingress, or reverse proxy
- local developer reset workflow
- production deployment model
- production database ownership and backup strategy
- health, readiness, liveness, metrics, tracing, and alerting

## Non-Goals

- Creating Kubernetes manifests.
- Creating Kubernetes Secrets.
- Creating ConfigMaps.
- Creating Dockerfiles, compose files, or image build scripts.
- Creating ignored local env files.
- Executing Docker.
- Executing Kubernetes commands.
- Installing or upgrading Java, Node.js, Docker, Kubernetes, MariaDB, or any
  other runtime tool in this slice.
- Selecting Docker Desktop, minikube, kind, or another local cluster target in
  this slice.
- Production Kubernetes deployment.
- Cloud provider selection.
- CI/CD pipeline setup.
- TLS, ingress hardening, production DNS, WAF, or edge security.
- Production-grade secret management.
- Local seed data workflow.
- Health, readiness, liveness, metrics, tracing, or alerting.
- Database replication.
- Database backup or restore workflow.
- Changing auth login, credential verification, session, or login audit
  behavior.
- Changing reference-core API error or client diagnostics behavior.
- Changing frontend login shell or client observability behavior.
- Changing organization domain, application, web, or persistence behavior.
