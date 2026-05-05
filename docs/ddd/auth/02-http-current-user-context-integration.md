# DDD Derivation — 02 HTTP Current User Context Integration

This note is **derived from**
`docs/specs/auth/02-http-current-user-context-integration.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- The slice still exists to establish and expose authenticated user context.
- Only the adapter boundary changes; the downstream business behavior does not.

---

## Aggregate Root

No aggregate root is introduced in this slice.

Why:
- The slice integrates transport input into the existing auth behavior.
- It still does not introduce persisted business state or a new consistency
  boundary.

---

## Minimum Model Additions

### Existing Application Service Reused
- `EstablishCurrentUserContextUseCase`
  - remains the authority for accepting zero / one / many presented identities
  - returns `CurrentUserContext` only when exactly one authenticated user id is
    established

### Web Adapter
- `CurrentUserContextResolver`
  - accepts the `X-User-Id` values presented for one HTTP request
  - maps those values into `EstablishCurrentUserContextCommand`
  - returns `CurrentUserContext` for downstream protected behavior
  - rejects the request when current user context cannot be established

---

## Rule Placement

### Auth application/domain rules
- Exactly one authenticated user id is required.
- A blank user id is invalid.
- Current user context is immutable once established.

### Web adapter responsibilities
- Read the presented `X-User-Id` values from one HTTP request.
- Pass all presented values to the auth use case for one operation.
- Stop downstream protected behavior when auth context establishment fails.

### Explicitly not in this slice
- Bounded-context-specific authorization checks.
- Request body mapping for organization commands.
- Security framework, token, session, or login orchestration.

---

## Notes on the Organization Seam

- Downstream command controllers should consume `CurrentUserContext` or the
  resolved authenticated user id, not parse transport-specific auth headers
  themselves.
- This slice closes the gap left by Auth Spec 01, whose integration boundary was
  intentionally deferred.
