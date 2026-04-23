# Spec 08 — Organization Web API Integration

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
  For this demo integration slice, the authenticated user id is supplied through an HTTP header.

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

For this demo slice, the web adapter resolves the current user id from:

```text
X-User-Id: user-id
```

The resolved value is passed to the application layer as the actor/creator/inviter user id.

This header is an adapter mechanism only.
It does not introduce a security model or domain rule.

## Scenarios

### Scenario: HTTP client creates a group

**Given**
- The request contains `X-User-Id`.
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
- The request contains `X-User-Id`.
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
- The request contains `X-User-Id`.
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
- The request contains `X-User-Id`.
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
- The request contains `X-User-Id`.
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
- HTTP integration MUST NOT change persistence behavior from Spec 07.

## Error Cases

- Missing or blank `X-User-Id` → unauthenticated request rejected.
- Missing or malformed request body → validation error.
- Blank required path variable or body field → validation error.
- Unknown group id → not found or rejected according to the application error.
- Unknown invitation id → not found or rejected according to the application error.
- Actor is not allowed by existing Spec 02–05 rules → rejected.
- Domain/application validation failure from Spec 01–06 → rejected.

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
