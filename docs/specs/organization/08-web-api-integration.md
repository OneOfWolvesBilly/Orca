# Spec 08 — Organization Web API Integration

Status: Approved / Implemented.

## Goal

Expose the existing organization behaviors from Spec 01–07 through HTTP endpoints
without changing any specified domain or application behavior.

This slice defines the web adapter contract for:
- creating a group
- inviting a member
- accepting an invitation
- rejecting an invitation
- revoking an invitation

This slice introduces no new domain behavior, no new invariants, and no new invitation status.

The adapter consumes the typed organization application failure boundary from
Spec 06. It does not infer failure meaning from exception type alone, exception
message text, message prefix, or message wording.

## Domain Terms

- Group  
  The aggregate root that owns members and invitations.

- GroupId  
  Identifier of a Group.

- GroupInvitation  
  An invitation that can be accepted/rejected/revoked.

- GroupInvitationId  
  Identifier of a GroupInvitation.

- GroupRole  
  The intended membership role for a group member.

- Authenticated User  
  The user identity resolved by the web adapter for the current request.
  Auth Spec 09 supersedes this slice's original demo header transport: current
  behavior receives the actor from auth-owned `ORCA_SESSION` resolution and
  request-scoped current user context.

## HTTP Contract

All endpoints in this slice are command endpoints and MUST use `POST`.

Requests that depend on the current user MUST NOT use `GET`
(see `docs/constraints.md`).

### Create Group

```text
POST /api/groups
```

Request body:

```json
{
  "name": "Core Team",
  "description": "Optional description"
}
```

Response body:

```json
{
  "groupId": "group-id"
}
```

### Invite Member

```text
POST /api/groups/{groupId}/invitations
```

Request body:

```json
{
  "inviteeUserId": "user-id",
  "intendedRole": "MEMBER"
}
```

Response body:

```json
{
  "invitationId": "invitation-id"
}
```

### Accept Invitation

```text
POST /api/group-invitations/{invitationId}/accept
```

Request body:

```json
{}
```

Response body:

```json
{
  "status": "ACCEPTED"
}
```

### Reject Invitation

```text
POST /api/group-invitations/{invitationId}/reject
```

Request body:

```json
{}
```

Response body:

```json
{
  "status": "REJECTED"
}
```

### Revoke Invitation

```text
POST /api/group-invitations/{invitationId}/revoke
```

Request body:

```json
{}
```

Response body:

```json
{
  "status": "REVOKED"
}
```

## Authenticated User Resolution

The original organization-08 demo adapter read `X-User-Id`. Auth Spec 09 is the
active authority and supersedes that transport for every endpoint in this
spec. The web adapter now receives the actor from the request-scoped current
user context established from `ORCA_SESSION` and auth-owned server-side session
state before the organization use case executes.

The five routes remain the fixed Orca protected POST mapping owned by auth-04
and auth-09. Auth-12's public `@OrcaProtectedCommand` contract allows embedded
consumers to declare their own protected `POST`, `PUT`, `PATCH`, or `DELETE`
handlers without changing this organization route list. It does not change the
methods or behavior of these organization endpoints.

## Scenarios

### Scenario: HTTP client creates a group

**Given**
- Auth has established one request-scoped current user context from an
  establishable `ORCA_SESSION`.
- The request body contains a group name and optional description.

**When**
- The client submits `POST /api/groups`.

**Then**
- The web adapter invokes the Spec 01 application behavior.
- The authenticated user becomes the creator user id.
- The response contains the created `groupId`.
- No domain behavior beyond Spec 01 is introduced.

---

### Scenario: HTTP client invites a member

**Given**
- Auth has established one request-scoped current user context from an
  establishable `ORCA_SESSION`.
- The path contains `groupId`.
- The request body contains `inviteeUserId` and `intendedRole`.

**When**
- The client submits `POST /api/groups/{groupId}/invitations`.

