# Spec 03 — Accept Invitation

## Goal

An invited registered user accepts a pending group invitation, becoming a group member with the intended role, and marking the invitation as accepted.

## Domain Terms

- Group
  A workspace that owns members and access control.

- GroupInvitation
  A pending record that represents an invitation for a registered user to join a group.

- InvitationStatus
  The state of an invitation (e.g. PENDING, ACCEPTED, REVOKED, EXPIRED).

- GroupMember
  A user who belongs to a group with exactly one role.

## Scenario

### Scenario: Invitee accepts a PENDING invitation

**Given**
- A group exists.
- A GroupInvitation exists for the invitee userId in PENDING status.
- The accepting user is the same user as the invitee userId.
- The invitee is not already a group member.

**When**
- The invitee submits an accept request containing:
  - invitation id

**Then**
- The GroupInvitation status becomes ACCEPTED.
- A new GroupMember is created for the invitee userId.
- The member role equals the invitation intended group role.
- The group no longer contains that invitation as a pending invite.

## Acceptance Criteria

- Invitation id must be non-empty.
- Only the invitee userId can accept the invitation.
- Only a PENDING invitation can be accepted.
- A group cannot accept an invitation for a user who is already a group member.
- Accepting an invitation must be atomic:
  - membership is created
  - invitation status changes to ACCEPTED

## Invariants

- Accepting an invitation must transition status from PENDING to ACCEPTED.
- An ACCEPTED invitation cannot return to PENDING.

## Error Cases

- Invitation id is empty → validation error.
- Invitation does not exist → rejected.
- Invitation status is not PENDING → rejected.
- Accepting userId does not match invitee userId → rejected.
- Invitee is already a group member → rejected.
- Unauthenticated request → rejected.

## Non-Goals

- Invitation revocation / expiration policies.
- Re-sending invitation notifications.
- Inviting non-registered users by email.
- Cross-group membership transfer.

## Out of Scope (Integration)

This spec defines core behavior only (domain + application).

The following integration concerns are intentionally out of scope and will be specified separately:

- HTTP/controller contract (endpoint, request/response mapping, status codes)
- Authentication/authorization wiring (how `inviteeUserId` is derived from user context)
- Persistence details (schema, constraints, identifier generation strategy)
- Transaction boundary and consistency strategy
- Notification delivery mechanism (email/push/etc.)
- Infrastructure integration tests
