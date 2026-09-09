# Deployment 02 - Local MariaDB Login Runtime

Status: Approved / Implemented.

## Goal

Define the local MariaDB runtime path that lets a developer manually exercise
the already-specified password login success and failure behavior.

This slice authorized local runtime assets after the current machine preflight
was recorded and reviewed. It does not authorize installing tools, upgrading
tools, creating production deployment assets, or changing auth, reference-core,
organization, or frontend business behavior.

The implemented local runtime path provides component-owned Docker Compose
assets, backend local-profile MariaDB configuration, Flyway-backed schema
readiness, local-only login test data bootstrap, and manual login
success/failure verification without printing secret values.

This amendment selects the mode-aware login-runtime verification outcome from
`ORCA-DEPLOY-01`. It corrects the verification contract so a supported
`compose`, `container`, or `external` database mode is verified by the
capabilities that mode requires rather than by the presence of one fixed
three-container topology. Executable port-conflict preflight and loopback-only
MariaDB host exposure remain separate active outcomes and are not changed by
this amendment.

`deployment` remains a delivery/runtime support scope, not a bounded context.

## Workflow Traceability

- Workflows:
  - Authentication and Session
  - Operational Reliability
  - Frontend Reference Shell
- Workflow gap:
  - Login success and failure behavior is specified, but a developer needs a
    safe local MariaDB runtime path to exercise the database-backed behavior
    manually.
  - Local runtime credentials and login test data must stay out of Git.
  - Flyway must remain the schema owner before manual testing.
  - The current verification command requires the default frontend, backend,
    and database containers even when the selected supported database mode or
    a host-run component does not use those containers.
  - A developer therefore cannot obtain a truthful login-runtime readiness
    result for every supported local database mode.
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

The implemented local deployment assets are limited to this runtime path:

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

The implementation must not commit:

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

Deployment provides the local-only mechanism that places local test state into
the local MariaDB database. Deployment must not define password verification
rules, session semantics, login failure response shape, or auth business
behavior.

## Manual Verification Requirements

### Verification Outcome

The operator-visible outcome is one truthful local login-runtime readiness
result for the currently selected database mode.

Login-runtime readiness is capability-based. It requires:

- the selected database access path to be available
- the backend login HTTP boundary to be reachable at the configured local port
- the required Flyway-created tables to exist
- local-only login test state to exist
- valid login verification to return HTTP `204` with an `ORCA_SESSION` cookie
- invalid login verification to return HTTP `401 LOGIN_REJECTED` with an opaque
  `loginFailureReferenceId` and no session cookie

The verifier MUST NOT require a frontend runtime or fixed backend container to
prove this outcome. This spec permits a direct API client for manual login
verification, and the backend local profile may run on the host or in a
container. Full aggregator or frontend reachability is a different runtime
proof and MUST NOT be reported by the login-runtime verifier.

### Supported Database Mode Matrix

| Selected mode | Required database boundary | Docker required by database verification | Fixed containers forbidden as assumptions |
| --- | --- | --- | --- |
| `compose` | Orca-owned `orca-db` through the compose database adapter | yes | `orca-frontend`, `orca-backend` |
| `container` | `ORCA_LOCAL_DB_CONTAINER` through the existing-container adapter | yes | `orca-db`, `orca-frontend`, `orca-backend` |
| `external` | configured host and port through available MariaDB/MySQL client tooling | no | `orca-db`, `orca-frontend`, `orca-backend` |

Docker availability or permission MUST be checked only when the selected
database mode requires Docker. Backend readiness MUST be established through
the configured HTTP boundary, not inferred from a container name.

### Runtime Configuration Failure Set

The verification boundary consumes runtime strings from the ignored local
environment and from external command results. It MUST handle the following
failure classes before reporting readiness:

- absent: missing local environment file or required value
- null: a literal null marker used where a structured mode or port is required
- blank: empty or whitespace-only required value
- malformed: invalid database mode, invalid or out-of-range port, a runtime
  value that cannot be parsed or safely serialized for its structural role, or
  an unparseable HTTP result
- duplicate: more than one assignment for a verification-critical local
  environment key is ambiguous and MUST be rejected before that file is
  sourced; the verifier also MUST NOT infer readiness from multiple runtime
  processes or containers with similar names
- unsupported: database mode outside `compose`, `container`, and `external`
- untyped: mode, port, URL component, status, and command output remain runtime
  strings and MUST be validated before use
