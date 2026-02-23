# DDD — 04 Reject Invitation (Derived)

## Bounded Context

- organization

Why:
- Group membership and invitations are already modeled under this bounded context in code structure,
  and prior specs (01..03) operate on Group as the behavioral owner.

## Aggregate Root

- Group

Why:
- The invariants and state transitions in this slice are group-scoped:
  - invitation state transition (PENDING -> REJECTED)
  - authorization within group invitation (invitee identity match)
  - removal from "pending invite" tracking
- These changes must be consistent within a single atomic state update of Group.

## Entities

- GroupInvitation
- GroupMember

## Value Objects / Enums (minimum set)

- GroupInvitationId
- UserId
- InvitationStatus
  - existing: PENDING, ACCEPTED
  - added by this slice: REJECTED

## Invariant checklist (mapped to spec)

- [AC] Invitation id must be non-empty.
- [AC] Only invitee userId can reject the invitation.
- [AC] Only a PENDING invitation can be rejected.
- [AC] A group cannot reject an invitation for a user who is already a group member.
- [AC] Rejecting an invitation must be atomic:
  - invitation status changes to REJECTED
  - the invitation is removed from pending-invite tracking
- [INV] Rejecting an invitation must transition status from PENDING to REJECTED.
- [INV] A REJECTED invitation cannot return to PENDING.
- [BASELINE] Group must always have at least one GroupAdmin.

## Rule Classification

### Domain invariants (pure, aggregate-local)

- Invitation must exist in the loaded Group aggregate to be rejected.
- Rejector must match invitation invitee userId.
- Invitation status must be PENDING.
- Rejecting user must not already be a GroupMember.
- Transition must update invitation status to REJECTED and remove it from pending tracking in one state change.
- REJECTED cannot transition back to PENDING.
- Group must always have at least one GroupAdmin.

### Application rules

- None for this slice.
