# Deployment 02 - Local MariaDB Login Runtime

Status: Proposed / runtime asset authorization required before execution.

## Goal

Define the local MariaDB runtime path that lets a developer manually exercise
the already-specified password login success and failure behavior.

This slice authorizes the next implementation work to create local runtime
assets only after the current machine preflight is recorded and reviewed. It
does not authorize installing tools, upgrading tools, creating production
deployment assets, or changing auth, reference-core, organization, or frontend
business behavior.

`deployment` remains a delivery/runtime support scope, not a bounded context.

## Workflow Traceability

- Workflows:
  - Authentication and Session
  - Logging, Observability, and Operations
  - Frontend Reference Shell
- Workflow gap:
  - Login success and failure behavior is specified, but a developer needs a
    safe local MariaDB runtime path to exercise the database-backed behavior
    manually.
  - Local runtime credentials and login test data must stay out of Git.
  - Flyway must remain the schema owner before manual testing.
- Primary actor:
  - Developer or open-source contributor
- Supporting actors:
  - backend local profile
  - local MariaDB runtime
  - local frontend runtime or direct API client
- Predecessor slices:
  - `deployment-01`
  - `auth-08`
  - `auth-10`
  - `reference-core-01`
  - `frontend-01`

## Preflight Baseline

The local runtime implementation must use the current machine preflight as the
source of truth before creating assets or executing runtime commands.

The current preflight baseline is:

- macOS local developer machine on arm64.
- Java is present.
- Node.js and npm are present.
- Docker CLI is present.
- Docker daemon is available through Docker Desktop.
- `kubectl` is present, but Kubernetes is not required for this slice.
- MariaDB/MySQL client tooling is not present on the host.
- Default candidate ports `3306`, `8080`, `5173`, and `3000` had no detected
  listeners during preflight.

If any selected port becomes occupied, Docker becomes unavailable, or local
tooling state changes before runtime asset creation, implementation must stop,
rerun preflight, and either choose different local ports or ask the developer
to free the conflicting port before changing the machine.

## Selected Local Runtime Strategy

The first local runtime strategy is Docker Compose with enterprise-style
component ownership.

The local runtime component names are:

- `orca-frontend` for the frontend runtime component
- `orca-backend` for the backend runtime component
- `orca-db` for the MariaDB runtime component

The local compose structure has four compose entry points:

- one local aggregator compose for the full local runtime
- one component-owned compose for `orca-frontend`
- one component-owned compose for `orca-backend`
- one component-owned compose for `orca-db`

The aggregator compose exists for local developer convenience. The
component-owned compose files exist to practice enterprise-style service
ownership, where each runtime component can be reasoned about, started, and
evolved independently.

The selected strategy is intentionally smaller than Kubernetes because manual
login readiness only needs:

- one local frontend runtime component
- one local backend runtime component
- one local MariaDB runtime component
- backend external datasource configuration that can reach `orca-db` or a
  developer-selected existing MariaDB
- Flyway migration against MariaDB
- local-only login test data
- frontend-to-backend routing that preserves `ORCA_SESSION`

Kubernetes, ingress, production deployment topology, backup, restore,
monitoring, and TLS are outside this slice.

## Port Selection Boundary

Local ports are developer-selectable runtime values. Deployment documentation
may teach default candidates, but the implementation must let the developer
override them without editing committed source files.

The default local candidates are:

- MariaDB host port: `3306`
- backend HTTP port: `8080`
- React/Vite frontend port: `5173`
- alternate frontend or local web port: `3000`

The implementation must document which values must change together when a
developer selects a non-default port:

- Changing the MariaDB host port requires updating the Docker Compose port
  mapping and the backend datasource host port.
- Changing the backend HTTP port requires updating the backend runtime server
  port and any frontend API base URL or proxy target.
- Changing the frontend port requires updating the frontend dev server
  runtime value and any manual test URL that points to the frontend.
- Changing any port requires rerunning the port-conflict check before runtime
  execution.

