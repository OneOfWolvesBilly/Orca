# Spec 12 - Embedded Auth and Actor-context Integration

Status: Approved / Implemented.

## Slice Intake

Slice candidate: `auth-12` embedded auth and actor-context integration.

Workflow:

- Authentication and Session.
- Product-agnostic embedded Core consumption.

Workflow gap:

- Existing Orca HTTP commands can establish current user context from an
  auth-owned server-side session.
- An independently structured consumer host cannot yet declare one protected
  command and receive exactly one authenticated actor through an explicit,
  supported Orca public boundary.
- Existing protected path registration is internal Orca wiring and does not
  prove that a future product can consume the boundary through a normal build
  dependency.

Primary actor:

- A registered user invoking a product-neutral protected fixture command after
  completing the existing Orca password login.

Supporting actor:

- An application developer embedding Orca Core in the same process.

Successful outcome:

- The consumer fixture declares one protected command through the supported
  Orca boundary.
- Orca resolves the existing `ORCA_SESSION` cookie against auth-owned
  server-side session state.
- The fixture command receives exactly one authenticated actor id and no raw
  session value or auth persistence state.

Failure flows:

- Missing, blank, malformed, unknown, expired, invalid, revoked, or ambiguous
  session-cookie input rejects the fixture command as unauthenticated.
- `X-User-Id` does not establish the fixture actor context.
- Rejection occurs before fixture command behavior executes.
- The same session cannot execute the fixture command after logout.

Existing supported slices:

- `auth-01` current user context.
- `auth-03` request-scoped current user context access.
- `auth-08` password login with server-side session.
- `auth-09` protected HTTP session context.
- `auth-11` logout and session revocation.
- `reference-core-01` stable API error contract.

Planned predecessor slices:

- None.

Unknowns:

- Future artifact publication coordinates and repository.
- Whether later consumers need non-Spring or separately deployed integration.
- Whether a future current-user profile endpoint is required.

Non-goals:

- SSO, OAuth, OIDC, cross-product sessions, or hosted login.
- Session renewal or sliding expiration.
- New account lifecycle, role, or permission behavior.
- Frontend delivery or customer branding.
- Audit emission, structured logging, or correlation.
- Product-specific domain behavior.

Decision: enter SDD.

## Goal

Allow a same-process Java / Spring Boot consumer to use existing Orca login and
server-side session behavior and declare one product-neutral protected command
that receives exactly one authenticated actor id through a supported public
Orca boundary.

This slice proves embedded consumption with a Minimal Consumer Fixture. It does
not change password verification, session creation, session lifetime, logout,
or bounded-context authorization behavior.

## Scope Ownership

This is an `auth` integration slice because auth owns session resolution and
authenticated actor establishment.

The Minimal Consumer Fixture is a verification host only. It owns no product
domain and must not become a new bounded context.

Reference-core remains authoritative for the stable API error contract. The
fixture does not redefine unauthenticated error semantics.

## Contract Terms

- Embedded Auth Boundary
  The supported public integration surface through which a same-process Spring
  Boot consumer enables Orca login, logout, and protected actor resolution.

- Protected Consumer Command
  A consumer-owned HTTP command explicitly declared as requiring an
  authenticated actor before its handler executes.

- Authenticated Actor
  The product-neutral public value supplied to a protected consumer command.
  It contains exactly one non-blank actor id resolved from auth-owned session
  state.

- Minimal Consumer Fixture
  An independently structured verification host that depends on Orca through a
  normal build dependency and exposes one no-domain protected command.

## Public Integration Contract

The supported public boundary must allow a Spring Boot consumer to:

1. enable embedded Orca auth through an explicit public integration entry
   point;
2. declare a command handler as protected without adding its route to an Orca
   internal path list;
3. receive one authenticated actor value as handler input after Orca has
   resolved the presented session;
4. use the existing Orca login and logout HTTP behavior in the same process.

The public authenticated actor contains only:

```text
actorId
```

The actor id must be non-blank. The public actor boundary must not expose:

- raw `ORCA_SESSION` value or raw session id
- session repository or auth-owned database state
- session expiration or revocation details
- credential state or password data
- registered-user, role, organization, personnel, or profile objects
- an Orca internal resolver, interceptor, or request attribute

Consumers must not be required to import Orca internal infrastructure packages
to use the supported boundary.

## Minimal Consumer Fixture Contract

The fixture exposes one protected command:

```text
POST /api/fixture/actor-context-check
```

The request body is an empty JSON object. The command returns:

```text
204 No Content
```

The fixture handler receives the authenticated actor through the public Orca
boundary and invokes one product-neutral fixture application operation with
that actor id.

The fixture command has no project, issue, sprint, ticket, asset, risk,
control, evidence, alarm, organization, CRM, Jira, Lobster, ISO, or other
product meaning.

## Scenarios

### Scenario: Embedded consumer receives one actor after Orca login

**Given**

- The Minimal Consumer Fixture enables embedded Orca auth.
- A registered user completes the existing Orca password login.
- The browser presents the resulting `ORCA_SESSION` cookie.

**When**

- The browser invokes the protected fixture command.

**Then**

- Orca resolves the session from auth-owned server-side state.
- Orca establishes exactly one authenticated actor.
- The fixture handler receives the actor id through the public Orca boundary.
- The fixture operation executes exactly once.
- The fixture does not read or parse the session cookie.

