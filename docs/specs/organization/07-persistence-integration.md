# Spec 07 — Persistence Integration (Organization)

## Goal

Provide durable persistence for the organization context so that all existing behaviors
defined in Spec 01–06 can be executed across process restarts without changing any specified behavior.

This slice introduces no new domain behavior, no new invariants, and no new status.

## Domain Terms

- Group  
  The aggregate root that owns members and invitations.

- GroupId  
  Identifier of a Group.

- GroupMember  
  A member of a Group.

- GroupInvitation  
  An invitation that can be accepted/rejected/revoked.

- GroupInvitationId  
  Identifier of a GroupInvitation.

- InvitationStatus  
  One of: `PENDING`, `ACCEPTED`, `REJECTED`, `REVOKED` (as already defined in Spec 02–05).

- GroupRepository  
  The persistence port used by the application layer:
  - `findById(GroupId)`
  - `findByInvitationId(GroupInvitationId)`
  - `indexInvitation(GroupInvitationId, GroupId)`
  - `save(Group)`

## Scenarios

### Scenario: Persist and reload a Group aggregate by id

**Given**
- A Group exists and its state is the result of executing only Spec 01–06 behaviors.

**When**
- The application persists the Group and later reloads it by `GroupId`.

**Then**
- The reloaded Group contains sufficient state to continue executing Spec 02–06 flows:
  - members can be validated and updated
  - invitations can be validated and transitioned
- Invitation status values are preserved exactly as previously persisted:
  - `PENDING`, `ACCEPTED`, `REJECTED`, `REVOKED`
- No additional behavior is introduced during reload.

---

### Scenario: Resolve a GroupId by invitation id for invitation lifecycle flows

**Given**
- A Group contains an invitation with a `GroupInvitationId` produced by Spec 02.
- The system has persisted the Group state.

**When**
- The application receives a request that references only the invitation id
  (accept / reject / revoke as in Spec 03–05).

**Then**
- The system can resolve the correct Group using `findByInvitationId(invitationId)`.
- The resolved Group is the same Group that originally produced that invitation id.
- The system can proceed to enforce all rules from Spec 03–05 without requiring additional behavior.

---

### Scenario: Persist invitation id → group id lookup updates consistently with Group state changes

**Given**
- A Group state change is produced by a Spec 02–05 command
  (invite / accept / reject / revoke) and orchestrated by Spec 06.

**When**
- The application commits the change through persistence.

**Then**
- The Group aggregate state and the invitation id → group id lookup updates are committed as one atomic application operation,
  consistent with Spec 03–06 atomic requirements.
- Subsequent reads (by group id or invitation id) observe a consistent state that does not violate Spec 01–05 invariants.

## Acceptance Criteria

- Persistence MUST be sufficient to reconstitute Group aggregate state needed by Spec 01–06:
  - members
  - invitations
  - invitation statuses
  - any identifiers needed by existing rules
- `findByInvitationId(invitationId)` MUST work for invitation lifecycle flows (Spec 03–05) after a restart.
- Persistence MUST NOT change any specified behavior in Spec 01–06.
- Persistence MUST NOT introduce new statuses, transitions, or invariants.
- Domain code MUST remain independent of persistence technology and schemas (see `docs/constraints.md`).
- Schema ownership is external to the domain and is managed explicitly (no implicit schema mutation).

## Invariants

- No new invariants are introduced in this slice.
- All invariants remain exactly as defined in Spec 01–05.

## Error Cases

- Reloading a Group that does not exist by `GroupId` → not found.
- Resolving a Group by an unknown invitation id → not found.
- If persisted data cannot be reconstituted into a valid Group that satisfies existing invariants → rejected as invalid persisted state.

## Non-Goals

- New behavior (expiration, re-invite policy, reminders, etc.).
- New domain events or audit requirements beyond Spec 01.
- Any changes to Spec 01–06 scenarios, acceptance criteria, or error cases.
- Cross-bounded-context persistence coupling or shared-database assumptions.
- Query projections, reporting models, caching, or performance optimizations.
- API/HTTP contracts and authentication wiring.