**Then**
- The web adapter invokes the Spec 02 / Spec 06 application behavior.
- The authenticated user becomes the inviter user id.
- The response contains the created `invitationId`.
- No domain behavior beyond Spec 02 and Spec 06 is introduced.

---

### Scenario: HTTP client accepts an invitation

**Given**
- Auth has established one request-scoped current user context from an
  establishable `ORCA_SESSION`.
- The path contains `invitationId`.

**When**
- The client submits `POST /api/group-invitations/{invitationId}/accept`.

**Then**
- The web adapter invokes the Spec 03 / Spec 06 application behavior.
- The authenticated user becomes the accepting user id.
- The response indicates `ACCEPTED`.
- No domain behavior beyond Spec 03 and Spec 06 is introduced.

---

### Scenario: HTTP client rejects an invitation

**Given**
- Auth has established one request-scoped current user context from an
  establishable `ORCA_SESSION`.
- The path contains `invitationId`.

**When**
- The client submits `POST /api/group-invitations/{invitationId}/reject`.

**Then**
- The web adapter invokes the Spec 04 / Spec 06 application behavior.
- The authenticated user becomes the rejecting user id.
- The response indicates `REJECTED`.
- No domain behavior beyond Spec 04 and Spec 06 is introduced.

---

### Scenario: HTTP client revokes an invitation

**Given**
- Auth has established one request-scoped current user context from an
  establishable `ORCA_SESSION`.
- The path contains `invitationId`.

**When**
- The client submits `POST /api/group-invitations/{invitationId}/revoke`.

**Then**
- The web adapter invokes the Spec 05 / Spec 06 application behavior.
- The authenticated user becomes the revoking user id.
- The response indicates `REVOKED`.
- No domain behavior beyond Spec 05 and Spec 06 is introduced.

## Acceptance Criteria

- All organization command endpoints MUST use `POST`.
- The web adapter MUST resolve authenticated user id before invoking application use cases.
- A missing or blank authenticated user id MUST be rejected before application use case execution.
- The web adapter MUST translate path variables and request bodies into existing application commands.
- The web adapter MUST NOT enforce domain rules itself.
- The web adapter MUST delegate all business behavior to existing application use cases.
- The web adapter MUST expose outcomes sufficient for clients to identify created resources or completed command results:
  - create group returns `groupId`
  - invite member returns `invitationId`
  - accept/reject/revoke return the resulting terminal status
- The web adapter MUST map validation, authorization, not-found, and domain/application rejections to HTTP error responses consistently.
- The web adapter MUST validate path variables and request bodies before an
  organization use case executes. A transport rejection MUST result in zero
  executions of the targeted use case.
- The web adapter MUST expose organization failure meaning only through the
  typed Spec 06 application boundary and MUST NOT inspect organization domain
  error enums or exception messages.
- Reference-core HTTP translation MUST use this matrix:

  | Failure source | HTTP status | Stable code |
  | --- | --- | --- |
  | invalid transport input | `400` | `VALIDATION_ERROR` |
  | unknown group or invitation | `404` | `NOT_FOUND` |
  | existing permission mismatch | `403` | `FORBIDDEN` |
  | unknown invitee | `400` | `APPLICATION_REJECTED` |
  | duplicate pending invitation or terminal-state rejection | `400` | `APPLICATION_REJECTED` |
  | generated group-id collision | `400` | `APPLICATION_REJECTED` |
  | unexpected failure | `500` | `INTERNAL_ERROR` |

- No existing organization failure defined by Spec 01–07 maps to
  `409 CONFLICT`.
- HTTP integration MUST NOT change persistence behavior from Spec 07.

## Error Cases

- Missing, blank, malformed, unknown, expired, invalid, revoked, or ambiguous
  `ORCA_SESSION` input -> unauthenticated request rejected under auth-09 before
  organization use-case execution.