### Scenario: Session rejection prevents fixture execution

**Given**

- The protected fixture command receives no establishable auth session.

**When**

- Orca evaluates the command before fixture handler execution.

**Then**

- The request is rejected with `401 UNAUTHENTICATED` under
  `reference-core-01`.
- The fixture operation does not execute.
- The response does not identify the failed session condition.

### Scenario: Multiple session cookies are rejected as ambiguous

**Given**

- A protected command request presents more than one `ORCA_SESSION` cookie.

**When**

- Orca evaluates the protected command boundary.

**Then**

- Orca does not choose one cookie value.
- No actor context is established.
- The command is rejected with the same `401 UNAUTHENTICATED` response used by
  other session rejection conditions.
- Downstream command behavior does not execute.

### Scenario: Demo header cannot establish a fixture actor

**Given**

- A request presents `X-User-Id` without one establishable `ORCA_SESSION`
  cookie.

**When**

- The request invokes the protected fixture command.

**Then**

- `X-User-Id` is ignored as an auth source.
- No actor context is established.
- The fixture operation does not execute.

### Scenario: Logout invalidates fixture command access

**Given**

- A session previously executed the protected fixture command.
- The client completes the existing Orca logout behavior.

**When**

- The same session value is presented to the protected fixture command.

**Then**

- The command is rejected as unauthenticated under `auth-09` and `auth-11`.
- The fixture operation does not execute.

## Acceptance Criteria

- Orca MUST expose one explicit supported public entry point for embedded auth
  integration.
- A consumer MUST be able to declare a protected command without importing or
  modifying an Orca internal protected-path list.
- The protected boundary MUST resolve actor context before consumer handler
  execution.
- The consumer handler MUST receive exactly one non-blank authenticated actor
  id for an establishable session.
- The actor id MUST come only from auth-owned server-side session state.
- The consumer MUST NOT parse `ORCA_SESSION` or query auth-owned tables.
- The consumer MUST NOT import Orca internal infrastructure to use the public
  boundary.
- Missing, blank, malformed, unknown, expired, invalid, revoked, or multiple
  `ORCA_SESSION` cookies MUST be rejected as `401 UNAUTHENTICATED` without
  identifying the condition.
- Orca MUST NOT select a first or last cookie when multiple `ORCA_SESSION`
  cookies are present.
- `X-User-Id` MUST NOT establish protected consumer actor context.
- Rejected requests MUST NOT execute the consumer command.
- The fixture MUST consume Orca through a normal build dependency.
- A consumer contract test MUST prove login, actor resolution, logout, and
  post-logout rejection through the public boundary.
- The fixture MUST contain no product-specific domain behavior.
- This slice MUST preserve auth-08 login, auth-09 session rejection, auth-11
  logout, and reference-core-01 error behavior.
- This slice MUST NOT add or change organization behavior.

## Invariants

- One protected operation has at most one authenticated actor.
- The authenticated actor id remains unchanged for the full operation.
- Authenticated actor identity is resolved only from auth-owned server-side
  session state.
- A consumer receives no raw session value through the actor boundary.
- Ambiguous session-cookie input establishes no actor.
- Public integration contracts do not expose Orca internal infrastructure as a
  supported dependency.

## Error Cases

- Embedded auth is not enabled for a declared protected command -> application
  integration must fail visibly rather than execute unauthenticated behavior.
- Missing `ORCA_SESSION` cookie -> `401 UNAUTHENTICATED`.
- Blank session value -> `401 UNAUTHENTICATED`.
- Malformed or unacceptable session value -> `401 UNAUTHENTICATED`.
- Unknown session -> `401 UNAUTHENTICATED`.
- Expired session -> `401 UNAUTHENTICATED`.
- Invalid session -> `401 UNAUTHENTICATED`.
- Revoked session -> `401 UNAUTHENTICATED`.
- More than one `ORCA_SESSION` cookie -> `401 UNAUTHENTICATED`.
- Only `X-User-Id` is presented -> `401 UNAUTHENTICATED`.
- Non-empty fixture request body -> `400 VALIDATION_ERROR` before fixture
  operation execution.

## Sensitive Data Boundary

The public actor contract, fixture, responses, logs, and tests must not expose
or persist:

- password or credential secret
- raw session cookie value or raw session id
- request headers
- session expiration or revocation internals
- credential or registered-user state
- system role, organization role, membership, or permission details
- personnel, profile, name, email, or department data
- raw request or response body
- unrestricted exception or stack trace

Tests may use opaque test session values only as HTTP inputs. They must not make
those values part of the public actor contract or response.

## Non-Goals

- Session renewal or sliding expiration.
- Refresh tokens, access tokens, remember-me, or cross-product session sharing.
- OAuth, SSO, OIDC, MFA, hosted login, or redirect/callback behavior.
- Account disable, suspend, recovery, or credential setup.
- Current-user profile endpoint.
- New authorization roles or permission rules.
- Product-specific protected commands.
- React, Vue, Angular, branding, or frontend navigation.
- Audit event emission or recorder integration.
- Structured logging or correlation.
- Separately deployed Orca service.
- Production artifact publication or repository selection.
- New database schema or Flyway migration.
