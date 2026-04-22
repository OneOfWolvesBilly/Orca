# DDD Note 08 — Organization Web API Integration

This note is derived from `docs/specs/08-web-api-integration.md`.
It does not introduce new behavior.

## Bounded Context

- **organization**

Rationale:
- The web adapter exposes existing organization commands only.
- All exposed behavior is already defined by Spec 01–07.
- No new bounded context is introduced.

## Aggregate Root

- **Group** (aggregate root)

Rationale:
- The HTTP endpoints only route requests to existing application use cases.
- The underlying behavior still mutates or validates the Group aggregate:
  - create group
  - invite member
  - accept invitation
  - reject invitation
  - revoke invitation
- The web layer does not own any aggregate or invariant.

## Web Adapter Boundary

The web adapter is an inbound adapter for the organization application layer.

Responsibilities:
- parse HTTP path variables
- parse JSON request bodies
- resolve authenticated user id from the request
- construct existing application commands
- invoke existing use cases
- serialize command results
- map expected failures to HTTP error responses

The web adapter MUST NOT:
- enforce business rules
- inspect or mutate aggregate internals
- bypass application use cases
- call repository adapters directly
- introduce behavior not specified by Spec 01–07

## Authenticated User Adapter

For this slice, authenticated user id resolution is modeled as a web adapter concern.

Implementation policy:
- Resolve the actor from the `X-User-Id` HTTP header.
- Reject missing or blank header values before invoking a use case.
- Pass the resolved user id into existing application commands.

This is a demo integration mechanism.
It is not a production authentication or authorization design.

## Endpoint to Use Case Mapping

| Endpoint | Use case | Actor source |
| --- | --- | --- |
| `POST /api/groups` | `CreateGroupUseCase` | `X-User-Id` as creator |
| `POST /api/groups/{groupId}/invitations` | `InviteMemberUseCase` | `X-User-Id` as inviter |
| `POST /api/group-invitations/{invitationId}/accept` | `AcceptInvitationUseCase` | `X-User-Id` as invitee actor |
| `POST /api/group-invitations/{invitationId}/reject` | `RejectInvitationUseCase` | `X-User-Id` as invitee actor |
| `POST /api/group-invitations/{invitationId}/revoke` | `RevokeInvitationUseCase` | `X-User-Id` as revoking actor |

## Rule Classification

### Domain invariants

Unchanged from Spec 01–05.
The Group aggregate continues to enforce all aggregate-local invariants.

### Application rules

Unchanged from Spec 06.
Application use cases continue to coordinate:
- repository loading
- invitee existence checks
- invocation of aggregate methods
- persistence of state changes

### Web adapter rules

Rules introduced by this slice are transport-boundary rules only:
- HTTP methods and paths
- JSON request/response shape
- authenticated user id extraction from `X-User-Id`
- request mapping into application commands
- HTTP error mapping

These are not domain rules.

## Error Mapping Policy

Recommended HTTP mapping:

- Missing or blank `X-User-Id` → `401 Unauthorized`
- Malformed JSON or missing required body → `400 Bad Request`
- Blank required body field or path variable → `400 Bad Request`
- Unknown group id or invitation id → `404 Not Found`
- Actor fails Spec 02–05 permission/identity checks → `403 Forbidden`
- Other domain/application validation failures → `400 Bad Request`

The mapping layer may inspect exception categories or messages only to choose HTTP status.
It MUST NOT re-evaluate domain rules.

## Spring Wiring Boundary

This slice may introduce minimal Spring Boot web wiring:
- application entry point
- controller
- request/response DTOs
- configuration that wires use cases to existing ports/adapters
- web integration tests

Spring annotations and HTTP concerns MUST remain outside:
- `organization/domain`
- `organization/application`

If adapter beans wrap existing persistence or in-memory implementations,
the wrapping MUST not change behavior.

## Test Layer Placement

This slice is validated with web/integration tests.

Recommended tests:
- create group endpoint resolves `X-User-Id` and returns `groupId`
- invite endpoint maps path/body/header to `InviteMemberUseCase` and returns `invitationId`
- accept/reject/revoke endpoints resolve invitation id and actor header
- missing `X-User-Id` is rejected before use case execution
- non-POST access is not part of the command contract
- representative application/domain failures are mapped to stable HTTP errors

Domain tests remain unchanged.
Application tests remain unchanged except where wiring defects are discovered.
Persistence tests remain unchanged except where web integration requires minimal bootstrapping.

## Non-Goals / Out of Scope

- Spring Security or production authentication.
- User registration/login/session/token flows.
- New user bounded context.
- Query/read model APIs.
- Frontend implementation.
- OpenAPI generation or API versioning.
- Changing any Spec 01–07 behavior.
