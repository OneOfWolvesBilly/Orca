# Spec 01 — Create Group

## Goal

A registered user creates a new group and becomes its administrator.

This is the first ownership-establishing action in the system.

## Domain Terms

- User  
  An authenticated identity in the system.

- Group  
  A collaboration boundary that owns projects and members.

- GroupAdmin  
  A role granting full administrative control over a group.

## Scenario

### Scenario: User creates a group

**Given**
- A user is registered and authenticated.
- The user does not need any existing group membership.

**When**
- The user submits a request to create a group with:
  - a name
  - an optional description

**Then**
- A new group is created.
- The user becomes a member of the group.
- The user is assigned the GroupAdmin role for that group.
- The group has exactly one member at creation time.
- The operation is recorded as an auditable event.

## Acceptance Criteria

- Group name must be non-empty.
- Group identifier must be unique.
- The creator must always be a GroupAdmin.
- Group creation is atomic:
  - group creation and membership assignment succeed or fail together.

## Invariants

- A group must always have at least one GroupAdmin.
- A user cannot be assigned multiple roles within the same group.
- Group membership cannot exist without a group.

## Error Cases

- Creating a group with an empty name → validation error.
- Creating a group with a duplicate identifier → rejected.
- Unauthenticated request → rejected.

## Non-Goals

- Inviting additional members.
- Managing projects.
- Permission delegation beyond initial admin role.

These behaviors will be specified in separate specs.

## Out of Scope (Integration)

This spec defines core behavior only (domain + application).
The following integration concerns are intentionally out of scope here and will be specified separately:

- HTTP/controller contract (endpoint, request/response mapping, status codes)
- Authentication/authorization wiring (how `creatorUserId` is derived from user context)
- Persistence details (schema, constraints, identifier generation strategy)
- Transaction boundary and consistency strategy for persistence + audit
- Audit event storage mechanism (table/outbox/log/etc.)
- Spring Boot integration tests for adapters/infrastructure