- stale: configuration points to a stopped container, unreachable host, wrong
  port, wrong database, incomplete schema, or different backend runtime
- unauthorized: Docker access, database authentication, or local login
  authentication is rejected
- unexpected: a required command is unavailable, exits unexpectedly, or
  returns output that cannot be safely classified

Verification-critical local environment keys are the selected database mode,
database host, database port, database name, database username, database
password, conditional existing-container name, backend port, local login
identifier, and local login password. Duplicate-key inspection MUST compare key
names without printing their values.

Every failure MUST stop verification with a non-zero result and a safe,
actionable message. Standard output and standard error MUST NOT contain local
passwords, password hashes, database administrator passwords, raw session
cookie values, or secret environment values.

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
- The verifier reports only checks it actually performed and MUST NOT claim
  that the frontend or all default containers are running when those checks are
  outside the selected verification outcome.

Verification commands must not print passwords, password hashes, database root
passwords, session cookie values, or secret environment values.

## Scenarios

### Scenario: Runtime assets are created only after preflight

**Given**
- The developer wants local MariaDB login runtime support.
- The current machine preflight has been recorded.

**When**
- Deployment implementation prepares or updates runtime assets.

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

### Scenario: Compose database mode verifies required capabilities

**Given**
- `ORCA_LOCAL_DB_MODE=compose`.
- The Orca-owned database container is available.
- The backend local profile is reachable through its configured HTTP port,
  whether the backend runs on the host or in a container.

**When**
- The developer verifies the local login runtime.

**Then**
- Verification uses the compose database adapter.
- Verification does not require the frontend container.
- Verification does not require a fixed backend container name.
- Verification proves schema readiness and the existing login success and
  failure contracts.

### Scenario: Existing-container database mode verifies selected database

**Given**
- `ORCA_LOCAL_DB_MODE=container`.
- `ORCA_LOCAL_DB_CONTAINER` names the available MariaDB container.
- The backend local profile is reachable through its configured HTTP port.

**When**
- The developer verifies the local login runtime.

**Then**
- Verification uses the selected existing database container.
- Verification does not require an Orca-owned `orca-db` container.
- Verification does not require frontend or backend containers.
- Verification proves schema readiness and the existing login success and
  failure contracts.

### Scenario: External database mode verifies without Docker dependency

**Given**
- `ORCA_LOCAL_DB_MODE=external`.
- MariaDB is reachable through the configured host and port.
- Supported MariaDB or MySQL client tooling is available on the host.
- The backend local profile is reachable through its configured HTTP port.

**When**
- The developer verifies the local login runtime.

**Then**
- Verification uses the configured external database boundary.
- Verification does not require Docker or any fixed container.
- Verification proves schema readiness and the existing login success and
  failure contracts.

### Scenario: Stale or unsupported runtime state fails safely

**Given**
- A required runtime value is absent, null, blank, malformed, duplicated,
  unsupported, or untyped; or
- The selected runtime state is stale, unauthorized, or unexpectedly
  unavailable.

**When**
- The developer verifies the local login runtime.

**Then**
- Verification exits with a non-zero result.
- The message identifies the failed capability without claiming unrelated
  component state.
- The message does not expose passwords, hashes, secret environment values, or
  raw session cookie values.

## Acceptance Criteria

- The login-runtime verifier MUST produce one truthful readiness result for the
  selected `compose`, `container`, or `external` database mode.
- The verifier MUST use the database access boundary selected by
  `ORCA_LOCAL_DB_MODE`.
- `compose` mode MUST require only the Orca-owned database container from the
  fixed compose topology.
- `container` mode MUST use `ORCA_LOCAL_DB_CONTAINER` and MUST NOT require
  `orca-db`.
- `external` mode MUST use the configured host/client boundary and MUST NOT
  require Docker.
- Every mode MUST verify backend reachability through the configured HTTP
  boundary rather than through a fixed backend container name.
- Login-runtime verification MUST NOT require frontend reachability.
- Every mode MUST verify the required Flyway schema and existing auth and
  reference-core login contracts.
- The verifier MUST safely serialize non-blank local login values into the HTTP
  request without imposing a deployment-owned credential policy.
- Runtime/public failure classes identified by this amendment MUST fail with a
  non-zero result before readiness is reported.
- Duplicate assignments for verification-critical local environment keys MUST
  be rejected before the local environment file is sourced.
- Verification output MUST NOT expose passwords, password hashes, database
  administrator passwords, secret environment values, or raw session cookie
  values.