Committed runtime assets may include default candidate values only when they
are non-secret and documented as overrideable. Environment-specific selected
port values belong in ignored local override files or runtime environment
variables.

## Runtime Asset Boundary

Future implementation may create local deployment assets only for this runtime
path:

- Aggregator Docker Compose asset for the full local runtime.
- Component-owned Docker Compose asset for `orca-frontend`.
- Component-owned Docker Compose asset for `orca-backend`.
- Component-owned Docker Compose asset for `orca-db`.
- Runtime build asset for `orca-frontend`, only to run the already-specified
  frontend behavior.
- Runtime build asset for `orca-backend`, only to run the already-specified
  backend behavior.
- Template or documented placeholder file for local-only environment values.
- Git-ignored local override file for actual runtime values.
- Local-only bootstrap mechanism for login test data.
- Documentation or script that verifies runtime readiness without printing
  secret values.

The committed local environment template is `.env.example`. It may contain
only placeholder secret values and documented non-secret defaults.

The ignored local runtime override is `.env.local`. It is created on the
developer machine and may contain real local passwords, selected ports,
runtime mode, component names, and local-only login test credential values.
It must not be committed.

Future implementation must not commit:

- real database passwords
- root or administrator database passwords
- generated password hashes
- Kubernetes Secrets
- Docker secrets containing real values
- session cookie values
- local environment-specific values
- real local test credentials

Committed examples must use placeholders only.

## Local Environment Value Boundary

Local runtime values are grouped by purpose:

- database connection values:
  - database host
  - database host port
  - database name
  - database user
  - database password
  - database root or administrator password
- database mode values:
  - Orca-owned compose database
  - existing Docker container database
  - existing external host/port database
  - existing database container name, when selected
- runtime component values:
  - frontend component name
  - backend component name
  - database component name
  - backend HTTP port
  - frontend HTTP port
  - backend-to-database host and port from inside the runtime network
  - frontend-to-backend API proxy target
- local-only login test values:
  - local test user id
  - unique local test login identifier
  - local test password

Committed documentation must explain which values are defaults, which values
are selected per developer machine, and which values are sensitive. Real
sensitive values belong only in ignored local runtime files or runtime
environment variables.

## Backend Runtime Configuration Boundary

The backend must connect to local MariaDB through external runtime
configuration.

The implementation must preserve these boundaries:

- Flyway owns schema creation.
- Application code must not mutate schema implicitly.
- Runtime credentials are provided outside committed source values.
- Local profile configuration may define non-secret defaults such as driver
  name, profile name, and default candidate ports, but actual secret values and
  environment-specific selected ports must be overrideable without editing
  committed source.
- The backend must start with the selected local profile before manual login
  testing is claimed ready.

## Flyway Schema Readiness

Before manual login testing is claimed ready, Flyway must have migrated the
local MariaDB schema and the database must include at least:

- `organization_groups`
- `group_members`
- `group_invitations`
- `invitation_index`
- `auth_registered_users`
- `auth_system_role_assignments`
- `auth_provisioning_verification_requests`
- `auth_login_credentials`
- `auth_authenticated_sessions`
- `auth_login_failure_audits`
- `reference_core_client_diagnostics`

Deployment must not create or rename these tables outside Flyway migrations.

## Local-only Login Test Data Boundary

Manual login success requires one local-only registered user and login
credential.

The local test credential mechanism must:

- be explicit and local-only
- use a unique login identifier because `auth_login_credentials.login_identifier`
  is the credential lookup key
- require the developer to provide the plaintext test password at runtime or
  through an ignored local file
- avoid printing the password
- avoid committing the password
- avoid committing the generated password hash
- avoid creating production seed data
- insert only the minimum auth-owned state required for login success
- update the same local credential when the same login identifier is bootstrapped
  again
- preserve auth-owned credential verification rules

Deployment may provide the mechanism that places local test state into the
local MariaDB database. Deployment must not define password verification rules,
session semantics, login failure response shape, or auth business behavior.

## Manual Verification Requirements

The local runtime is not ready unless the following checks can be performed
without exposing secret values:

