# Spec 06 - Admin-managed User Provisioning

## Goal

An IT admin creates a regular registered user identity that can later be used as
an authenticated actor and as an organization invitee.

This slice defines the normal managed path for creating auth-owned registered
user identities. It does not introduce public self-registration, login,
credentials, sessions, or system role assignment.

## Domain Terms

- Registered User Identity
  An auth-owned user identity that the system recognizes as an existing user.

- IT Admin
  A registered user identity with the auth-owned system role required to manage
  registered user provisioning.

- Auth System Role
  A system-level auth role used for auth-owned administrative behavior. For this
  slice, the only required system role is `IT_ADMIN`.

- Provisioned User
  The registered user identity created by an IT admin through this behavior.

- Bootstrap IT Admin
  The first IT admin identity that exists before normal admin-managed
  provisioning can be used.

## Preconditions

- At least one bootstrap IT admin registered user identity already exists.
- The actor is authenticated through the existing current user context boundary.

## Scenarios

### Scenario: IT admin provisions a regular registered user identity

**Given**
- The actor is a registered user identity.
- The actor has the `IT_ADMIN` auth system role.
- The requested user id is not already registered.

**When**
- The actor submits a provision-user request containing the requested user id.

**Then**
- Auth creates a registered user identity for the requested user id.
- The new identity is available to auth current-user-context establishment.
- The new identity is available to organization registered-user checks.
- The new identity does not receive any auth system role from this behavior.

### Scenario: Non-admin registered user cannot provision users

**Given**
- The actor is a registered user identity.
- The actor does not have the `IT_ADMIN` auth system role.

**When**
- The actor submits a provision-user request.

**Then**
- The request is rejected.
- No registered user identity is created.

## Acceptance Criteria

- Only an authenticated registered user identity with the `IT_ADMIN` auth system
  role MAY provision a registered user identity.
- The requested user id MUST be non-empty.
- The requested user id MUST NOT already belong to a registered user identity.
- A successfully provisioned user MUST become an auth-owned registered user
  identity.
- A successfully provisioned user MUST be visible through the registered-user
  source defined by Spec 05.
- A successfully provisioned user MUST NOT receive `IT_ADMIN` or any other auth
  system role from this behavior.
- This slice MUST NOT change organization domain behavior.
- This slice MUST NOT give IT admins any organization GroupAdmin role.
- This slice MUST NOT allow GroupAdmins to provision registered user identities.
- This slice MUST NOT define how the first bootstrap IT admin is created.

## Invariants

- A registered user identity always has one non-empty authenticated user id.
- Auth system roles are auth-owned and are separate from organization group
  roles.
- The `IT_ADMIN` auth system role authorizes auth user provisioning only.
- Organization GroupAdmin role authorizes group-scoped membership behavior only.
- A newly provisioned registered user identity has no auth system role by
  default.

## Error Cases

- Missing authenticated actor -> rejected as unauthenticated.
- Blank authenticated actor -> rejected as unauthenticated under existing auth
  rules.
- Unknown authenticated actor -> rejected as unauthenticated under existing auth
  rules.
- Authenticated actor lacks `IT_ADMIN` -> rejected as unauthorized.
- Requested user id is empty -> validation error.
- Requested user id is already registered -> rejected.

## Non-Goals

- Creating the first bootstrap IT admin.
- Public self-registration.
- Login.
- Logout.
- Session management.
- Cookie issuance.
- Token issuance or refresh.
- OAuth or external identity provider flows.
- Password storage or hashing.
- Password reset or credential recovery.
- User profile behavior.
- Assigning, revoking, or listing auth system roles.
- Disabling, deleting, suspending, or reactivating users.
- Email invitation delivery.
- Bulk user import.
- Frontend UI.
- Changing organization command domain rules.
- Refining organization GroupAdmin permissions.
