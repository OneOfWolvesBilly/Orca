# DDD Derivation - 12 Embedded Auth and Actor-context Integration

Status: Approved / Implemented.

This note is **derived from**
`docs/specs/auth/12-embedded-auth-actor-context-integration.md`.
It does not introduce new behavior.

## Bounded Context

**auth**

Rationale:

- Auth already owns password login, server-side session state, session
  resolution, current user context, and logout.
- This slice exposes those existing behaviors through a supported embedded
  integration boundary.
- The consumer fixture owns only a no-domain verification command.
- Reference-core remains authoritative for stable error translation.

## Consistency Boundary

No new aggregate root is required.

`AuthenticatedSession` remains the auth-owned source used to resolve an
authenticated user. `CurrentUserContext` remains the internal per-operation
auth model. This slice adds an adapter-facing public actor value after auth has
successfully resolved that internal context.

The public actor value is not a second identity registry and must not contain
session, role, organization, personnel, or profile state.

## Minimum Model

### Existing auth model reused

- `AuthenticatedSession`
  - remains the server-side session aggregate
  - remains authoritative for active, expired, and revoked state

- `CurrentUserContext`
  - remains the internal per-operation context
  - continues to contain exactly one `AuthenticatedUserId`

- `AuthenticatedUserId`
  - remains the auth-owned authenticated identity value
  - is mapped to the public actor value only after successful session
    resolution

### Public integration model

Recommended public contract names:

- `AuthenticatedActor`
  - immutable value with one non-blank `actorId`
  - contains no raw session or auth persistence state

- `OrcaProtectedCommand`
  - public declarative marker for a Spring HTTP command that requires embedded
    Orca actor resolution
  - carries no route, role, permission, or product meaning

- `EnableOrcaEmbeddedAuth`
  - explicit public entry point that installs the supported auth web adapters
    in a consumer Spring Boot host

These names are implementation guidance. Their behavior remains governed by
the spec.

## Rule Placement

### Auth domain rules

- Existing current-user and session invariants remain unchanged.
- One current user context maps to exactly one authenticated user id.
- No new session state transition is introduced.

### Auth application rules

- Resolve a presented opaque session id through existing auth-owned ports.
- Establish internal current user context before mapping the public actor.
- Never accept `X-User-Id` as the protected consumer auth source.
- Keep all session rejection conditions indistinguishable.

### Public integration adapter rules

- Detect protected consumer commands through the supported declaration.
- Resolve authentication before invoking the consumer handler.
- Reject multiple `ORCA_SESSION` cookies instead of selecting one.
- Store internal request context only inside Orca web integration.
- Supply only `AuthenticatedActor` to consumer code.
- Make missing embedded integration fail visibly rather than silently executing
  a protected command without authentication.

### Consumer fixture rules

- Depend on Orca through a normal Maven dependency.
- Enable embedded auth only through the public integration entry point.
- Declare the fixture command through the public protected-command contract.
- Pass the actor id to one product-neutral fixture application operation.
- Do not parse cookies, query Orca tables, or import internal Orca packages.

### Reference-core rules

- Translate unauthenticated and validation failures through the existing stable
  API error contract.
- Do not invent a fixture-specific error contract.

### Infrastructure rules

- Reuse the existing auth repositories, session persistence, Flyway migrations,
  and clock wiring.
- Add no database table or migration.
- Keep persistence types outside the public integration contract.

## Package and Module Placement

Recommended public package:

```text
io.github.oneofwolvesbilly.orca.auth.api
```

Recommended internal integration placement:

```text
io.github.oneofwolvesbilly.orca.auth.infrastructure.spring
io.github.oneofwolvesbilly.orca.auth.web
```

Recommended consumer fixture module:

```text
minimal_consumer_fixture
```

The fixture package must not be nested under the Orca application component
scan root. This proves that integration comes from the public entry point, not
from incidental package scanning.

The first proof may use a Maven reactor dependency. Production publication
coordinates and repository selection remain outside this slice.

## Dependency Direction

```text
Minimal Consumer Fixture
  -> auth public API
  -> Orca embedded Spring entry point
  -> auth web/application boundary
  -> auth domain and ports

auth infrastructure
  -> auth application ports

auth domain
  -> no Spring, HTTP, database, or consumer dependency
```

The dependency must never reverse from Orca into a fixture event, controller,
or application class.

## Multiple-cookie Design

The HTTP adapter must collect all cookies named `ORCA_SESSION`.

- exactly one cookie: pass its opaque value to existing session resolution
- zero cookies: existing unauthenticated path
- more than one cookie: reject as unauthenticated before session lookup

This rule belongs in the web adapter because cookie multiplicity is HTTP input,
not an `AuthenticatedSession` invariant.

## Test Layer Placement

Public contract unit tests validate:

- authenticated actor id is required and non-blank
- the public actor exposes only its actor id
- the protected marker carries no role or product behavior

Auth web tests validate:

- a declared protected command resolves one actor before handler execution
- missing, blank, malformed, unknown, expired, invalid, and revoked sessions
  remain unauthenticated
- multiple `ORCA_SESSION` cookies are rejected without selecting one
- `X-User-Id` remains ineffective
- unprotected handlers are not forced through actor resolution

Consumer contract tests validate:

- the fixture compiles against only the public Orca integration API
- existing login issues an opaque session cookie
- the session invokes the fixture command once with one actor id
- fixture response exposes no actor or session value
- logout revokes the session
- the same fixture command is rejected after logout
- invalid session requests never invoke fixture behavior

Regression tests validate:

- existing organization and reference-core protected commands remain protected
- existing auth-08, auth-09, and auth-11 behavior remains unchanged
- all existing tests continue to pass

Infrastructure tests:

- no new persistence tests are required because the slice adds no adapter or
  schema behavior

## Sensitive Data Design

Map from internal `CurrentUserContext` to the public actor after successful
session resolution. Do not expose the internal context object itself as the
supported consumer contract.

The public actor must not contain cookie, session, credential, role,
organization, personnel, profile, request, or exception fields.

Consumer contract tests may submit opaque session cookies but must not echo or
store those values in fixture behavior.

## Non-Goals

- New auth aggregate or domain invariant.
- New database migration.
- Session renewal or lifetime changes.
- Product authorization model.
- Product-specific command semantics.
- Audit or logging integration.
- Frontend delivery.
- Non-Spring integration.
- Separately deployed Orca service.
- Production artifact publication.
