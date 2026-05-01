# Spec 01 — Establish Authenticated User Context

## Goal

Before any protected behavior is executed, the system establishes the current authenticated user context so downstream behavior can act on a single authenticated user id.

This is the minimum auth capability required by other bounded contexts that depend on an actor user id.

## Domain Terms

- Authenticated User
  A user identity that the system has already accepted as authenticated for the current operation.

- Current User Context
  The per-operation auth context that supplies the authenticated user's userId to downstream behavior.

- Protected Behavior
  Any behavior that requires an authenticated actor and must be rejected when no authenticated user context is available.

## Scenario

### Scenario: System establishes current user context for a protected behavior

**Given**
- A protected behavior is about to execute.
- The operation has authenticated user information available to the auth boundary.

**When**
- The system establishes the current user context for that operation.

**Then**
- A current user context is created for the operation.
- The current user context contains exactly one authenticated user id.
- The same authenticated user id is provided to downstream behavior for the full operation.
- The protected behavior may proceed using that authenticated user id as its actor.

## Acceptance Criteria

- Protected behavior MUST NOT execute before current user context is established.
- Current user context MUST contain a non-empty userId.
- Current user context MUST represent exactly one authenticated user for the operation.
- The authenticated user id provided to downstream behavior MUST remain the same for the full operation.
- If current user context cannot be established, the protected behavior MUST be rejected as unauthenticated.

## Invariants

- A protected behavior has at most one current authenticated user context.
- A current user context always maps to exactly one authenticated user id.
- Once established for an operation, the authenticated user id does not change during that operation.

## Error Cases

- No authenticated user information is available for a protected behavior → rejected as unauthenticated.
- Authenticated user information does not yield a non-empty userId → rejected.
- More than one authenticated user identity is presented for the same operation → rejected.

## Non-Goals

- Login.
- Registration.
- Logout.
- Session management.
- Token issuance or refresh.
- OAuth or external identity provider flows.
- Password reset or credential recovery.
- User profile or user persistence behavior.

## Out of Scope (Integration)

- HTTP/security framework contract and transport-specific auth mechanisms.
- How authenticated user information is carried into the auth boundary.
- Mapping from specific adapters, headers, cookies, tokens, or sessions into current user context.
- Authorization rules for bounded-context-specific permissions such as GroupAdmin checks.
