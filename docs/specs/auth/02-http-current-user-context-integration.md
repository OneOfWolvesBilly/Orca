# Spec 02 — HTTP Current User Context Integration

## Goal

Expose the existing auth behavior from Spec 01 to HTTP-protected operations by
mapping presented request user identities into the auth boundary and establishing
the current user context before downstream protected behavior executes.

This slice introduces no login, no session, no token model, and no
bounded-context-specific authorization rules.

## Domain Terms

- Authenticated User
  A user identity that the system has already accepted as authenticated for the
  current operation.

- Current User Context
  The per-operation auth context that supplies the authenticated user's userId
  to downstream behavior.

- Protected HTTP Behavior
  An HTTP command operation that requires an authenticated actor and must be
  rejected when current user context cannot be established.

## HTTP Adapter Contract

For this demo integration slice, the HTTP adapter receives authenticated user
information from:

```text
X-User-Id: user-id
```

If more than one `X-User-Id` value is presented for the same request, the
request presents more than one authenticated identity for the operation.

This header is an adapter mechanism only.
It does not introduce a security model or bounded-context-specific permission
rule.

## Scenario

### Scenario: HTTP adapter establishes current user context for a protected request

**Given**
- A protected HTTP behavior is about to execute.
- The request presents zero, one, or more `X-User-Id` values to the auth boundary.

**When**
- The HTTP adapter establishes current user context for that request.

**Then**
- The adapter invokes the existing auth behavior from Spec 01.
- Exactly one presented non-empty user id establishes a current user context.
- The established authenticated user id is provided to downstream protected behavior
  for the full request.
- If current user context cannot be established, downstream protected behavior
  does not execute.

## Acceptance Criteria

- The HTTP adapter MUST establish current user context before invoking downstream
  protected behavior.
- The HTTP adapter MUST map all presented `X-User-Id` values for the request into
  the auth boundary for one operation.
- Exactly one non-empty presented user id MUST establish current user context.
- No presented `X-User-Id` value MUST be rejected as unauthenticated.
- A blank presented `X-User-Id` value MUST be rejected as unauthenticated.
- More than one presented `X-User-Id` value MUST be rejected as unauthenticated.
- The HTTP adapter MUST NOT add or change downstream business behavior.

## Invariants

- No new invariants are introduced in this slice.
- The invariants from Spec 01 remain the source of truth for current user context.

## Error Cases

- No `X-User-Id` value is presented for a protected request → rejected as
  unauthenticated.
- A presented `X-User-Id` value is blank → rejected as unauthenticated.
- More than one `X-User-Id` value is presented for the same request → rejected as
  unauthenticated.

## Non-Goals

- Login.
- Registration.
- Logout.
- Session management.
- Token issuance or refresh.
- OAuth or external identity provider flows.
- Password reset or credential recovery.
- Authorization rules for specific bounded contexts such as GroupAdmin checks.
- Request body mapping for unrelated business commands.
