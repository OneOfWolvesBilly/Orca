# DDD Derivation - 04 Protected HTTP Command Boundary Mapping

This note is **derived from**
`docs/specs/auth/04-protected-http-command-boundary-mapping.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- The slice defines where the auth HTTP request boundary is applied.
- The behavior is about current user context establishment before protected
  command execution, not about organization command semantics.

---

## Aggregate Root

No aggregate root is introduced in this slice.

Why:
- The slice is HTTP adapter mapping only.
- It introduces no persisted state, no lifecycle, and no new consistency
  boundary.

---

## Minimum Model Additions

No domain or application model additions are required.

### Existing Behavior Reused
- `CurrentUserContextInterceptor`
  - establishes request-scoped current user context for protected HTTP requests
- `CurrentUserContextResolver`
  - maps presented `X-User-Id` values into the existing auth use case
- `CurrentUserContextArgumentResolver`
  - supplies the established context to downstream web adapters

### Web Adapter Mapping
- The Spring MVC interceptor registration is scoped to the currently specified
  protected command paths:
  - `POST /api/groups`
  - `POST /api/groups/{groupId}/invitations`
  - `POST /api/group-invitations/{invitationId}/accept`
  - `POST /api/group-invitations/{invitationId}/reject`
  - `POST /api/group-invitations/{invitationId}/revoke`

---

## Rule Placement

### Auth application/domain rules
- Unchanged from Spec 01 and Spec 02.
- Exactly one non-blank authenticated user id is required when current user
  context is established.

### Web adapter responsibilities
- Apply the current user context interceptor to currently specified protected
  command paths.
- Avoid rejecting unmapped requests solely because they lack current user
  context.
- Keep downstream command controllers consuming request-scoped
  `CurrentUserContext`.

### Explicitly not in this slice
- Login, session, token, OAuth, registration, or password flows.
- Spring Security or a production authentication framework.
- New public endpoint behavior.
- Current-user inspection endpoint.
- Changes to organization command behavior.

---

## Test Layer Placement

This slice is validated with HTTP/web integration tests:
- listed protected command endpoints reject missing current user context
- an unmapped `/api` request without current user context is not rejected by auth
  before normal web routing

Domain and application tests remain unchanged because this slice adds no domain
or application behavior.
