# DDD Notes — 02 Invite Member (Derived)

## Bounded Context

- organization

Reason: the behavior concerns Group membership management (members, roles, invitations).

## Aggregate Root

- Group

Why:
- Atomicity: "no duplicate pending invites for same invitee userId" and "cannot invite existing member" must be enforced within one consistency boundary.
- Invariants: membership uniqueness and pending invite uniqueness are group-scoped.

## Entities / Value Objects / Enums (minimum set)

### Entities
- Group
  - id
  - members (userId -> role)
  - pendingInvitations (inviteeUserId -> GroupInvitation)

- GroupInvitation
  - id
  - groupId
  - inviteeUserId
  - intendedRole
  - status (PENDING/...)

### Value Objects
- GroupId
- GroupInvitationId
- UserId

### Enums
- GroupRole (minimum: GROUP_ADMIN, MEMBER)
- InvitationStatus (minimum: PENDING)

## Invariant Checklist (mapped to spec)

- Only GroupAdmin can invite
  - Enforced by Group checking inviter's role.

- No duplicate PENDING invitation for the same invitee userId
  - Enforced by Group ensuring at most one PENDING invitation keyed by inviteeUserId.

- Cannot invite a user who is already a group member
  - Enforced by Group checking membership set/map before creating an invitation.

- PENDING invitation must reference exactly one group
  - Enforced by GroupInvitation containing groupId set at creation.

## Rule Classification

### Domain invariants (pure, enforceable inside aggregate)
- Invitee userId must be non-empty.
- Inviter must be GroupAdmin of the group.
- Reject duplicate pending invitation for the same invitee userId within the group.
- Reject inviting a user who is already a group member.
- Create invitation with status PENDING and associate it with the group.

### Application rules (require repository/external checks)
- Invitee user exists (UserRepository / identity boundary).
- Unauthenticated request rejection (web/auth boundary).
