# DDD - Deployment 02 - Local MariaDB Login Runtime

Status: Derived from
`docs/specs/deployment/02-local-mariadb-login-runtime.md`.

## Purpose

This note derives the model boundary and rule placement for `deployment-02`.
It explains how local MariaDB runtime support should be implemented without
turning deployment into an owner of auth, organization, reference-core, or
frontend behavior.

It must not introduce behavior beyond the deployment spec.

## Scope Classification

`deployment` remains a delivery/runtime support scope.

This slice is not a bounded context and introduces no domain aggregate,
repository, domain service, or application use case. It describes runtime
wiring and operational gates that let already-specified behavior run locally.

## Model Boundary

No domain model is introduced.

The useful support-scope terms are:

- Local MariaDB Runtime
- Docker Compose Runtime Asset
- Backend Local Profile Runtime
- Runtime Credential Placeholder
- Ignored Local Runtime Value
- Selected Local Port
- Default Candidate Port
- Local-only Login Test Data
- Flyway Schema Readiness
- Manual Login Verification
- Stop Condition

These terms may appear in deployment files, documentation, scripts, and static
checks. They must not become auth, organization, reference-core, or frontend
domain concepts.

## Rule Placement

### Deployment Support Rules

Deployment owns rules about:

- selecting Docker Compose as the first local MariaDB runtime strategy
- documenting default candidate ports while allowing developer-selected
  overrides
- stopping when Docker is unavailable or selected ports are occupied
- keeping Kubernetes outside this slice
- separating committed placeholders from ignored local runtime values
- preventing real passwords, password hashes, Secrets, and environment-specific
  values from entering Git
- verifying that Flyway created the required schema
- documenting how to manually verify login success and login failure
- preserving browser cookie behavior when frontend routing is used later

These rules belong in deployment documentation and deployment assets.

### Auth Rules

Auth remains authoritative for:

- credential verification
- registered user identity state
- server-side session creation
- `ORCA_SESSION`
- successful login response behavior
- rejected login behavior
- login failure audit records
- `loginFailureReferenceId`

Deployment may create local runtime state that existing auth behavior consumes.
It must not define password hashing semantics, credential verification rules,
session rules, or login failure response shape.

### Reference-core Rules

Reference-core remains authoritative for:

- stable API error response shape
- `LOGIN_REJECTED`
- safe client-visible failure responses
- client diagnostics behavior

Deployment may verify that the runtime returns the expected error code, but it
must not redefine the error contract.

### Organization Rules

Organization remains authoritative for organization tables and behavior.

Deployment may verify that Flyway created organization tables needed for backend
startup. It must not create organization commands, roles, workflows, or
business rules.

### Frontend Rules

Frontend remains authoritative for login shell and client failure behavior.

Deployment may later provide local routing that preserves cookie behavior. It
must not reimplement backend rules in the frontend.

## Test Layer Placement

This slice primarily authorizes deployment assets and manual runtime
verification, not domain or application code.

Future implementation should use:

- documentation review for the spec and DDD files
- static checks for committed placeholder files and ignored local value files
- Git checks that real secrets, generated password hashes, and environment
  values are not staged
- runtime verification commands for Docker, backend local profile, Flyway
  tables, and login success/failure behavior

No domain test is required because no domain invariant is introduced.

Backend integration tests may be added only if the implementation changes
backend runtime configuration or persistence integration. Those tests must
verify wiring, not redefine auth or reference-core behavior.

## Design Decisions

### Decision: Use Docker Compose before Kubernetes

Manual login readiness needs one MariaDB runtime and backend external
configuration. Docker Compose is the smallest local runtime shape for that
need. Kubernetes is deferred because ingress, service exposure, Secrets, and
cluster lifecycle would add operational choices that are not required for the
first local login test.

### Decision: Run backend on the host with the local profile

Keeping the backend outside the first Compose runtime reduces the number of
runtime components that must be containerized before login can be practiced.
The backend still uses external datasource values and Flyway remains the schema
owner.

### Decision: Treat ports as selectable runtime values

Default ports make local setup teachable, but they are not behavior. Developers
may already have services on common ports, so the runtime assets must document
which values move together and allow selected ports to live in ignored local
overrides or runtime environment variables.

### Decision: Treat local test data as runtime support

The local test identity exists only to exercise already-specified login
behavior. It is not product seed data and must not be committed as real
credentials or generated password hashes.

### Decision: Verify behavior through HTTP without duplicating rules

Manual verification calls `POST /api/auth/login` because that is the
auth-owned public boundary for login. Deployment verifies that the runtime can
exercise success and failure, while auth and reference-core remain authoritative
for the response semantics.

## Risk Notes

- Committing a generated password hash would make local-only runtime data part
  of repository history.
- Reusing committed datasource passwords as real local secrets would blur the
  source/runtime boundary.
- Treating default ports as mandatory could force users to stop unrelated local
  services.
- Starting runtime assets before rechecking selected ports could conflict with
  existing user services.
- Creating Kubernetes assets in this slice would jump beyond the selected
  runtime strategy.
- Verifying login by inspecting internal auth state alone would miss the
  cookie and stable error contract behavior that manual testing needs.
- Printing session cookie values or passwords in verification output would
  violate the secret boundary.

## Non-Goals Confirmed

- No domain aggregate.
- No application use case.
- No auth rule change.
- No reference-core rule change.
- No organization rule change.
- No frontend rule change.
- No production deployment baseline.
- No Kubernetes runtime.
- No installation or upgrade workflow.
