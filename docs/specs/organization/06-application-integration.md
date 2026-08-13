# Spec 06 — Application Integration (Organization Invitation Lifecycle Orchestration)

Status: Approved / Implemented.

## Goal

Provide application-layer orchestration for the completed group invitation lifecycle
(invite / accept / reject / revoke) using only the existing domain API and rules
defined in Spec 02–05.

This slice introduces no new domain behavior, no new invariants, and no new status.

It also owns the typed application failure meaning exposed by organization use
cases. That boundary classifies existing Spec 01–05 failures without changing
the rule that caused the failure or relying on exception message text.

## Domain Terms

- Group
- GroupMember
- GroupAdmin
- GroupInvitation
- InvitationStatus
- OrganizationApplicationFailure
  - a typed application-boundary failure carrying exactly one of:
    `NOT_FOUND`, `FORBIDDEN`, or `APPLICATION_REJECTED`

(See Spec 02–05 for term definitions.)

## Scenarios

### Scenario: Application orchestrates "Invite Member"

**Given**
- The preconditions of Spec 02 hold.

**When**
- A GroupAdmin submits an invite request for a group.

**Then**
- The system enforces all acceptance criteria and error cases of Spec 02.
- The system persists the resulting group state change as a single operation.

---

### Scenario: Application orchestrates "Accept Invitation"

**Given**
- The preconditions of Spec 03 hold.

**When**
- The invitee submits an accept request with invitation id.

**Then**
- The system enforces all acceptance criteria and error cases of Spec 03.
- The system persists membership creation and invitation status change atomically.

---

### Scenario: Application orchestrates "Reject Invitation"

**Given**
- The preconditions of Spec 04 hold.

**When**
- The invitee submits a reject request with invitation id.

**Then**
- The system enforces all acceptance criteria and error cases of Spec 04.
- The system persists invitation status change and pending-invite tracking update atomically.

---

### Scenario: Application orchestrates "Revoke Invitation"

**Given**
- The preconditions of Spec 05 hold.

**When**
- A GroupAdmin submits a revoke request with invitation id.

**Then**
- The system enforces all acceptance criteria and error cases of Spec 05.
- The system persists invitation status change and pending-invite tracking update atomically.

## Acceptance Criteria

- The application layer MUST enforce all cross-entity checks required by Spec 02–05:
  - invitee user existence check (Spec 02)
  - invitation existence check by id (Spec 03–05)
  - permission/actor checks (GroupAdmin vs invitee) (Spec 02–05)
  - membership existence checks (Spec 02–04)
  - "no duplicate pending invitation per invitee per group" (Spec 02)
- Each operation MUST commit its resulting state change as a single atomic application operation,
  consistent with Spec 03–05 "atomic" requirements.
- The application layer MUST NOT introduce any behavior beyond Spec 02–05.
- Every expected organization command failure that leaves the application
  boundary MUST carry typed organization-owned failure meaning:
  - an unknown group or invitation is `NOT_FOUND`;
  - an existing actor permission mismatch is `FORBIDDEN`;
  - an unknown invitee, generated group-id collision, duplicate pending
    invitation, existing-member rejection, or terminal-state rejection is
    `APPLICATION_REJECTED`.
- Failure classification MUST NOT inspect or depend on exception message text,
  a message prefix, or message wording.
- Invalid command construction or transport values remain input validation and
  are not reclassified as application rejection by this boundary.

## Invariants

- No new invariants are introduced in this slice.
- Domain invariants remain owned by the existing aggregates as specified in Spec 01–05.

## Error Cases

- All underlying error cases remain exactly the union of Spec 01–05 error
  cases for their respective operations.
- Generated group-id collision -> typed `APPLICATION_REJECTED`.
- Unknown invitee -> typed `APPLICATION_REJECTED`.
- Unknown group -> typed `NOT_FOUND`.
- Unknown invitation for accept, reject, or revoke -> typed `NOT_FOUND`.
- Non-GroupAdmin inviter or revoker -> typed `FORBIDDEN`.
- Wrong acceptor or rejector -> typed `FORBIDDEN`.
- Duplicate pending invitation, existing-member rejection, or an already
  accepted, rejected, or revoked invitation -> typed `APPLICATION_REJECTED`.
- An unexpected dependency or programming failure MUST remain unexpected and
  MUST NOT be converted to an expected organization failure.

## Dependency Ownership and Public Boundary

- Organization owns the meaning of expected organization application
  failures.
- Spec 01–05 and the Group aggregate remain authoritative for the underlying
  business rules and status transitions.
- The application layer owns translation from existing domain error identity
  and repository/external lookup outcomes to the typed application failure.
- The allowed downstream public boundary is the typed application failure and
  its category. HTTP adapters may consume that category but MUST NOT import
  organization domain error enums, parse exception messages, or reconstruct
  organization rules.
- Reference-core owns only stable HTTP status/code/message translation as
  specified by `reference-core-01`.

## Verification Mapping

| Normative outcome | Verification |
| --- | --- |
| generated id collision and unknown invitee are `APPLICATION_REJECTED` | `CreateGroupUseCaseTest`, `InviteMemberUseCaseTest` |
| unknown group/invitation is `NOT_FOUND` | organization application use-case tests for invite/accept/reject/revoke |
| inviter/revoker/acceptor/rejector mismatch is `FORBIDDEN` | organization application use-case tests |
| duplicate and terminal-state failures are `APPLICATION_REJECTED` | organization application use-case tests |
| message wording does not determine classification | application tests assert typed category while using alternate internal messages |
| unexpected failures remain unexpected | application test with a failing dependency and reference-core HTTP integration proof |

## Affected and Superseded Documents

- This amendment aligns `organization-08` and `reference-core-01` with the
  organization-owned typed application failure boundary.
- It supersedes only untyped or message-shaped application failure signaling
  for the existing organization commands.
- Spec 01–05 domain behavior and Spec 07 persistence behavior are unchanged.

## Non-Goals

- New invitation statuses or policies (e.g., expiration).
- New re-invite policy.
- New domain events or audit requirements beyond what already exists.
- HTTP/API contracts, authentication wiring, persistence schema, and infrastructure concerns.
- New error categories, `409 Conflict` mappings, or a shared cross-context
  business failure taxonomy.
