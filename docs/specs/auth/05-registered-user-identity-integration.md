# Spec 05 - Registered User Identity Integration

## Goal

Define auth-owned registered user identity as the shared source for authenticated
actors and cross-context registered-user checks.

This slice connects auth and organization without introducing login, sessions,
tokens, registration, or credential verification.

## Domain Terms

- Registered User Identity
  An auth-owned user identity that the system recognizes as an existing user.

- Authenticated User Id
  The user id carried by current user context after the auth boundary accepts a
  presented identity.

- Registered User Check
  A cross-context application check that asks whether a user id belongs to an
  existing registered user.

## Scenarios

### Scenario: Protected request establishes context only for a registered identity

**Given**
- A protected HTTP command request presents one authenticated user id.
- Auth has registered user identity state.

**When**
- The auth boundary establishes current user context for the request.

**Then**
- The presented user id must belong to a registered user identity.
- The protected command may proceed only when current user context is established.
- An unknown presented user id is rejected as unauthenticated.

### Scenario: Organization validates invitee against auth registered identities

**Given**
- Organization command behavior needs to verify that an invitee user id belongs
  to a registered user.
- Auth has registered user identity state.

**When**
- Organization asks whether the invitee user id exists.

**Then**
- The registered-user check is answered from auth-owned registered user identity
  state.
- Organization behavior does not maintain a separate registered-user source.

## Acceptance Criteria

- Auth MUST own registered user identity state.
- Current user context establishment MUST reject an unknown presented user id as
  unauthenticated.
- Current user context establishment MUST continue to reject missing, blank, or
  multiple presented user ids under the existing auth rules.
- Organization registered-user checks MUST be backed by auth registered user
  identity state.
- Organization MUST NOT define or maintain a separate registered-user source for
  HTTP application wiring.
- This slice MUST NOT add login, credential verification, session, cookie, token,
  registration, or password behavior.
- This slice MUST NOT change organization domain behavior.

## Invariants

- A registered user identity always has one non-empty authenticated user id.
- Current user context can be established only for a registered user identity.
- Organization registered-user checks and auth protected-request actor checks use
  the same registered user identity source.

## Error Cases

- Missing presented user id for protected behavior -> rejected as unauthenticated.
- Blank presented user id for protected behavior -> rejected as unauthenticated.
- Multiple presented user ids for protected behavior -> rejected as unauthenticated.
- Unknown presented user id for protected behavior -> rejected as unauthenticated.
- Organization invitee user id is not registered in auth -> existing
  organization application validation rejects the command.

## Non-Goals

- Login.
- Registration.
- Logout.
- Session management.
- Cookie issuance.
- Token issuance or refresh.
- OAuth or external identity provider flows.
- Password storage or hashing.
- Password reset or credential recovery.
- User profile behavior.
- Changing organization command domain rules.
