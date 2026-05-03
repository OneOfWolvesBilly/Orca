# Spec 05 — Revoke Invitation

## Goal

A GroupAdmin revokes a pending group invitation,
marking it as revoked and ensuring it no longer counts as a pending invite.

## Domain Terms

- Group
  A workspace that owns members and access control.

- GroupAdmin
  A group role that is allowed to manage membership.

- GroupInvitation
  A record that represents an invitation for a registered user to join a group.

- InvitationStatus
  The state of an invitation (e.g. PENDING, ACCEPTED, REJECTED, REVOKED).

## Scenario

### Scenario: GroupAdmin revokes a PENDING invitation

**Given**
- A group exists.
- The revoking user is a member of the group with the GroupAdmin role.
- A GroupInvitation exists in the group in PENDING status.

**When**
- The GroupAdmin submits a revoke request containing:
  - invitation id

**Then**
- The GroupInvitation status becomes REVOKED.
- The group no longer contains that invitation as a pending invite.
- The revoked invitation does not count as pending.

## Acceptance Criteria

- Invitation id must be non-empty.
- Only a GroupAdmin can revoke an invitation for the group.
- Only a PENDING invitation can be revoked.
- Revoking an invitation must be atomic:
  - invitation status changes to REVOKED
  - the invitation is removed from pending-invite tracking
- There is no expiration policy.
- There is no re-invite policy.

## Invariants

- Revoking an invitation must transition status from PENDING to REVOKED.
- A REVOKED invitation cannot return to PENDING.
- A revoked invitation must no longer count as a pending invite.

## Error Cases

- Invitation id is empty → validation error.
- Invitation does not exist → rejected.
- Revoking user is not a GroupAdmin of the group → rejected.
- Invitation status is not PENDING → rejected.
- Unauthenticated request → rejected.

## Non-Goals

- Invitation acceptance flow.
- Invitation rejection flow.
- Invitation expiration policies.
- Re-inviting a revoked invitation target.
- Sending notification messages.

## Out of Scope (Integration)

This spec defines core behavior only (domain + application).

The following integration concerns are intentionally out of scope and will be specified separately:

- HTTP/controller contract (endpoint, request/response mapping, status codes)
- Authentication/authorization wiring (how `revokingUserId` is derived from user context)
- Persistence details (schema, constraints, identifier generation strategy)
- Transaction boundary and consistency strategy
- Notification delivery mechanism (email/push/etc.)
- Infrastructure integration tests
