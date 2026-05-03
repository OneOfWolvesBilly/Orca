# DDD Derivation — 01 Establish Authenticated User Context

This note is **derived from** `docs/specs/auth/01-establish-authenticated-user-context.md`.
It does not introduce new behavior.
Its purpose is to make model decisions explicit and reviewable.

---

## Bounded Context

**auth**

Rationale:
- The spec defines authenticated identity and current user context as auth concepts.
- The behavior exists to supply a stable actor user id to downstream protected behavior.

---

## Aggregate Root

No aggregate root is introduced in this slice.

Why:
- The slice defines per-operation context establishment, not long-lived mutable business state.
- There is no multi-entity consistency boundary or persistence-owned lifecycle in this behavior.
- The core output is an immutable current user context value passed downstream.

---

## Entities / Value Objects / Services (Minimum Set)

### Value Objects
- `AuthenticatedUserId`
  - non-empty authenticated user identity
- `CurrentUserContext`
  - immutable per-operation context containing exactly one authenticated user id

### Application Service
- `EstablishCurrentUserContextUseCase`
  - accepts the identities presented to the auth boundary for one operation
  - returns a `CurrentUserContext` only when exactly one authenticated user id is established

---

## Invariant Checklist (Mapped to the Spec)

### Domain invariants
- A current user context always contains exactly one authenticated user id.
- An authenticated user id must be non-empty.
- Once created, current user context does not change user id during the operation.

### Application rules
- Protected behavior must not execute before current user context is established.
- No presented authenticated identity for the operation → reject as unauthenticated.
- More than one presented authenticated identity for the operation → reject.

---

## Command Shape (Conceptual)

The auth boundary receives the authenticated identities presented for one operation.

Expected outcomes:
- exactly one presented identity → establish `CurrentUserContext`
- zero presented identities → reject
- more than one presented identity → reject

This slice does not define how those identities are carried into the auth boundary.

---

## Notes on Placement

- This slice introduces no HTTP contract, security framework, session, token, or adapter mechanism.
- Transport-specific mapping into the auth boundary belongs to a later integration slice.
- Downstream bounded contexts should depend on `CurrentUserContext` or its resolved authenticated user id,
  not on transport-specific request details.
