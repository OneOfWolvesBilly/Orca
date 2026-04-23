# DDD — 03 Accept Invitation

## Bounded Context

organization

## Aggregate Root

Group

Why:
- Group owns membership and invitations vocabulary (Specs 01-02).
- Acceptance must be atomic: create member + transition invitation status (Spec 03 AC).
- Membership/role uniqueness invariant lives at group boundary (Specs 01-02).

## Entities

- Group (aggregate root)
- GroupMember
- GroupInvitation

## Value Objects / Enums

- UserId
- GroupRole
- InvitationStatus
- GroupInvitationId (or equivalent identifier)

## Invariant Checklist (mapped to spec)

- Only invitee userId can accept → enforced by Group when handling accept.
- Only PENDING can be accepted → enforced by InvitationStatus transition rule.
- Cannot accept if already member → enforced by Group membership set.
- Atomic accept → one aggregate method performs both updates.
- Group must always have at least one GroupAdmin → already established by Spec 01; 03 must not break it.

## Rule Classification

### Domain invariants
- Invitation exists and is PENDING.
- Accepting userId matches invitee userId.
- Accepting user is not already a member.
- Membership role uniqueness inside group.
- Transition PENDING → ACCEPTED only.

### Application rules
- None for this slice.
