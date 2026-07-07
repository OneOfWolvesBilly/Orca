# Deployment 01 - Secure Local Runtime Boundary

Status: Approved / documentation authority only.

## Goal

Define the first deployment support slice for Orca: a secure local runtime
boundary for practicing the already-specified frontend, backend, and MariaDB
runtime shape without committing sensitive runtime configuration or changing
business behavior.

This slice establishes deployment authority for local runtime wiring,
configuration boundaries, and safety rules. It does not create Kubernetes
manifests, Docker runtime files, Secrets, ConfigMaps, scripts, production
deployment assets, or executable runtime changes.

`deployment` is a delivery/runtime support scope, not a bounded context.

## Workflow Traceability

- Workflows:
  - Authentication and Session
  - Frontend Reference Shell
  - Error and Exception Handling
  - Logging, Observability, and Operations
- Workflow gap:
  - backend and frontend behavior can be built and tested, but there is no
    authoritative deployment spec for safe local runtime practice
  - local runtime practice risks placing secrets in source files, terminal
    logs, manifests, or Git history without an explicit boundary
  - frontend/backend/database separation needs a documented runtime shape that
    preserves already-specified behavior
- Primary actor:
  - Developer
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

`deployment-01` describes how already-specified Orca behavior may be run
locally as separated runtime components. It does not own auth, organization,
reference-core, or frontend behavior.

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

## Local Runtime Boundary

The first local runtime boundary is:

```text
Browser
  -> local frontend entry point
  -> frontend runtime component
  -> backend runtime component
  -> MariaDB runtime component
```

The local runtime may later be implemented with Kubernetes, but this slice does
not select or create the concrete implementation. A future implementation slice
must decide the local cluster target, file layout, and executable assets before
creating them.

The local runtime boundary must preserve these rules:

- Local environment inventory happens before any install, upgrade, runtime
  asset creation, Docker action, Kubernetes action, or secret creation.
- The browser reaches only the local user-facing entry point required for
  manual practice.
- The backend receives runtime configuration from external runtime sources, not
  hard-coded source values.
- MariaDB is reachable by the backend runtime only.
- MariaDB must not be exposed through a browser-facing route, public ingress,
  host port, or NodePort-equivalent path.
- Database schema remains owned by Flyway migrations.
- Runtime topology must not change auth, organization, reference-core, or
  frontend business behavior.

## Local Environment Preflight

Every future deployment implementation, installation, upgrade, or runtime asset
slice must start by inspecting the developer machine's current state.

The preflight inventory must happen before:

- installing a local runtime tool
- upgrading an existing runtime tool
- selecting a local Kubernetes target
- creating manifests, scripts, Secrets, ConfigMaps, or ignored local files
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

If the current machine state conflicts with the planned installation or
upgrade, the future slice must stop before changing the machine and record the
conflict, risk, and required decision.

This slice does not run the preflight inventory. It makes that inventory a
mandatory first step for future executable deployment work.

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

## Secret Safety Rules

A future implementation slice must choose an explicit local secret mechanism
before creating runtime assets.

Allowed categories for the future mechanism include:

- manually created local Kubernetes Secret values
- generated local-only secret values
- ignored local files loaded by an explicit local process

This slice does not choose one mechanism.

Secret handling must satisfy:

- Secret values must not be stored in committed manifests.
- Secret values must not be printed as required terminal output.
- Secret values must not appear in documentation examples except as obvious
  placeholders.
- Secret values must not be used as test fixtures, seed data, or product
  behavior.
- A future implementation must document how to verify that no secret value is
  staged or committed.

## Local Database Topology Boundary

The first runtime topology is standalone local MariaDB.

```text
backend runtime component
  -> internal MariaDB endpoint
  -> single MariaDB runtime instance
  -> persistent local storage boundary, if the chosen runtime supports it
```

Primary/replica MariaDB, database operators, cloud-managed databases, backup
strategy, restore strategy, and production database ownership are outside this
slice. They may be proposed only as separate future slices.

