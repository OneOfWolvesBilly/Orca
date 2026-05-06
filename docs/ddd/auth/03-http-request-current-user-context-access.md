# DDD Derivation — 03 HTTP Request Current User Context Access

This note is **derived from**
`docs/specs/auth/03-http-request-current-user-context-access.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- The slice still governs how authenticated current user context is exposed to
  protected HTTP behavior.
- The behavior refines the auth HTTP adapter seam without introducing new
  business behavior in downstream bounded contexts.

---

## Aggregate Root

No aggregate root is introduced in this slice.

Why:
- The slice remains request-scoped adapter behavior only.
- No persisted state or new consistency boundary is introduced.

---

## Minimum Model Additions

### Existing Behavior Reused
- `CurrentUserContextResolver`
  - remains the authority for establishing current user context from presented
    HTTP request identities

### Web Adapter Additions
- request-scoped current user context exposure
  - stores the established `CurrentUserContext` for the protected HTTP request
  - makes that established context available to downstream web adapter code for
    the same request
  - prevents downstream web adapter code from needing to remap request
    identities into auth context again

---

## Rule Placement

### Auth establishment rules
- Exactly one authenticated user id is required.
- Blank or multiple presented identities are rejected.
- These rules remain owned by Spec 01 and Spec 02 behavior.

### Web adapter responsibilities
- Establish current user context once at the protected HTTP request boundary.
- Expose the established context as request-scoped input for downstream command
  mapping in the same request.
- Stop downstream protected behavior when current user context is unavailable.

### Explicitly not in this slice
- New authentication mechanisms.
- Bounded-context-specific authorization checks.
- Changes to organization command semantics.
- Security framework, login, token, or session orchestration.

---

## Notes on the Organization Seam

- Downstream protected command controllers should consume the established
  `CurrentUserContext` for the request rather than re-reading presented
  `X-User-Id` values and re-establishing auth context themselves.
- This slice refines the HTTP seam after Spec 02 by making the established auth
  context directly available inside the same protected request.
