# Spec 06 — Application Integration (Organization Invitation Lifecycle Orchestration)

## Goal

Provide application-layer orchestration for the completed group invitation lifecycle
(invite / accept / reject / revoke) using only the existing domain API and rules
defined in Spec 02–05.

This slice introduces no new domain behavior, no new invariants, and no new status.

## Domain Terms

- Group
- GroupMember
- GroupAdmin
- GroupInvitation
- InvitationStatus

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

## Invariants

- No new invariants are introduced in this slice.
- Domain invariants remain owned by the existing aggregates as specified in Spec 01–05.

## Error Cases

- All error cases are exactly the union of Spec 02–05 error cases for their respective operations.
- Any request failing those rules MUST be rejected.

## Non-Goals

- New invitation statuses or policies (e.g., expiration).
- New re-invite policy.
- New domain events or audit requirements beyond what already exists.
- HTTP/API contracts, authentication wiring, persistence schema, and infrastructure concerns.
