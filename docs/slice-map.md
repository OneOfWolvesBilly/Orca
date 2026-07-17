# Slice Map

This document is a derived index.
It helps contributors find behavior slices by bounded context.
It does not introduce behavior.

Authoritative behavior remains in `docs/specs/<bounded-context>/*` and approved
`docs/specs/<support-scope>/*` support scopes.

---

## Naming Rules

Slice ids are scoped by bounded context:

```text
<bounded-context>-<NN>
```

Spec and DDD note files use matching names:

```text
docs/specs/<bounded-context>/<NN>-<behavior-or-integration>.md
docs/ddd/<bounded-context>/<NN>-<behavior-or-integration>.md
```

Approved support scopes use the same file shape:

```text
docs/specs/<support-scope>/<NN>-<behavior-or-integration>.md
docs/ddd/<support-scope>/<NN>-<behavior-or-integration>.md
```

The Spec column links to authoritative behavior definitions. The DDD column
links to derived notes that explain model decisions and must not introduce
behavior.

Future slices must also be traceable to a workflow gap or existing workflow
protection need described in `docs/product/*`. A technical category, framework
feature, or cache/logging idea is not enough by itself to create a slice.

Frontend work is normally a delivery slice inside the bounded context it exposes.
For example, an organization UI command console should be tracked as an
organization slice, not as a separate frontend bounded context.

Cross-cutting support behavior that protects multiple bounded contexts may use
the `reference-core` support scope after passing slice intake. Reference-core
slices do not own or redefine bounded-context business rules.

Deployment is a delivery/runtime support scope, not a bounded context. It may
describe runtime wiring, configuration boundaries, and operational enablement
for already-specified behavior after passing slice intake. Deployment slices
must not own, redefine, or rewrite auth, organization, reference-core, or
frontend business rules.

---

## Organization

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `organization-01` | [Create Group](specs/organization/01-create-group.md) | [DDD](ddd/organization/01-create-group.md) | Done |
| `organization-02` | [Invite Member](specs/organization/02-invite-member.md) | [DDD](ddd/organization/02-invite-member.md) | Done |
| `organization-03` | [Accept Invitation](specs/organization/03-accept-invitation.md) | [DDD](ddd/organization/03-accept-invitation.md) | Done |
| `organization-04` | [Reject Invitation](specs/organization/04-reject-invitation.md) | [DDD](ddd/organization/04-reject-invitation.md) | Done |
| `organization-05` | [Revoke Invitation](specs/organization/05-revoke-invitation.md) | [DDD](ddd/organization/05-revoke-invitation.md) | Done |
| `organization-06` | [Application Integration](specs/organization/06-application-integration.md) | [DDD](ddd/organization/06-application-integration.md) | Done |
| `organization-07` | [Persistence Integration](specs/organization/07-persistence-integration.md) | [DDD](ddd/organization/07-persistence-integration.md) | Done |
| `organization-08` | [Web API Integration](specs/organization/08-web-api-integration.md) | [DDD](ddd/organization/08-web-api-integration.md) | Done |