- MariaDB runtime is available.
- Backend using the `local` profile can connect to MariaDB.
- Flyway has created the required tables, including
  `auth_login_credentials`.
- A local-only login test identity exists.
- `POST /api/auth/login` with the local valid credential returns HTTP `204`.
- The successful login response includes an `ORCA_SESSION` cookie.
- `POST /api/auth/login` with an invalid credential returns HTTP `401`.
- The failed login response body uses code `LOGIN_REJECTED`.
- The failed login response body includes `loginFailureReferenceId`.
- The failed login response does not include a session cookie.

Verification commands must not print passwords, password hashes, database root
passwords, session cookie values, or secret environment values.

## Scenarios

### Scenario: Runtime assets are created only after preflight

**Given**
- The developer wants local MariaDB login runtime support.
- The current machine preflight has been recorded.

**When**
- Deployment implementation prepares runtime assets.

**Then**
- The implementation uses the selected Docker Compose MariaDB strategy.
- The implementation stops if Docker is unavailable or a selected port is
  occupied.
- The implementation documents default ports and the files or environment
  values that must change when the developer selects different ports.
- The implementation separates the aggregator compose from component-owned
  compose files for `orca-frontend`, `orca-backend`, and `orca-db`.
- The implementation does not install or upgrade local tools.
- The implementation does not create Kubernetes assets.

### Scenario: Aggregator starts the full local runtime

**Given**
- The developer wants to run the full local runtime.
- Local runtime values are provided through ignored local configuration.

**When**
- The developer starts the local aggregator compose.

**Then**
- `orca-frontend`, `orca-backend`, and `orca-db` run as separate local runtime
  containers.
- The frontend reaches the backend through local runtime routing.
- The backend reaches MariaDB through runtime datasource configuration.
- The database remains owned by Flyway migrations.

### Scenario: Component compose preserves enterprise ownership practice

**Given**
- The developer wants to inspect or start one runtime component independently.

**When**
- The developer uses a component-owned compose file.

**Then**
- `orca-frontend`, `orca-backend`, and `orca-db` each have their own local
  compose definition.
- Component compose files do not redefine auth, frontend, reference-core, or
  organization business behavior.
- The aggregator compose coordinates the local runtime without becoming the
  owner of component behavior.

### Scenario: Backend starts against Flyway-managed MariaDB

**Given**
- The local MariaDB runtime is available.
- Backend datasource values are provided through local runtime configuration.

**When**
- The backend starts with the local profile.

**Then**
- The backend connects to MariaDB.
- Flyway creates or verifies the required schema.
- Application code does not create schema outside Flyway.

### Scenario: Local test credential enables successful login

**Given**
- Flyway has created the auth login tables.
- A local-only registered user and credential have been inserted through the
  approved local-only mechanism.

**When**
- The developer submits the local valid credential to `POST /api/auth/login`.

**Then**
- The existing auth behavior returns HTTP `204`.
- The response includes the opaque `ORCA_SESSION` cookie.
- The response does not expose user profile, role, organization, or session
  state details.

### Scenario: Invalid login remains indistinguishable

**Given**
- The local backend is running against MariaDB.

**When**
- The developer submits an invalid credential to `POST /api/auth/login`.

**Then**
- The existing auth and reference-core behavior returns HTTP `401`.
- The response code is `LOGIN_REJECTED`.
- The response includes an opaque `loginFailureReferenceId`.
- The response does not reveal whether the identifier or password caused the
  rejection.
- No `ORCA_SESSION` cookie is issued.

## Non-Goals

- Installing Docker, Java, Node.js, MariaDB, MySQL, or Kubernetes tooling.
- Upgrading local tools.
- Running Kubernetes.
- Creating Kubernetes manifests, ConfigMaps, or Secrets.
- Production deployment topology.
- Public MariaDB exposure.
- Replacing Flyway schema ownership.
- Changing auth credential verification or session behavior.
- Changing login failure audit behavior.
- Changing reference-core API error behavior.
- Changing organization behavior.
- Changing frontend business behavior.
