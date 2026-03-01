# DDD Note 07 — Persistence Integration (Organization)

## Bounded Context

- **organization**

This slice is strictly an infrastructure concern for persisting and reloading the existing organization model
without changing Spec 01–06 behavior.

## Aggregate Root

- **Group** (aggregate root)

Rationale:
- Group owns the invariants already defined in Spec 01–05:
  - group must have at least one admin
  - membership uniqueness rules
  - invitation status transition rules
- Spec 03–06 require atomic persistence of membership creation and invitation status changes.
  Persisting Group as the unit-of-work aligns with those atomicity requirements.

## Persistence Model Boundaries

### 1) Aggregate persistence boundary (authoritative)

- Persist **one Group aggregate** as the authoritative source of:
  - Group identity and core fields
  - Group members (including role)
  - Group invitations (including invitee + status + invitation id)

The persistence model MUST be capable of restoring the full aggregate state required to continue Spec 02–06 flows.

### 2) Invitation lookup boundary (derived index)

- The invitation id → group id lookup is an **infrastructure index** used to support Spec 03–05 entry by invitation id:
  - `GroupRepository.findByInvitationId(GroupInvitationId)` MUST resolve the owning Group.

This lookup is NOT a domain concept and MUST NOT introduce new rules.
It exists solely to locate the Group aggregate efficiently and consistently.

## Mapping Policy (Repository Adapter + EntityMapper)

### Design choice (Option 2)

- Implement persistence as:
  - **Repository Adapter** that implements the existing `GroupRepository` contract
  - **EntityMapper** that maps between:
    - Domain objects (Group aggregate + value objects)
    - Persistence entities (storage-friendly representation)

### Mapping rules (MUST)

- Domain types remain pure Java and MUST NOT depend on Spring/JPA/DB schemas (per `docs/constraints.md`).
- Identifiers (`GroupId`, `GroupInvitationId`, `UserId`) are stored as stable scalar values and rehydrated back into value objects.
- Enum values such as `InvitationStatus` are stored and restored using their canonical names:
  - `PENDING`, `ACCEPTED`, `REJECTED`, `REVOKED`
- EntityMapper MUST be deterministic and side-effect free:
  - mapping does not perform validation beyond structural conversion
  - mapping does not “fix” or “auto-correct” domain state

### Reconstitution policy alignment

- Aggregate reconstitution MUST NOT become a domain behavior shortcut.
- Rehydration is owned by the repository adapter:
  - the adapter reconstructs the aggregate and ensures invariants are validated during reconstitution
  - application/web layers MUST NOT use any reconstitution APIs to bypass domain rules

(If a reconstitution method exists today, it remains repository/tests-only, and later can be removed from the aggregate API per policy.)

## Rule Classification

- **Domain invariants:** unchanged; still enforced by the Group aggregate (Spec 01–05).
- **Application rules:** unchanged; orchestration remains in the application layer (Spec 06).
- **Infrastructure responsibilities introduced in this slice:**
  - durable storage of Group aggregate state
  - invitation id → group id lookup maintenance
  - transactional commit of Group state + lookup updates

No new rules are introduced.

## Test Layer Placement (Infrastructure Tests)

This slice is validated at the **infrastructure test layer**, not domain tests.

Recommended infra tests (MUST map to Spec 07 AC, without asserting new behavior):
- Persist a Group produced via Spec 01–06 flows, reload by `GroupId`, and verify the reloaded state supports:
  - membership checks
  - invitation existence + status transition preconditions
- Persist invitation index and verify `findByInvitationId(invitationId)` resolves the correct Group.
- Verify atomicity boundary at persistence layer:
  - Group state and invitation index updates are committed together or not at all.

Notes:
- Domain tests remain pure Java and unchanged.
- Infrastructure tests may use the persistence technology (e.g., JPA) and Flyway-managed schema,
  but MUST NOT leak those concerns into domain code.

## Non-Goals / Out of Scope (DDD)

- Introducing new aggregates or splitting Group into multiple persistence roots.
- Cross-context joins or shared tables between bounded contexts.
- Read-model projections for UI or reporting.
- Performance tuning or caching policies.