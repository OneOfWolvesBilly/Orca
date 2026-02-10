# DDD Derivation — 01 Create Group

This note is **derived from** `docs/specs/01-create-group.md`.
It does not introduce new behavior.
Its purpose is to make model decisions explicit and reviewable.

---

## Bounded Context

**organization**

Rationale:
- The spec defines `Group`, `GroupMember`, and `GroupAdmin` as core terms.
- These concepts establish ownership boundaries and membership rules.

---

## Aggregate Root

**Group** is the aggregate root.

Why:
- Creation must be **atomic**: group creation + initial membership + role assignment succeed or fail together.
- Invariants in the spec are centered on a single group boundary:
  - a group must always have at least one admin
  - membership cannot exist without a group
  - a user cannot hold multiple roles within the same group

Atomicity and invariant enforcement belong inside one aggregate boundary.

---

## Entities / Value Objects / Enums (Minimum Set)

### Entities
- `Group` (Aggregate Root)
- `GroupMember` (Entity inside Group)

### Value Objects
- `GroupName` (non-empty validation)
- `GroupDescription` (optional)
- `GroupId` (identifier; uniqueness requires repository check)

### Enums
- `GroupRole`
  - `ADMIN` (maps to `GroupAdmin` in the spec)

---

## Invariant Checklist (Mapped to the Spec)

### Domain invariants (enforced inside the Group aggregate)
- A group must always have at least one admin.
- A user cannot be assigned multiple roles within the same group.
- Group membership cannot exist without a group.
- At creation time, the group has exactly one member.
- At creation time, the creator is always an admin.

### Application rules (require external state / repository)
- Group identifier must be unique.
- Unauthenticated request is rejected (security / application boundary).
- Auditable event recording (side-effect outside pure domain).

---

## Creation Command Shape (Conceptual)

A creation request provides:
- `name` (required)
- `description` (optional)

The system produces a new Group aggregate containing exactly one member:
- member userId = creator
- role = ADMIN

---

## Notes on Placement

- Domain code must not depend on Spring/JPA/DB (see `docs/constraints.md`).
- Repository checks (e.g., id uniqueness) belong to application layer, not in the aggregate.
- Audit/event persistence is infrastructure. The domain may emit an in-memory domain event,
  but recording is not a domain invariant.

