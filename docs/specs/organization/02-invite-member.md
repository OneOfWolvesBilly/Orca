# Spec 02 — Invite Member

## Goal

A GroupAdmin invites an existing registered user to join a group, creating a pending invitation that can later be accepted.

## Domain Terms

- Group
  A workspace that owns members and access control.

- GroupAdmin
  A group role that is allowed to manage membership.

- GroupMember
  A user who belongs to a group with exactly one role.

- GroupInvitation
  A pending record that represents an invitation for a registered user to join a group.

- InvitationStatus
  The state of an invitation (e.g. PENDING, ACCEPTED, REVOKED, EXPIRED).

## Scenario

### Scenario: GroupAdmin invites a registered user by userId

**Given**
- A group exists.
- The inviter is a member of the group with the GroupAdmin role.
- The invitee is a registered user.

**When**
- The inviter submits an invite request containing:
  - invitee userId
  - intended group role for the invitee

**Then**
- A new GroupInvitation is created in PENDING status.
- The invitation is associated with the group.
- The invitation targets the invitee userId and includes the intended group role.
- The group now contains that invitation as a pending invite.

## Acceptance Criteria

- Invitee userId must be non-empty.
- Only a GroupAdmin can create an invitation for the group.
- A group cannot have more than one PENDING invitation for the same invitee userId.
- A group cannot invite a user who is already a group member.

## Invariants

- A user cannot be assigned multiple roles within the same group.
- Group membership cannot exist without a group.
- A PENDING invitation must always reference exactly one group.

## Error Cases

- Invitee userId is empty → validation error.
- Inviter is not a GroupAdmin of the group → rejected.
- Invitee already has a PENDING invitation in the same group → rejected.
- Invitee is already a group member → rejected.
- Invitee user does not exist → rejected.
- Unauthenticated request → rejected.

## Non-Goals

- Inviting non-registered users by email.
- Invitation acceptance flow (joining the group).
- Invitation revocation / expiration policies.
- Sending email delivery or templates.
- Cross-group invitations or bulk invites.

## Out of Scope (Integration)

This spec defines core behavior only (domain + application).

The following integration concerns are intentionally out of scope and will be specified separately:

- HTTP/controller contract (endpoint, request/response mapping, status codes)
- Authentication/authorization wiring (how `inviterUserId` is derived from user context)
- Persistence details (schema, constraints, identifier generation strategy)
- Transaction boundary and consistency strategy
- Notification delivery mechanism (email/push/etc.)
- Infrastructure integration tests
