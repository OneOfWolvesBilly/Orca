# Spec 12 - Embedded Auth and Actor-context Integration

Status: Approved / Implemented.

## Repair Intake and Single Visible Outcome

Planning path: `continue current capability`.

This repair completes `auth-12`; it does not create `auth-14`.

Single actor-visible outcome:

- when a same-process Spring consumer declares `@OrcaProtectedCommand`, Orca
  establishes exactly one authenticated actor before the handler executes;
- if embedded auth enablement or the declared HTTP method is invalid, the
  application fails startup;
- no protected declaration is silently ignored or allowed to execute without
  authenticated actor context.

Overlapping planning dispositions:

- `ORCA-REPAIR-01`: included in this repair;
- `ORCA-DOC-01`: included only for the protected-method contract and the
  supersession relationship among `auth-04`, `auth-09`, organization-08, and
  this public embedded declaration contract;
- `ORCA-ARCH-01`, `ORCA-DELIVERY-01`, and `ORCA-SECURITY-01`: deferred and
  remain outside this slice.

The accepted startup and method decisions close gaps in the already-approved
public boundary. They do not add a new workflow or actor outcome.

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

## Dependency Ownership

| Required mechanism | Owner | Authoritative predecessor | Allowed boundary | Completion evidence |
| --- | --- | --- | --- | --- |
| authenticated actor invariants | auth | `auth-01` | established internal current-user context | auth domain and unit tests |
| request-scoped actor access | auth | `auth-03` | Orca request argument resolution | auth web tests |
| login and opaque session creation | auth | `auth-08` | `POST /api/auth/login` and `ORCA_SESSION` | login web integration tests |
| protected session resolution | auth | `auth-09` | auth-owned session-resolution application boundary | session application/web tests |
| logout and revocation | auth | `auth-11` | `POST /api/auth/logout` | logout web integration tests |
| embedded declaration and actor value | auth | `auth-12` | `@EnableOrcaEmbeddedAuth`, `@OrcaProtectedCommand`, and `AuthenticatedActor` | public API, web, and consumer contract tests |
| stable unauthenticated response | reference-core | `reference-core-01` | `401 UNAUTHENTICATED` API error contract | reference-core web tests |

Every predecessor is implemented on the current repository baseline. No
consumer may bypass these boundaries by reading auth persistence, parsing a
cookie, or importing an internal interceptor or resolver.

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

## Relationship to Existing Orca Protected Paths

`auth-04` and `auth-09` remain authoritative for the existing five
organization `POST` routes. Their fixed mapping records which existing Orca
organization commands require auth; it is not the public extension mechanism
for embedded consumers.

`auth-12` is authoritative for consumer-owned declarations. A consumer uses
`@OrcaProtectedCommand` and does not add its route to the `auth-04` / `auth-09`
path list. Both forms use the same auth-owned session resolution and
request-scoped actor establishment. Organization-08 continues to define its
five command routes and organization behavior, while `auth-09` supersedes its
original demo `X-User-Id` actor transport.

No document may treat `X-User-Id`, the fixed organization path list, or the
absence of `POST` as permission to bypass an `@OrcaProtectedCommand`
declaration.

## Startup Validation and Protected Method Matrix

Startup validation inspects every Spring handler declaration marked directly
or at controller type level with `@OrcaProtectedCommand`.

| Declaration state | Startup result | Request-time result |
| --- | --- | --- |
| no protected declaration | startup succeeds without requiring embedded auth | no behavior added by this slice |
| protected declaration and embedded auth enabled | startup succeeds only for `POST`, `PUT`, `PATCH`, or `DELETE` | actor context is established before handler execution |
| protected declaration without `@EnableOrcaEmbeddedAuth` | startup fails with an explicit integration error | handler is unavailable |
| protected `GET`, `HEAD`, or `OPTIONS` declaration | startup fails with an explicit unsupported-method error | handler is unavailable |
| protected declaration with no explicit method or any other method | startup fails closed as unsupported | handler is unavailable |