- Verification MUST NOT claim that an unverified frontend, container, or full
  aggregator topology is ready.
- This amendment MUST NOT change auth, reference-core, frontend, organization,
  credential encoding, port-conflict, or database-exposure behavior.

## Error Cases

- Missing local environment or required value -> reject before verification.
- Literal null, blank, malformed, unsupported, or untyped mode/port value ->
  reject before command execution.
- Duplicate verification-critical local environment assignment -> reject
  before sourcing the local environment file.
- Docker unavailable or unauthorized in `compose` or `container` mode -> fail
  the selected database capability without printing secrets.
- Docker unavailable in `external` mode -> do not fail solely because Docker is
  unavailable.
- Selected compose or existing database container unavailable -> fail without
  requiring unrelated containers.
- External database client missing or database unreachable -> fail with a safe
  mode-specific message.
- Backend HTTP boundary unreachable or unparseable -> fail without inferring
  container state.
- Required Flyway table missing -> fail schema readiness.
- Local test credential not accepted as the expected valid login -> fail login
  readiness without exposing the credential.
- Login success lacks HTTP `204` or `ORCA_SESSION` -> fail.
- Login rejection lacks `401 LOGIN_REJECTED` or
  `loginFailureReferenceId`, or includes a session cookie -> fail.
- Verification command fails unexpectedly -> return non-zero with safe output.

## Verification Mapping

| Normative outcome | Verification |
| --- | --- |
| compose mode checks only its required database topology | automated shell contract test `verifies compose login runtime by required capabilities` |
| existing-container mode uses the selected container | automated shell contract test `verifies selected existing database container mode` |
| external mode has no Docker requirement | automated shell contract test `verifies external database mode without Docker` |
| backend availability does not depend on a container name | automated shell contract test `verifies backend availability through HTTP` |
| runtime values are validated before external commands | automated shell contract tests `rejects untyped database port before command execution`, `rejects blank required runtime values before command execution`, `rejects unsupported database mode before command execution`, `rejects null database mode before command execution`, `rejects absent local environment before command execution`, and `rejects login values with control characters before command execution` |
| duplicate or ambiguous runtime identity does not imply readiness | automated shell contract tests `rejects duplicate verification-critical environment keys before sourcing` and `rejects a similar database container name` |
| stale, unauthorized, malformed, and unexpected boundaries fail safely | automated shell contract tests `fails safely when Docker access is unauthorized`, `fails when the selected external database is stale or unreachable`, `fails when the backend HTTP boundary is unexpectedly unavailable`, and `rejects an unparseable backend HTTP status` |
| required Flyway schema is present | automated shell contract test `fails when a required Flyway table is missing`, plus the supported-mode success tests |
| login request values are safely serialized | automated shell contract tests `safely serializes local login values` and `rejects login values with control characters before command execution` |
| successful login preserves the auth-owned status and cookie contract | supported-mode success tests plus `fails when the valid local credential is rejected` and `fails when successful login omits the session cookie` |
| rejected login preserves the auth/reference-core status, code, reference, and no-cookie contract | supported-mode success tests plus `fails when rejected login returns an unexpected status`, `fails when rejected login omits LOGIN_REJECTED`, `fails when rejected login omits the failure reference`, and `fails when rejected login includes a session cookie` |
| sensitive values never appear in output | shared captured-output assertion applied to every success and failure case in `deploy/local/test/verify-login-runtime.test.sh` |
| all supported modes have executable evidence | run `sh deploy/local/test/verify-login-runtime.test.sh` for isolated `compose`, `container`, and `external` contract evidence; run `deploy/local/bin/verify-login-runtime.sh` with the selected ignored local environment for reproducible runtime proof |

## Affected and Deferred Documents

- The matching `deployment-02` DDD note derives capability-based verification
  and shell-contract test placement from this amendment.
- `docs/drafts/slice-planning-handoff.md` records that only the mode-aware
  verification outcome is included now.
- Executable port-conflict preflight and loopback-only MariaDB exposure remain
  active under `ORCA-DEPLOY-01` and are not superseded.
- Credential encoding ownership remains active under `ORCA-SECURITY-01` and is
  not changed by this amendment.

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
- Requiring frontend reachability for direct API login-runtime verification.
- Defining full-aggregator readiness.
- Implementing executable port-conflict preflight.
- Changing MariaDB host binding or loopback-only exposure.
- Changing credential encoding, hashing, or migration ownership.
