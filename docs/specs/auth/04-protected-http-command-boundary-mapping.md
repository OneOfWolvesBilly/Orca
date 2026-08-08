# Spec 04 - Protected HTTP Command Boundary Mapping

## Goal

Define the minimum HTTP request mapping for protected command behavior so the
auth HTTP boundary is applied to the command endpoints that are currently
specified as requiring current user context.

This slice does not introduce a new authentication mechanism. It only makes the
existing protected command boundary explicit for the HTTP endpoints that already
exist.

## Domain Terms

- Protected HTTP Command Behavior
  An HTTP command operation that requires an authenticated actor and must not
  execute without current user context.

- Current User Context
  The request-scoped auth context established by the existing auth HTTP boundary.

- Protected Command Mapping
  The adapter-level HTTP path mapping that determines which requests must pass
  through current user context establishment before downstream command mapping.

## Protected Command Mapping

The currently specified protected HTTP command endpoints are:

```text
POST /api/groups
POST /api/groups/{groupId}/invitations
POST /api/group-invitations/{invitationId}/accept
POST /api/group-invitations/{invitationId}/reject
POST /api/group-invitations/{invitationId}/revoke
```

These endpoints are derived from
`docs/specs/organization/08-web-api-integration.md`.

This fixed list owns only the existing Orca organization command mapping. Auth
Spec 09 supersedes the original demo identity transport for these routes with
`ORCA_SESSION` session resolution. Auth Spec 12 separately owns the public
`@OrcaProtectedCommand` declaration used by embedded consumers; consumer routes
are not added to this list.

## Scenario

### Scenario: Protected command endpoints pass through the auth request boundary

**Given**
- A request targets one of the currently specified protected HTTP command endpoints.

**When**
- The HTTP request enters the web adapter.

**Then**
- The auth HTTP boundary establishes current user context before downstream
  command mapping executes.
- If current user context cannot be established, the downstream command behavior
  does not execute.

### Scenario: Unmapped HTTP requests do not require current user context

**Given**
- A request does not target one of the currently specified protected HTTP command
  endpoints.

**When**
- The HTTP request enters the web adapter.

**Then**
- The auth HTTP boundary does not require current user context for that request.
- This slice does not define any new public behavior or response body for that
  request.

## Acceptance Criteria

- Every endpoint listed in the Protected Command Mapping MUST pass through the
  auth request boundary before downstream command mapping executes.
- A listed protected command endpoint MUST be rejected as unauthenticated when
  current user context cannot be established.
- Requests not listed in the Protected Command Mapping MUST NOT be rejected by
  auth solely because current user context is absent.
- The protected command mapping MUST be derived only from currently authoritative
  HTTP command specs.
- This fixed mapping MUST NOT be treated as the allowlist for embedded consumer
  declarations governed by auth-12.
- The actor transport for these existing routes MUST follow auth-09 rather than
  the earlier demo `X-User-Id` transport.
- This slice MUST NOT add or change downstream organization behavior.
- This slice MUST NOT introduce a security framework or a new authentication
  mechanism.

## Invariants

- No new auth domain invariants are introduced.
- The current user context invariants from Spec 01 remain the source of truth.
- Auth Spec 09 is the active source of truth for establishing protected command
  current user context from auth-owned session state; Spec 02 remains only the
  superseded demo transport definition.
- The request-scoped access rules from Spec 03 remain the source of truth for how
  downstream web adapters consume the established context.

## Error Cases

- A listed protected command endpoint is invoked without establishable current
  user context -> rejected as unauthenticated.
- A request is not listed in the protected command mapping -> not rejected by
  auth solely because current user context is absent.

## Non-Goals

- Login.
- Registration.
- Logout.
- Session management.
- Token issuance or refresh.
- OAuth or external identity provider flows.
- Password reset or credential recovery.
- Spring Security or a production authentication framework.
- Authorization rules for specific bounded contexts such as GroupAdmin checks.
- New current-user inspection endpoint.
- New health, info, public, query, or read endpoints.
- Frontend UI.
