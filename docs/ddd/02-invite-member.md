# DDD Derivation — 02 Invite Member (Derived)

This note is **derived from** `docs/specs/02-invite-member.md`.
It does not introduce new behavior.
Its purpose is to make model decisions explicit and reviewable.

---

## Bounded Context

**organization**

Rationale:
- The spec defines `Group`, `GroupMember`, `GroupAdmin`, and `GroupInvitation` as core terms.
- The behavior is group-scoped membership management (roles, membership, invitations).

---

## Aggregate Root

**Group** is the aggregate root.

Why:
- Atomicity: the spec requires enforcing:
  - no duplicate PENDING invitations for the same invitee userId within a group
  - cannot invite a user who is already a group member
- These invariants are group-scoped and must be enforced within one consistency boundary.

---

## Entities / Value Objects / Enums (Minimum Set)

### Entities
- `Group` (Aggregate Root)
- `GroupInvitation` (Entity inside Group)
- `GroupMember` (Entity inside Group)

### Value Objects
- `GroupId`
- `GroupInvitationId`
- `UserId`

### Enums
- `GroupRole`
  - ADMIN (maps to `GroupAdmin` in the spec)
  - MEMBER
- `InvitationStatus`
  - PENDING (minimum for this slice; other statuses exist but are out of scope)

---

## Invariant Checklist (Mapped to the Spec)

### Domain invariants (enforced inside the Group aggregate)
- Invitee userId must be non-empty.
- Only a GroupAdmin can create an invitation for the group.
- A group cannot have more than one PENDING invitation for the same invitee userId.
- A group cannot invite a user who is already a group member.
- A PENDING invitation must always reference exactly one group.

### Application rules (require external state / repository)
- Invitee user exists (UserRepository / identity boundary).
- Unauthenticated request is rejected (security / application boundary).

---

## Notes on Placement

- Domain code must not depend on Spring/JPA/DB (see `docs/constraints.md`).
- Repository checks (e.g., invitee existence) belong to application layer, not in the aggregate.
- Notification delivery is integration and out of scope for this slice.
