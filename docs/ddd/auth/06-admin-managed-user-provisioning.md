# DDD Derivation - 06 Admin-managed User Provisioning

This note is **derived from**
`docs/specs/auth/06-admin-managed-user-provisioning.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- Registered user identity lifecycle is auth-owned.
- The `IT_ADMIN` system role authorizes auth-owned administrative behavior.
- Organization consumes registered-user existence checks but does not own user
  identity creation.

---

## Aggregate Root

**RegisteredUserIdentity**

Why:
- Spec 05 introduced registered user identity state without a lifecycle.
- Spec 06 introduces the first normal lifecycle command that creates registered
  user identities.
- The identity itself owns the invariant that a registered user identity has one
  valid non-empty authenticated user id.

Auth system role state is auth-owned, but this slice only requires checking
whether the actor has `IT_ADMIN`. It does not introduce system role assignment
or revocation behavior.

---

## Minimum Model Additions

### Domain Model
- `RegisteredUserIdentity`
  - remains the auth-owned identity model
  - is created for the provisioned user id
- `AuthSystemRole`
  - enum or value object containing at least `IT_ADMIN`

### Application Ports
- `RegisteredUserIdentityRepository`
  - already persists registered user identities
  - must support checking whether the requested user id already exists
- `AuthSystemRoleDirectory`
  - answers whether an actor registered user identity has an auth system role
  - this slice only needs the `IT_ADMIN` check

### Application Use Case
- `ProvisionRegisteredUserIdentityUseCase`
  - receives actor user id from current user context
  - receives requested user id from the command
  - verifies actor has `IT_ADMIN`
  - verifies requested user id is not already registered
  - creates and persists `RegisteredUserIdentity`

---

## Rule Placement

### Auth domain rules
- `AuthenticatedUserId` remains the source of truth for valid auth user ids.
- `RegisteredUserIdentity` guarantees the registered identity contains a valid
  auth user id.
- `AuthSystemRole` defines auth-owned system role values.

### Auth application rules
- Actor must be authenticated and registered through the existing current user
  context boundary.
- Actor must have `IT_ADMIN`.
- Requested user id must not already be registered.
- Provisioned users receive no auth system role from this behavior.

### Infrastructure rules
- Persistence may store registered identities and auth system role assignments in
  auth-owned tables.
- Bootstrap IT admin data may exist before this slice runs, but its creation is
  outside this slice.

### Organization rules
- Organization continues to use the registered-user source defined by Spec 05.
- Organization GroupAdmin checks remain group-scoped and unchanged.

---

## Explicitly Not In This Slice

- Bootstrap creation of the first IT admin.
- Public self-registration.
- Login, logout, sessions, cookies, tokens, OAuth, passwords, and credential
  recovery.
- User profiles.
- System role assignment, revocation, or listing.
- User deletion, disabling, suspension, or reactivation.
- Organization permission refinement.
- Frontend UI.

---

## Test Layer Placement

- Domain tests validate any new `AuthSystemRole` value constraints if represented
  as a value object.
- Application tests validate IT admin authorization, duplicate user rejection,
  and successful registered user provisioning.
- Infrastructure tests may validate auth-owned persistence for system role lookup
  and registered identity creation.
- Web tests should be added only if this slice exposes an HTTP adapter.