Method validation applies to every mapped method on the declaration. A mapping
that contains both supported and unsupported methods fails startup. Framework
fallback handling for `HEAD` or `OPTIONS` does not turn those methods into
protected commands.

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

### Scenario: Missing embedded enablement fails startup

**Given**

- A Spring consumer has at least one `@OrcaProtectedCommand` declaration.
- The consumer has not enabled Orca through `@EnableOrcaEmbeddedAuth`.

**When**

- Spring validates handler mappings during application startup.

**Then**

- Application startup fails with an explicit Orca integration error.
- The error identifies missing embedded auth enablement without exposing
  session, credential, or actor data.
- No protected handler becomes available for request execution.

### Scenario: Supported protected command methods establish an actor

**Given**

- Embedded auth is enabled.
- A protected handler is explicitly mapped to `POST`, `PUT`, `PATCH`, or
  `DELETE`.
- The request presents exactly one establishable `ORCA_SESSION` cookie.

**When**

- The request invokes that handler.

**Then**

- Orca resolves and stores authenticated actor context before the handler.
- The handler receives the session-resolved actor rather than any
  attacker-controlled request parameter.
- The handler executes exactly once.

### Scenario: Read-like or unspecified protected methods fail startup

**Given**

- A handler is marked `@OrcaProtectedCommand`.
- Its mapping includes `GET`, `HEAD`, or `OPTIONS`, has no explicit HTTP
  method, or includes another unsupported method.

**When**

- Spring validates handler mappings during application startup.

**Then**

- Application startup fails with an explicit unsupported-method integration
  error.
- No request can reach the declared handler.

### Scenario: Rejected command cannot be forced to execute with actor input

**Given**

- A caller supplies an actor-like query, path, header, or body parameter.
- The request has no establishable session or has an ambiguous session.

**When**

- The caller invokes a protected command.

**Then**

- Orca ignores attacker-controlled actor input as an authentication source.
- The request is rejected as `401 UNAUTHENTICATED`.
- The protected handler and downstream operation execute zero times.

## Acceptance Criteria

- Orca MUST expose one explicit supported public entry point for embedded auth
  integration.
- Any `@OrcaProtectedCommand` declaration without
  `@EnableOrcaEmbeddedAuth` MUST fail application startup.
- Protected `POST`, `PUT`, `PATCH`, and `DELETE` handlers MUST establish
  authenticated actor context before handler execution.
- Protected `GET`, `HEAD`, and `OPTIONS` declarations MUST fail application
  startup.
- A protected mapping with no explicit method or any unsupported method MUST
  fail application startup.
- A protected declaration MUST NOT be silently ignored or execute
  unauthenticated.
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
- Attacker-controlled actor query, path, header, or body input MUST NOT replace
  or create the authenticated actor.
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
  startup failure.
- Protected `GET`, `HEAD`, or `OPTIONS` declaration -> application startup
  failure.
- Protected declaration without an explicit method or with another unsupported
  method -> application startup failure.
- Missing `ORCA_SESSION` cookie -> `401 UNAUTHENTICATED`.
- Blank session value -> `401 UNAUTHENTICATED`.
- Malformed or unacceptable session value -> `401 UNAUTHENTICATED`.
- Unknown session -> `401 UNAUTHENTICATED`.
- Expired session -> `401 UNAUTHENTICATED`.
- Invalid session -> `401 UNAUTHENTICATED`.
- Revoked session -> `401 UNAUTHENTICATED`.
- More than one `ORCA_SESSION` cookie -> `401 UNAUTHENTICATED`.
- Only `X-User-Id` is presented -> `401 UNAUTHENTICATED`.
- Attacker-controlled actor input is presented without an establishable
  session -> `401 UNAUTHENTICATED`.
- Non-empty fixture request body -> `400 VALIDATION_ERROR` before fixture
  operation execution.

Every startup failure occurs before the protected handler can accept traffic.
Every request-time rejection occurs before handler execution and uses an
execution count of zero as verification evidence.

