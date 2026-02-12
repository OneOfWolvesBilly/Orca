# Spec 03 — Accept Invitation

## Goal

A registered user accepts a PENDING GroupInvitation and becomes a GroupMember of the group with the intended role.

## Domain Terms

- GroupInvitation
  A pending record that represents an invitation for a registered user to join a group.

- InvitationStatus
  The state of an invitation (PENDING, ACCEPTED, REVOKED, EXPIRED).

- GroupMember
  A user who belongs to a group with exactly one role.

## Scenario

### Scenario: Invitee accepts a PENDING invitation

**Given**
- A group exists.
- A GroupInvitation exists in PENDING status.
- The invitation references:
  - exactly one group
  - exactly one invitee userId
  - exactly one intended group role.
- The accepting user is the same user as the invitee userId.
- The user is not already a member of the group.

**When**
- The invitee submits an accept invitation request for that invitation.

**Then**
- The GroupInvitation status becomes ACCEPTED.
- A new GroupMember is created in the group.
- The GroupMember userId equals the invitation invitee userId.
- The GroupMember role equals the invitation intended group role.
- The group now contains the new member.
- The invitation is no longer PENDING.

## Acceptance Criteria

- Only the invitee userId can accept the invitation.
- Only a PENDING invitation can be accepted.
- A user cannot accept an invitation if already a group member.
- Accepting an invitation is atomic:
  - membership is created
  - invitation status changes to ACCEPTED.

## Invariants

- A user cannot have multiple roles within the same group.
- A GroupInvitation in ACCEPTED status cannot return to PENDING.
- A GroupMember must always belong to exactly one group.
- Invitation must reference exactly one group.

## Error Cases

- Invitation does not exist → rejected.
- Invitation status is not PENDING → rejected.
- Accepting userId does not match invitee userId → rejected.
- User is already a group member → rejected.
- Unauthenticated request → rejected.

## Non-Goals

- Invitation revocation logic.
- Invitation expiration policy.
- Re-accepting an already ACCEPTED invitation.
- Cross-group membership transfer.
- Audit/event streaming integration.