Frontend and backend behavior must not depend on whether the database is
standalone, replicated, local, or managed. Backend runtime configuration must
provide the database endpoint and credentials.

## Frontend to Backend Boundary

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

### Scenario: Developer identifies the local runtime boundary

**Given**
- Orca has implemented backend and frontend behavior from the predecessor
  slices.
- A developer wants to practice running Orca locally as separated runtime
  components.

**When**
- The developer reads the deployment support specification.

**Then**
- The developer can identify the intended local frontend, backend, and MariaDB
  runtime boundary.
- The developer can identify that deployment is a support scope, not a bounded
  context.
- The developer can identify that this slice does not create executable
  runtime assets.

### Scenario: Sensitive runtime configuration is kept out of Git

**Given**
- A future local runtime requires database credentials or other runtime-only
  secrets.

**When**
- A future implementation slice defines the runtime assets.

**Then**
- The implementation must use an explicit local secret mechanism.
- The secret values must not be committed.
- Documentation examples must use placeholders rather than real values.
- The implementation must include a verification step for staged or committed
  secret values.

### Scenario: Future runtime work starts with local machine inventory

**Given**
- A future deployment slice proposes installing, upgrading, configuring, or
  running local runtime tooling.

**When**
- The future slice begins implementation planning.

**Then**
- The first executable step is a local environment preflight inventory.
- The inventory records existing tool versions and relevant local runtime
  state before changes are made.
- The inventory avoids printing secret values.
- Installation, upgrade, runtime asset creation, Docker actions, Kubernetes
  actions, and secret creation wait until the inventory is complete.

### Scenario: Local runtime preserves existing behavior ownership

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

### Scenario: Database remains internal to the local runtime

**Given**
- A future local runtime includes MariaDB.

**When**
- The runtime exposes a user-facing entry point.

**Then**
- The database is not exposed as a browser-facing route.
- The database is not published for direct user access.
- Backend-to-database access uses runtime configuration.
- Flyway remains the schema ownership boundary.

## Acceptance Criteria

- Deployment MUST be treated as a delivery/runtime support scope, not a bounded
  context.
- This slice MUST NOT create Kubernetes manifests, Secrets, ConfigMaps, Docker
  runtime files, scripts, or executable runtime changes.
- This slice MUST define only the support authority and safety boundary for
  future local runtime work.
- Future deployment implementation, installation, upgrade, or runtime asset
  work MUST begin with a local environment preflight inventory.
- The local environment preflight inventory MUST happen before installing,
  upgrading, creating runtime assets, creating secrets, or executing Docker or
  Kubernetes commands.
- The local environment preflight inventory MUST avoid printing secret values.
- If the local environment preflight finds a version, port, volume, profile, or
  secret-storage conflict, the future slice MUST stop before changing the
  machine and record the required decision.
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
- Runtime wiring is an adapter concern around already-specified behavior.
- Sensitive runtime values are not source-controlled behavior.
- Database schema ownership remains separate from runtime wiring.

## Error Cases

- A proposed deployment change commits a real secret value -> reject before
  implementation.
- A proposed deployment change exposes MariaDB through a user-facing route ->
  reject before implementation.
- A proposed deployment change requires frontend code to parse or store
  `ORCA_SESSION` -> reject as a frontend/auth boundary violation.
- A proposed deployment change changes login, session, login audit, diagnostic,
  or organization command behavior -> reject as a scope violation.
- A proposed deployment change treats local manifests as production-ready ->
  reject as a production deployment boundary violation.
- A proposed install, upgrade, or runtime execution starts without local
  environment preflight -> reject before changing the machine.
- A preflight command would print secret values -> replace it with a safer
  check or skip it with an explicit risk note.

## Unknown / To Be Discovered

- concrete local Kubernetes target
- future runtime asset path
- exact local secret creation mechanism
- exact frontend-to-backend routing pattern
- exact preflight command set for each supported developer machine
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
- Executing Docker.
- Executing Kubernetes commands.
- Selecting Docker Desktop, minikube, kind, or another local cluster target.
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
