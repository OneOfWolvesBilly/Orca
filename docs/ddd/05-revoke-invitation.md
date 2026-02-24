# DDD Derivation — 05 Revoke Invitation (Derived)

## Bounded Context

- organization

Rationale:
- Existing specs define Group, GroupMember, GroupInvitation, and GroupAdmin as a cohesive membership/invitation domain.

## Aggregate Root (and why: atomicity + invariants)

- Group (Aggregate Root)

Why:
- The revoke operation must update invitation status and pending-invite tracking atomically.
- Membership and invitation rules in specs 01–04 are group-scoped, so the consistency boundary is the Group aggregate.

## Entities / Value Objects / Enums (minimum set)

Entities:
- Group
- GroupInvitation
- GroupMember

Value Objects (names are illustrative; actual naming must follow existing codebase conventions):
- GroupId
- UserId
- InvitationId

Enums:
- InvitationStatus (must include at least: PENDING, ACCEPTED, REJECTED, REVOKED)

## Invariant checklist (mapped to spec)

- [Spec] Only a PENDING invitation can be revoked.
  - Enforced by Group aggregate when processing revoke by invitation id.

- [Spec] Revocation must be initiated by a GroupAdmin.
  - Enforced by Group aggregate based on revoking user’s role within the group.

- [Spec] State transition: PENDING → REVOKED.
  - Enforced by GroupInvitation state transition rule.

- [Spec] Revoked invitation must no longer count as pending.
  - Enforced by Group aggregate’s pending-invite tracking update as part of the same operation.

- [Spec] No expiration policy.
  - No domain behavior added.

- [Spec] No re-invite policy.
  - No domain behavior added.

- [Spec] No cross-context side effects.
  - No events/side-effects required by this slice beyond group-local state change.

## Rule classification

Domain invariants (enforceable inside Group aggregate without external state):
- Only GroupAdmin can revoke an invitation for the group.
- Only PENDING invitations can be revoked.
- Revoke transition must be PENDING → REVOKED.
- REVOKED cannot return to PENDING.
- Revoked invitation is removed from pending-invite tracking (and thus does not count as pending).

Application rules (require repository/external state):
- None introduced by this slice.
  - “Invitation does not exist” is a lookup concern; the existence check is handled at application layer before calling the aggregate,
    but the aggregate still enforces transition/role rules once the invitation is loaded into the Group.