# Spec 03 — HTTP Request Current User Context Access

## Goal

Expose the current user context established by Spec 02 as request-scoped input
for downstream protected HTTP command behavior within the same request.

This slice ensures the HTTP auth boundary establishes current user context once
and downstream web adapters consume that established context without remapping
presented request identities again.

## Domain Terms

- Current User Context
  The per-operation auth context that supplies the authenticated user's userId
  to downstream behavior.

- Protected HTTP Command Behavior
  An HTTP command operation that requires an authenticated actor and must not
  execute without current user context.

- Downstream Web Adapter
  The HTTP adapter code that maps a protected request into an existing
  bounded-context application command after auth context has been established.

## Scenario

### Scenario: Protected request consumes the already-established current user context

**Given**
- A protected HTTP command request is about to execute.
- The auth boundary has established current user context for that request using
  Spec 02.

**When**
- Downstream web adapter code needs the authenticated actor for that same
  request.

**Then**
- The downstream web adapter receives the already-established current user
  context for the request.
- The downstream web adapter does not remap presented request identities into
  auth context again for that request.
- The authenticated user id remains the same for the full request.

## Acceptance Criteria

- For a protected HTTP command request, current user context MUST be established
  before downstream web adapter command mapping executes.
- After current user context is established for a request, downstream web
  adapter code MUST be able to consume that established context directly within
  the same request.
- Downstream web adapter code MUST NOT re-establish current user context from
  presented request identities again for that same request.
- If current user context cannot be established, downstream protected command
  behavior MUST NOT execute.
- This slice MUST NOT add or change downstream business behavior.

## Invariants

- No new auth invariants are introduced.
- The invariants from Spec 01 remain the source of truth for current user
  context.
- The HTTP establishment rules from Spec 02 remain the source of truth for how
  current user context is created from the request.

## Error Cases

- Current user context cannot be established for the request under Spec 02 →
  protected command request is rejected as unauthenticated.
- Downstream protected command behavior attempts to execute without established
  current user context for the request → rejected.

## Non-Goals

- Login.
- Registration.
- Logout.
- Session management.
- Token issuance or refresh.
- OAuth or external identity provider flows.
- Password reset or credential recovery.
- Authorization rules for specific bounded contexts such as GroupAdmin checks.
- New request identity transport mechanisms beyond the existing Spec 02 HTTP
  contract.
