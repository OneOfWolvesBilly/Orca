# Spec 09 - Protected HTTP Session Context

## Goal

Allow protected HTTP command requests to establish current user context from the
server-side session created by auth-08 password login.

This slice replaces the demo `X-User-Id` protected command mechanism with the
auth-08 `ORCA_SESSION` cookie and auth-owned server-side session state. A
protected command may proceed only when the request presents a session cookie
whose opaque session id resolves to a valid authenticated user id on the server.

This slice does not change login, session creation, organization domain
behavior, or organization application command behavior.

## Domain Terms

- Auth Session Cookie
  The `ORCA_SESSION` cookie issued by auth-08 after successful password login.
  Its value is the opaque session id for auth-owned server-side session state.

- Opaque Session Id
  A server-issued session identifier that is only a lookup reference. It carries
  no encoded user, personnel, role, organization, profile, or permission
  information.

- Server-side Session State
  Auth-owned persistent authenticated session state created by auth-08. It maps
  an opaque session id to exactly one authenticated user id and bounded
  expiration state.

- Protected HTTP Command Request
  An HTTP command request listed in the protected command mapping from Spec 04.

- Current User Context
  The request-scoped auth context that supplies the authenticated user id to
  downstream protected command behavior.

## HTTP Adapter Contract

Protected HTTP command requests present the auth-08 session cookie:

```text
Cookie: ORCA_SESSION=<opaque-session-id>
```

The cookie value is a lookup reference only. The client MUST NOT be able to
derive user id, personnel, role, organization, profile, or permission
information from the cookie value.

For protected HTTP command requests, `ORCA_SESSION` replaces the demo
`X-User-Id` transport defined by the earlier HTTP current-user-context slices.
The protected command boundary MUST NOT establish current user context from
`X-User-Id`.

## Protected Command Mapping

The protected HTTP command endpoints remain the list defined by Spec 04 and
derived from organization Spec 08:

```text
POST /api/groups
POST /api/groups/{groupId}/invitations
POST /api/group-invitations/{invitationId}/accept
POST /api/group-invitations/{invitationId}/reject
POST /api/group-invitations/{invitationId}/revoke
```

This slice does not add, remove, or rename protected command endpoints.

This fixed list remains the existing Orca organization POST mapping from
auth-04. It is not the extension contract for embedded consumers. Auth-12 owns
consumer-declared `@OrcaProtectedCommand` handlers and their supported HTTP
method matrix; those declarations reuse this slice's auth-owned session
resolution without being added to the fixed organization list.

## Scenarios

### Scenario: Protected command establishes current user context from a valid session

**Given**
- A request targets a protected HTTP command endpoint.
- The request presents an `ORCA_SESSION` cookie.
- The cookie value is an opaque session id.
- Auth-owned server-side session state contains that session id.
- The server-side session is valid and unexpired.

**When**
- The HTTP auth boundary establishes current user context for the request.

**Then**
- Auth resolves the authenticated user id only from server-side session state.
- Auth establishes current user context for the resolved authenticated user id.
- Downstream protected command mapping consumes the established current user
  context.
- The protected command may proceed using that authenticated user id as its
  actor.

### Scenario: Protected command rejects a request without an establishable session

**Given**
- A request targets a protected HTTP command endpoint.
- The request does not present an `ORCA_SESSION` cookie, presents a blank session
  id, presents a session id that does not exist, or presents a session id whose
  server-side session is expired or invalid.

**When**
- The HTTP auth boundary attempts to establish current user context.

**Then**
- Current user context is not established.
- Downstream protected command behavior does not execute.
- The request is rejected as unauthenticated.
- The response does not reveal whether the session cookie was missing, blank,
  malformed, unknown, expired, invalid, or revoked.

### Scenario: Session cookie does not expose authenticated user data

**Given**
- A protected HTTP command request presents an `ORCA_SESSION` cookie.

**When**
- The HTTP auth boundary reads the cookie value.

**Then**
- The cookie value is treated only as an opaque server-side lookup key.
- No user id, personnel, role, organization, profile, or permission information
  is decoded from the cookie value.
- The authenticated user id is obtained only from auth-owned server-side session
  state.

### Scenario: Demo header no longer establishes protected command context

**Given**
- A request targets a protected HTTP command endpoint.
- The request presents `X-User-Id`.
- The request does not present an establishable `ORCA_SESSION` cookie.

**When**
- The HTTP auth boundary attempts to establish current user context.

**Then**
- The `X-User-Id` value is not used as the protected command auth source.
- Current user context is not established.
- The request is rejected as unauthenticated.
- Downstream protected command behavior does not execute.

## Acceptance Criteria

- A protected HTTP command request MUST establish current user context from the
  `ORCA_SESSION` cookie and auth-owned server-side session state.
- The `ORCA_SESSION` cookie value MUST be treated as an opaque session id.
- The session cookie value MUST NOT encode user id, personnel, role,
  organization, profile, or permission information.
- Authenticated user id MUST be resolved only from auth-owned server-side
  session state.
- A valid, unexpired server-side session MUST establish current user context for
  exactly one authenticated user id.
- Missing `ORCA_SESSION` cookie MUST be rejected as unauthenticated.
- Blank session id MUST be rejected as unauthenticated.
- Unknown session id MUST be rejected as unauthenticated.
- Expired session MUST be rejected as unauthenticated.
- Invalid session MUST be rejected as unauthenticated.
- Session rejection responses MUST NOT reveal which session condition failed.
- Protected command context MUST NOT be established from `X-User-Id`.
- This slice MUST NOT add, remove, or change protected command endpoints.
- Embedded consumer protected declarations MUST follow auth-12 and MUST NOT be
  silently excluded because they are absent from the auth-04 fixed path list.
- This slice MUST NOT change login endpoint behavior.
- This slice MUST NOT create sessions.
- This slice MUST NOT renew sessions or extend session lifetime.
- This slice MUST NOT add or change downstream organization behavior.

## Invariants

- No new auth domain aggregate invariant is introduced by this slice.
- Current user context invariants from Spec 01 remain the source of truth after
  a session resolves to an authenticated user id.
- A protected HTTP command request has at most one current user context.
- A session cookie value is an opaque reference only.
- User, personnel, role, organization, profile, and permission information
  remain server-side.
- Session expiration is evaluated from auth-owned server-side session state.

## Error Cases

- Missing `ORCA_SESSION` cookie -> rejected as unauthenticated.
- Blank session id -> rejected as unauthenticated.
- Malformed or otherwise unacceptable session id input -> rejected as
  unauthenticated.
- Unknown session id -> rejected as unauthenticated.
- Expired session -> rejected as unauthenticated.
- Invalid or revoked session -> rejected as unauthenticated.
- Request presents only `X-User-Id` for a protected command -> rejected as
  unauthenticated.

All rejection responses use the same unauthenticated response shape and do not
identify the failed session condition.

## Non-Goals

- Login credential verification.
- Session creation.
- Login endpoint changes.
- Logout.
- Session renewal or sliding expiration.
- Login failure reference id.
- Login audit.
- Refresh token or access token behavior.
- OAuth or external identity provider flows.
- MFA.
- Password reset or credential recovery.
- Role assignment, revocation, or listing.
- Authorization permission model changes.
- IT_ADMIN lifecycle.
- GroupAdmin lifecycle.
- DBM, ITSM, or approval workflow behavior.
- Frontend UI.
- User profile or current-user endpoint.
- Changing organization domain behavior.
- Changing organization application command behavior.
- Spring Security or a production authentication framework.