- `X-User-Id` without an establishable session -> unauthenticated request
  rejected.
- Missing or malformed request body → validation error.
- Missing or blank required body field → `400 VALIDATION_ERROR` before use-case
  execution.
- Blank group id or invitation id → `400 VALIDATION_ERROR` before use-case
  execution.
- Unknown group id → `404 NOT_FOUND`.
- Unknown invitation id for accept, reject, or revoke → `404 NOT_FOUND`.
- Non-GroupAdmin inviter or revoker, wrong acceptor, or wrong rejector →
  `403 FORBIDDEN`.
- Unknown invitee → `400 APPLICATION_REJECTED`.
- Duplicate pending invitation, existing-member rejection, or an already
  accepted, rejected, or revoked invitation → `400 APPLICATION_REJECTED`.
- Generated group-id collision → `400 APPLICATION_REJECTED`.
- Unexpected exception → `500 INTERNAL_ERROR`.
- Changing internal exception message wording MUST NOT change any
  classification above.

## Dependency Ownership and Allowed Public Boundary

- Organization Spec 01–06 owns command behavior and typed failure meaning.
- Auth-09 owns authenticated current-user resolution before organization
  command execution.
- Reference-core-01 owns the stable HTTP response shape and translation of the
  typed category to status/code/safe message.
- The organization web adapter may construct organization value objects and
  commands, invoke organization use cases, and allow the typed Spec 06 failure
  to reach reference-core translation.
- Reference-core MUST NOT decide whether an organization rule was violated,
  import organization domain error identity, or parse exception messages.

## Public HTTP Failure Set

- Request body: absent, malformed, empty object, missing required field, null
  required field, blank required field, unsupported enum value, and unexpected
  field input where framework binding rejects it.
- Path input: absent route, blank group id, and blank invitation id.
- Application outcome: unknown group, unknown invitation, unknown invitee,
  duplicate pending invitation, existing member, terminal invitation state,
  generated id collision, unauthorized actor, and unexpected failure.
- Authentication failure remains owned by auth-09 and occurs before
  organization use-case execution.

## Verification Mapping

| Normative outcome | Verification |
| --- | --- |
| blank group/invitation id and invalid body are validation; use-case count is zero | organization controller boundary tests for create/invite/accept/reject/revoke |
| unknown group/invitation is `404 NOT_FOUND` | organization web integration tests |
| all four permission mismatches are `403 FORBIDDEN` | organization web integration tests |
| unknown invitee, duplicate pending, each terminal state, and id collision are `400 APPLICATION_REJECTED` | application and organization web integration tests |
| message wording cannot affect classification | typed failure translation test with alternate message |
| unexpected exception is safe `500 INTERNAL_ERROR` | reference-core stable error integration test |
| stable response body remains reference-core-owned | reference-core error contract tests |

## Affected and Superseded Documents

- This amendment depends on the typed boundary added to `organization-06` and
  aligns the HTTP translation in `reference-core-01`.
- It supersedes the previous ambiguous "not found or rejected according to the
  application error" wording and any message-based implementation classifier.
- Auth-09 actor establishment, Spec 01–07 behavior, routes, persistence, and
  response success payloads remain unchanged.

## Invariants

- No new domain invariants are introduced in this slice.
- All domain invariants remain exactly as defined in Spec 01–05.
- Application orchestration remains exactly as defined in Spec 06.
- Persistence behavior remains exactly as defined in Spec 07.

## Non-Goals

- Spring Security or production authentication.
- Login, registration, sessions, tokens, password handling, or identity provider integration.
- User bounded-context implementation or user persistence.
- New organization behavior, statuses, policies, or workflows.
- Query/list/read APIs for groups, members, or invitations.
- Frontend UI.
- API versioning strategy, OpenAPI generation, pagination, or filtering.
- New organization business behavior, error detail payloads, `409 Conflict`,
  logging, correlation, audit, or broad package refactoring.
