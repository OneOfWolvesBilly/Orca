# DDD Notes 06 — Application Integration (Organization Invitation Lifecycle Orchestration)

Status: Approved / Implemented.

## Bounded Context

- organization
  - Rationale: All concepts involved are Group, membership, roles, and invitations.

## Aggregate Root

- Group (aggregate root)
  - Owns:
    - members (GroupMember)
    - invitations (GroupInvitation)
  - Rationale:
    - All lifecycle operations (invite/accept/reject/revoke) mutate Group-owned state.
    - Atomicity requirements in Spec 03–05 are naturally satisfied by committing a single Group aggregate update.

## Entities / Value Objects / Enums (minimum set)

Entities (inside Group aggregate):
- Group
- GroupMember
- GroupInvitation

Value Objects (as implied by specs; naming may already exist in code):
- GroupId
- UserId
- InvitationId
- GroupRole (includes GroupAdmin and other member roles)

Enum:
- InvitationStatus (PENDING, ACCEPTED, REJECTED, REVOKED) — must NOT add new values.

## Invariant Checklist (mapped to Specs)

From Spec 01:
- Group must always have at least one GroupAdmin.
- A user cannot be assigned multiple roles within the same group.
- Group membership cannot exist without a group.

From Spec 02:
- At most one PENDING invitation per invitee userId per group.
- Cannot invite an existing group member.

From Spec 03:
- Accept transitions PENDING -> ACCEPTED only.
- ACCEPTED cannot return to PENDING.

From Spec 04:
- Reject transitions PENDING -> REJECTED only.
- REJECTED cannot return to PENDING.

From Spec 05:
- Revoke transitions PENDING -> REVOKED only.
- REVOKED cannot return to PENDING.
- Revoked invitation must no longer count as pending.

## Rule Classification

### Domain invariants (enforceable within Group aggregate)
- Status transition constraints:
  - PENDING -> ACCEPTED / REJECTED / REVOKED only as defined by Spec 03–05.
  - Terminal statuses cannot return to PENDING (Spec 03–05).
- "No duplicate pending invitation per invitee per group" (Spec 02):
  - If invitations are stored inside Group, the aggregate can prevent inserting a second PENDING for same invitee.
- "Cannot invite a user who is already a group member" (Spec 02):
  - If membership list is inside Group, aggregate can block it.
- Membership uniqueness / single role per user per group (Spec 01, Spec 02).

### Application rules (require repository/external state)
These are checks that need loading data and/or verifying existence outside the aggregate boundary:

- Invitee user existence (Spec 02):
  - Requires querying the user registry (outside Group aggregate).
- Invitation existence by invitation id (Spec 03–05):
  - Requires loading the correct Group that contains that invitation.
  - The lookup itself is an application responsibility (repository query).
- Actor identity and permission source (Spec 02–05):
  - "Only GroupAdmin can invite/revoke" requires:
    - loading Group aggregate
    - checking actor's membership role inside Group
  - "Only invitee can accept/reject" requires:
    - loading Group aggregate
    - verifying actor userId equals invitation.inviteeUserId inside Group
- Atomic commit boundary (Spec 03–05):
  - Enforced by application service transaction boundary around a single Group save.

## Application Service Shape (non-authoritative guidance)

- Provide four use case entry points matching Spec 02–05:
  - InviteMember
  - AcceptInvitation
  - RejectInvitation
  - RevokeInvitation

Responsibilities (application layer):
1) Load Group aggregate by groupId OR by invitationId (depending on operation).
2) (Invite) Verify invitee user exists via User lookup.
3) Invoke existing domain methods on Group (no reconstitution bypass).
4) Persist updated Group as one atomic operation.

No new behavior is allowed beyond Spec 02–05.

## Typed Application Failure Boundary

Organization owns one narrow application failure contract for expected command
failures. It carries a typed category rather than requiring downstream adapters
to inspect exception messages:

- `NOT_FOUND` for an unknown group or invitation;
- `FORBIDDEN` for the existing GroupAdmin or invitee identity mismatch rules;
- `APPLICATION_REJECTED` for unknown invitee, generated id collision,
  duplicate/existing-member rejection, and terminal invitation state.

The application service translates repository lookup outcomes and existing
`DomainError` identities into this boundary. Domain exceptions remain internal
to organization; their messages remain diagnostic text and are not a public
classification contract. Invalid value-object or command construction remains
input validation, while an unexpected dependency/programming exception passes
through unchanged.

Allowed consumer:

- the reference-core HTTP adapter may depend on the application failure type
  and category only.

Forbidden consumer behavior:

- importing `DomainError` into reference-core;
- parsing a failure message or prefix;
- loading repositories or re-evaluating Group rules at the HTTP boundary.

## Test Layer Placement for the Repair

- Application tests assert each category for generated-id collision, unknown
  invitee, lookup absence, permission mismatch, duplicate/terminal state, and
  unexpected dependency failure.
- Domain tests remain authoritative for the unchanged business rules and state
  transitions.
- Web tests assert only stable HTTP translation and transport short-circuiting.
