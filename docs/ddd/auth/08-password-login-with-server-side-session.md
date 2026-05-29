# DDD Derivation - 08 Password Login with Server-side Session

This note is **derived from**
`docs/specs/auth/08-password-login-with-server-side-session.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- Password credential verification is auth-owned.
- Server-side authenticated session state is auth-owned.
- Organization consumes authenticated actor context in later protected behavior
  but does not own login credentials or session lifecycle.

---

## Aggregate Root

**AuthenticatedSession**

Why:
- The slice introduces a server-side authenticated session lifecycle.
- A successful login creates one session for one registered user identity.
- The session owns the invariant that its id is opaque and its authenticated
  user id is server-side state.
- Cookie transport is an HTTP adapter concern; the session itself remains an
  auth domain concept.

Credential verification is not modeled as an aggregate in this slice. It is an
auth application capability backed by server-side credential state.

---

## Minimum Model Additions

### Domain Model

- `LoginIdentifier`
  - value object for client-submitted login identifier
  - must be present and non-blank
  - remains opaque to this slice

- `SubmittedPassword`
  - value object or validated command input for client-submitted password
  - must be present and non-blank
  - must not be exposed in responses

- `AuthenticatedSession`
  - server-side authenticated session state
  - contains an opaque session id
  - contains exactly one authenticated user id server-side
  - has a bounded lifetime

- `AuthenticatedSessionId`
  - opaque session identifier
  - must not encode user, personnel, role, organization, or profile meaning

### Application Ports

- `LoginCredentialVerifier`
  - verifies a login identifier and submitted password against auth-owned
    server-side credential state
  - returns exactly one authenticated user id on success
  - exposes one indistinguishable failure category for invalid login attempts

- `AuthenticatedSessionRepository`
  - persists created server-side session state

- `AuthenticatedSessionIdGenerator`
  - creates opaque session ids

- Time source
  - supplies login time and session expiration time when needed for tests

### Application Use Case

- `PasswordLoginUseCase`
  - receives login identifier and password
  - asks `LoginCredentialVerifier` to authenticate the submitted values
  - creates an `AuthenticatedSession` after successful verification
  - persists the server-side session
  - returns only session cookie issuance data needed by the web adapter
  - rejects invalid login attempts through one indistinguishable failure
    category

### Web Adapter

- `POST /api/auth/login`
  - accepts only login identifier and password
  - maps successful login result to a `Set-Cookie` header
  - sets `HttpOnly`, `Secure`, `SameSite=Lax`, and bounded lifetime attributes
  - does not include user, personnel, profile, role, or organization data in the
    response
  - maps all login failures to one indistinguishable HTTP failure response

---

## Rule Placement

### Auth domain rules

- Login identifier input is present and non-blank.
- Submitted password input is present and non-blank.
- Session id is opaque.
- Authenticated session contains exactly one authenticated user id server-side.
- Authenticated session has a bounded lifetime.

### Auth application rules

- Credentials must authenticate exactly one registered user identity before a
  session is created.
- Failed login attempts create no session.
- Failed login attempts return one indistinguishable failure category.
- Credential state and registered-user state remain auth-owned.

### Infrastructure rules

- Credential storage and password hashing details are infrastructure concerns as
  long as verification behavior matches the spec.
- Session persistence stores auth-owned server-side session state.
- Session id generation must produce opaque identifiers.

### Web adapter rules

- Cookie attributes are HTTP concerns derived from the spec contract.
- Login response mapping must not expose authenticated user details.
- The demo `X-User-Id` header is not a login mechanism.

### Organization rules

- No organization domain, application, or web behavior changes are derived from
  this slice.

---

## Explicitly Not In This Slice

- OAuth or external identity provider flows.
- MFA.
- Password reset or credential recovery.
- Account lockout or retry limit.
- Refresh token or access token.
- Public self-registration.
- Provisioning verification request lifecycle.
- Completing registered user identity provisioning.
- Auth system role assignment, revocation, or listing.
- Organization group role assignment, revocation, or listing.
- Authorization permission model changes.
- IT_ADMIN lifecycle.
- GroupAdmin lifecycle.
- DBM, ITSM, or external approval workflow integration.
- Logout.
- Session renewal or sliding expiration.
- Protected HTTP session context establishment.
- Replacing existing protected HTTP command mapping.
- User profile or current-user endpoint.
- Frontend UI.
- Organization behavior changes.

---

## Test Layer Placement

- Domain tests validate:
  - login identifier input validation
  - submitted password input validation if represented as a value object
  - authenticated session creation invariants
  - opaque session id input validation
  - bounded session lifetime invariant

- Application tests validate:
  - successful credential verification creates and persists one session
  - failed credential verification creates no session
  - missing or blank inputs are exposed as the same login failure category
  - credential verification that yields no or ambiguous authenticated user is
    rejected as the same login failure category
  - login result contains only session issuance data

- Infrastructure tests may validate:
  - session persistence round trip
  - credential verifier adapter behavior if implemented in this slice
  - opaque session id storage

- Web integration tests validate:
  - `POST /api/auth/login` returns `Set-Cookie` with required attributes after
    successful login
  - successful login response omits user, personnel, role, organization, and
    profile data
  - failed login returns one indistinguishable failure response
  - failed login does not issue a session cookie
  - existing protected command mappings are not migrated to session consumption
    in this slice