## Public Boundary Failure Set

- Absent / null:
  - absent enablement with a protected declaration fails startup;
  - absent cookie and absent actor context reject the request;
  - null or absent public actor id is rejected by `AuthenticatedActor`.
- Blank:
  - blank session input is unauthenticated;
  - blank public actor id is rejected.
- Malformed / untyped / unexpected:
  - malformed or otherwise unacceptable session input is unauthenticated;
  - a mapping without an explicit supported method, an unsupported method, or
    an unexpected actor-shaped request value fails closed.
- Duplicate / ambiguous:
  - multiple `ORCA_SESSION` cookies are unauthenticated and none is selected.
- Stale / invalid:
  - expired, revoked, unknown, or otherwise invalid auth-owned session state is
    unauthenticated.
- Unauthorized:
  - this slice establishes authentication only; product authorization remains
    outside this boundary and cannot be inferred from actor input.

## Verification Mapping

| Normative outcome | Verification |
| --- | --- |
| missing enablement fails startup | Minimal Consumer Fixture startup contract test |
| protected `GET`, `HEAD`, `OPTIONS`, `TRACE`, unspecified, and mixed mappings fail startup | auth startup-validation tests covering every Spring `RequestMethod` outside the supported set |
| protected `POST`, `PUT`, `PATCH`, and `DELETE` establish actor first | auth interceptor tests plus Minimal Consumer Fixture recording-handler contract tests for every supported method |
| missing, blank, malformed, unknown, expired, revoked, and multiple sessions reject identically | auth unit/web tests and Minimal Consumer Fixture contract tests |
| invalid session ownership cannot establish context | `ResolveCurrentUserContextFromSessionUseCaseTest` unregistered-session-owner test; fixture persistence prevents this invalid foreign-key state |
| query, path, header, and actor-shaped body input cannot establish or replace actor | Minimal Consumer Fixture contract tests with and without an establishable session |
| every rejected request executes the handler zero times | Minimal Consumer Fixture recording-handler assertions, including every supported method |
| login, logout, and post-logout rejection remain unchanged | existing and expanded Minimal Consumer Fixture contract test |
| existing organization POST commands keep their mapped boundary | organization/auth web regression tests and Maven reactor verification |
| public actor contains one non-blank id only | `AuthenticatedActorTest` and argument-resolver tests |

No normative outcome requires a manual-only verification exception.

Completion evidence:

- `EmbeddedProtectedCommandStartupValidatorTest` covers the complete Spring
  request-method enum, unspecified mappings, mixed mappings, type-level
  declarations, and missing enablement.
- `CurrentUserContextInterceptorTest` proves actor context establishment for
  every supported method at the interceptor boundary.
- `EmbeddedAuthConsumerContractTest` proves actual handler execution counts for
  every supported method and proves query, path, header, and actor-shaped body
  input cannot create or replace the session actor.
- `ResolveCurrentUserContextFromSessionUseCaseTest` proves an otherwise active
  session with an invalid, unregistered owner is unauthenticated; the fixture's
  foreign-key constraint intentionally prevents constructing that persistence
  state.
- The Minimal Consumer Fixture tests and Maven reactor verification pass.

## Affected and Superseded Documents

- `auth-04` continues to enumerate existing Orca organization protected POST
  routes and explicitly defers consumer declarations to `auth-12`.
- `auth-09` continues to own session-backed context for those routes and is the
  active replacement for organization-08's original demo `X-User-Id`
  transport.
- organization-08 continues to own its five POST endpoints and command mapping;
  its current actor transport is the context established by `auth-09`.
- this spec is the sole authority for embedded consumer protected declaration,
  startup validation, and the supported protected-method matrix.

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

- Creating `auth-14` or another behavior slice.
- Changing the existing organization command paths or organization behavior.
- Replacing `auth-04` / `auth-09` fixed Orca path ownership with a consumer
  route registry.
- Broad package moves or cross-context architecture refactoring.
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
- Credential encoding or migration behavior.
- New database schema or Flyway migration.