## Auth

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `auth-01` | [Establish Authenticated User Context](specs/auth/01-establish-authenticated-user-context.md) | [DDD](ddd/auth/01-establish-authenticated-user-context.md) | Done |
| `auth-02` | [HTTP Current User Context Integration](specs/auth/02-http-current-user-context-integration.md) | [DDD](ddd/auth/02-http-current-user-context-integration.md) | Done |
| `auth-03` | [HTTP Request Current User Context Access](specs/auth/03-http-request-current-user-context-access.md) | [DDD](ddd/auth/03-http-request-current-user-context-access.md) | Done |
| `auth-04` | [Protected HTTP Command Boundary Mapping](specs/auth/04-protected-http-command-boundary-mapping.md) | [DDD](ddd/auth/04-protected-http-command-boundary-mapping.md) | Done |
| `auth-05` | [Registered User Identity Integration](specs/auth/05-registered-user-identity-integration.md) | [DDD](ddd/auth/05-registered-user-identity-integration.md) | Done |
| `auth-06` | [Admin-managed User Provisioning](specs/auth/06-admin-managed-user-provisioning.md) | [DDD](ddd/auth/06-admin-managed-user-provisioning.md) | Done |
| `auth-07` | [Provisioning Identity Verification](specs/auth/07-provisioning-identity-verification.md) | [DDD](ddd/auth/07-provisioning-identity-verification.md) | Done |
| `auth-08` | [Password Login with Server-side Session](specs/auth/08-password-login-with-server-side-session.md) | [DDD](ddd/auth/08-password-login-with-server-side-session.md) | Done |
| `auth-09` | [Protected HTTP Session Context](specs/auth/09-protected-http-session-context.md) | [DDD](ddd/auth/09-protected-http-session-context.md) | Done |
| `auth-10` | [Login Failure Audit](specs/auth/10-login-failure-audit.md) | [DDD](ddd/auth/10-login-failure-audit.md) | Done |
| `auth-11` | [Logout and Session Revocation](specs/auth/11-logout-session-revocation.md) | [DDD](ddd/auth/11-logout-session-revocation.md) | Done |
| `auth-12` | [Embedded Auth and Actor-context Integration](specs/auth/12-embedded-auth-actor-context-integration.md) | [DDD](ddd/auth/12-embedded-auth-actor-context-integration.md) | Done |

## Reference Core

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `reference-core-01` | [Stable API Error Contract](specs/reference-core/01-stable-api-error-contract.md) | [DDD](ddd/reference-core/01-stable-api-error-contract.md) | Done |
| `reference-core-02` | [Client Diagnostics Foundation](specs/reference-core/02-client-diagnostics-foundation.md) | [DDD](ddd/reference-core/02-client-diagnostics-foundation.md) | Done |
| `reference-core-03` | [Reusable Audit Recording Boundary](specs/reference-core/03-reusable-audit-recording-boundary.md) | [DDD](ddd/reference-core/03-reusable-audit-recording-boundary.md) | Done |

## Frontend

Frontend slices are delivery slices identified explicitly by the `frontend`
prefix. They consume bounded-context APIs without taking ownership of backend
domain or application behavior.

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `frontend-01` | [Frontend Login Result Shell](specs/frontend/01-login-result-shell.md) | [DDD](ddd/frontend/01-login-result-shell.md) | React Done / Vue and Angular Planned |
| `frontend-02` | [Client Failure Observability](specs/frontend/02-client-failure-observability.md) | [DDD](ddd/frontend/02-client-failure-observability.md) | React Done / Vue and Angular Planned |

## Deployment

Deployment slices are delivery/runtime support slices identified explicitly by
the `deployment` prefix. They are not a bounded context and must not create
Kubernetes manifests, Secrets, Docker runtime changes, or production deployment
assumptions until an authoritative deployment spec and derived DDD note exist.
Local environment preflight is an operational gate inside deployment support
work, not a standalone Orca application behavior slice.
Manual login runtime readiness requires backend runtime configuration, MariaDB
availability, Flyway-migrated schema, local-only login test data, and
frontend/backend routing. Frontend completion alone does not imply local manual
login readiness.

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `deployment-01` | [Local Runtime Build Plan](specs/deployment/01-secure-local-runtime-boundary.md) | [DDD](ddd/deployment/01-secure-local-runtime-boundary.md) | Approved support gate / no runtime assets |
| `deployment-02` | [Local MariaDB Login Runtime](specs/deployment/02-local-mariadb-login-runtime.md) | [DDD](ddd/deployment/02-local-mariadb-login-runtime.md) | Implemented / local manual verification ready |

## Planned Contexts

No additional domain bounded contexts beyond `organization` and `auth` are
authoritative yet. `reference-core`, `frontend`, and `deployment` are support
scopes, not domain bounded contexts.
Introduce `issue` or other bounded contexts only by adding their first
authoritative spec under `docs/specs/<bounded-context>/`.
