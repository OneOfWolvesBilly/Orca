# DDD — 03 Accept Invitation

## Bounded Context

organization

## Aggregate Root

Group

Reason:
- Group owns members.
- Group owns invitations.
- Membership invariant lives inside group boundary.
- Avoid cross-aggregate transaction between Invitation and Membership.

## Entities

- Group (aggregate root)
- GroupMember
- GroupInvitation

## Value Objects

- UserId
- GroupRole
- InvitationStatus

## Aggregate Responsibilities

Group must enforce:

1. Invitation exists.
2. Invitation belongs to this group.
3. Invitation is PENDING.
4. Accepting userId equals invitation invitee userId.
5. User is not already member.
6. No duplicate roles per user.
7. Membership creation + status transition atomic.

## Proposed Aggregate Method

public void acceptInvitation(
    InvitationId invitationId,
    UserId acceptingUserId
)

### Behavior inside aggregate

1. locate invitation
2. assert status == PENDING
3. assert invitation.inviteeUserId == acceptingUserId
4. assert user not already member
5. create new GroupMember with intended role
6. mark invitation as ACCEPTED

All invariant enforcement must happen inside Group.

## State Transition

PENDING → ACCEPTED

Other transitions not handled in this slice.

## Consistency Model

Strong consistency within aggregate boundary.
No cross-context behavior allowed.

## Domain Errors (explicit)

- InvitationNotFound
- InvitationNotPending
- InvitationNotOwnedByUser
- UserAlreadyMember
