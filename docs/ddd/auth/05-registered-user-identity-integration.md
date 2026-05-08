# DDD Derivation - 05 Registered User Identity Integration

This note is **derived from**
`docs/specs/auth/05-registered-user-identity-integration.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- Registered user identity is identity state owned by auth.
- Organization only asks whether a user id is registered; it does not own user
  identity lifecycle.

---

## Aggregate Root

No aggregate root lifecycle is introduced in this slice.

Why:
- This slice persists recognized user identities and exposes existence checks.
- It does not define registration, profile editing, credential changes, or user
  lifecycle transitions.

---

## Minimum Model Additions

### Domain Model
- `RegisteredUserIdentity`
  - wraps an existing `AuthenticatedUserId`
  - guarantees the registered identity uses a valid non-empty user id

### Application Port
- `RegisteredUserIdentityRepository`
  - persists registered user identities
  - answers whether an `AuthenticatedUserId` is registered

### Application Rule Change
- `EstablishCurrentUserContextUseCase`
  - still enforces exactly one presented user id
  - additionally requires the presented user id to exist in
    `RegisteredUserIdentityRepository`

### Infrastructure
- `JdbcRegisteredUserIdentityRepository`
  - stores auth registered identities in an auth-owned table
- Organization HTTP wiring receives a `RegisteredUserDirectory` adapter backed by
  auth registered identity state instead of an organization-owned in-memory user
  list.

---

## Rule Placement

### Auth domain rules
- `AuthenticatedUserId` remains the source of truth for valid auth user ids.
- `RegisteredUserIdentity` ensures persisted identity state contains a valid
  auth user id.

### Auth application rules
- Protected current user context is established only for registered identities.
- Unknown presented user ids are unauthenticated.

### Organization application rules
- Existing invitee validation remains in organization application behavior.
- The source used to answer registered-user existence comes from auth.

### Explicitly not in this slice
- Login, sessions, cookies, tokens, registration, password persistence, password
  hashing, OAuth, logout, and frontend UI.

---

## Test Layer Placement

- Domain tests validate `RegisteredUserIdentity` invariants.
- Application tests validate current user context establishment rejects unknown
  presented identities.
- Infrastructure tests validate auth identity persistence.
- Web integration tests validate protected command endpoints reject unknown
  actors and organization invitee checks use auth identity state.
