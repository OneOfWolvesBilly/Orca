# Spec 04 — Reject Invitation

## Goal

An invited registered user rejects a pending group invitation,
marking it as rejected and ensuring it no longer counts as a pending invite.

## Domain Terms

- Group
  A workspace that owns members and access control.

- GroupInvitation
  A record that represents an invitation for a registered user to join a group.

- InvitationStatus
  The state of an invitation (e.g. PENDING, ACCEPTED, REJECTED, REVOKED).

- GroupMember
  A user who belongs to a group with exactly one role.

## Scenario

### Scenario: Invitee rejects a PENDING invitation

**Given**
- A group exists.
- A GroupInvitation exists for the invitee userId in PENDING status.
- The rejecting user is the same user as the invitee userId.
- The invitee is not already a group member.

**When**
- The invitee submits a reject request containing:
  - invitation id

**Then**
- The GroupInvitation status becomes REJECTED.
- No GroupMember is created.
- The group no longer contains that invitation as a pending invite.

## Acceptance Criteria

- Invitation id must be non-empty.
- Only the invitee userId can reject the invitation.
- Only a PENDING invitation can be rejected.
- A group cannot reject an invitation for a user who is already a group member.
- Rejecting an invitation must be atomic:
  - invitation status changes to REJECTED
  - the invitation is removed from pending-invite tracking

## Invariants

- Rejecting an invitation must transition status from PENDING to REJECTED.
- A REJECTED invitation cannot return to PENDING.

## Error Cases

- Invitation id is empty → validation error.
- Invitation does not exist → rejected.
- Invitation status is not PENDING → rejected.
- Rejecting userId does not match invitee userId → rejected.
- Invitee is already a group member → rejected.
- Unauthenticated request → rejected.

## Non-Goals

- Invitation revocation / expiration policies.
- Re-sending invitation notifications.
- Inviting non-registered users by email.
- Cross-group membership transfer.
- Re-inviting a rejected user.

## Out of Scope (Integration)

This spec defines core behavior only (domain + application).

The following integration concerns are intentionally out of scope and will be specified separately:

- HTTP/controller contract (endpoint, request/response mapping, status codes)
- Authentication/authorization wiring (how `inviteeUserId` is derived from user context)
- Persistence details (schema, constraints, identifier generation strategy)
- Transaction boundary and consistency strategy
- Notification delivery mechanism (email/push/etc.)
- Infrastructure integration tests
