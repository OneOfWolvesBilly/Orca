# Spec 08 - Password Login with Server-side Session

## Goal

Allow a user to log in with a server-recognized login identifier and password.
Successful login creates server-side authenticated session state and returns an
opaque HttpOnly session cookie to the client.

This slice introduces password credential verification and session
establishment. It does not make protected HTTP command endpoints consume the
session cookie; that belongs to a later slice.

## Domain Terms

- Login Identifier
  A client-submitted identifier used only for login. It is opaque to this slice:
  clients and specs must not derive employee, personnel, profile, role,
  organization, or authorization meaning from it.

- Password
  A client-submitted secret used to prove control of login credentials. Password
  verification is performed against auth-owned server-side credential state.

- Login Credential
  Auth-owned server-side credential state that can verify whether a submitted
  login identifier and password authenticate exactly one registered user
  identity.

- Server-side Session
  Auth-owned persistent authenticated state created after successful login.

- Session Id
  An opaque server-issued identifier for a server-side session. It carries no
  encoded user, personnel, role, organization, or profile information.

- Session Cookie
  The HTTP cookie returned to the client after successful login. Its value is
  the opaque session id.

## HTTP Contract

This slice defines one login command endpoint:

```text
POST /api/auth/login
```

The request body contains only:

```text
loginIdentifier
password
```

Successful login returns a `Set-Cookie` response header containing the session
cookie. The success response must not expose user id, employee id, name, email,
department, supervisor information, system role, organization role, or profile
data.

## Scenarios

### Scenario: User logs in with valid credentials

**Given**
- A registered user identity exists in auth-owned state.
- Auth-owned login credential state can verify the submitted login identifier
  and password for that registered user identity.

**When**
- The user submits the login identifier and password to the login endpoint.

**Then**
- Auth verifies the submitted credentials against server-side credential state.
- Auth creates a server-side session for the authenticated registered user.
- Auth returns an HttpOnly session cookie whose value is an opaque session id.
- The response does not expose user, personnel, profile, role, or organization
  information.

### Scenario: Login attempt fails without exposing credential state

**Given**
- A user submits a login identifier and password.
- The submitted values do not authenticate exactly one registered user identity.

**When**
- Auth evaluates the login attempt.

**Then**
- The login attempt is rejected.
- No server-side session is created.
- No session cookie is issued.
- The response does not reveal whether the login identifier is unknown, the
  password is incorrect, credentials are missing, or the user is not registered.

## Acceptance Criteria

- The client MUST submit only a login identifier and password for this login
  behavior.
- The login identifier MUST be treated as opaque by this slice.
- The login identifier MUST NOT expose or require employee id, personnel id,
  name, email, department, supervisor status, system role, organization role, or
  profile information.
- Auth MUST verify submitted credentials against auth-owned server-side
  credential state.
- A successful login MUST authenticate exactly one registered user identity.
- A successful login MUST create server-side session state for the authenticated
  registered user.
- A successful login MUST return a session cookie with an opaque session id
  value.
- The session cookie MUST be `HttpOnly`.
- The session cookie MUST be `Secure`.
- The session cookie MUST specify `SameSite=Lax`.
- The session cookie MUST specify a bounded lifetime with `Max-Age` or
  `Expires`.
- The session cookie value MUST NOT encode or expose user id, employee id,
  personnel id, name, email, department, supervisor status, system role,
  organization role, or profile information.
- A failed login MUST NOT create a server-side session.
- A failed login MUST NOT issue a session cookie.
- Login failure responses MUST NOT reveal whether the login identifier,
  password, credential state, or registered-user state caused the rejection.
- The frontend MUST NOT receive user id, employee id, name, email, department,
  supervisor information, system role, organization role, or profile data from
  this login behavior.
- This slice MUST NOT use the demo `X-User-Id` header as a login mechanism.
- This slice MUST NOT change how existing protected HTTP command endpoints
  establish current user context.
- This slice MUST NOT change organization behavior.

## Invariants

- Login credential state is auth-owned.
- A login identifier is interpreted only by auth-owned credential verification.
- Successful login creates server-side session state for exactly one registered
  user identity.
- A session id is an opaque reference only.
- Session state is stored server-side.
- Client-held session state is limited to the opaque session cookie value.
- Failed login attempts do not create authenticated session state.

## Error Cases

- Missing login identifier -> rejected as login failure.
- Blank login identifier -> rejected as login failure.
- Missing password -> rejected as login failure.
- Blank password -> rejected as login failure.
- Unknown login identifier -> rejected as login failure without revealing that
  the identifier is unknown.
- Incorrect password -> rejected as login failure without revealing that the
  password is incorrect.
- Credential state does not map to a registered user identity -> rejected as
  login failure without revealing the registered-user state.
- Credential verification yields no authenticated user -> rejected as login
  failure.
- Credential verification yields more than one authenticated user -> rejected as
  login failure.

## Non-Goals

- OAuth or external identity provider flows.
- MFA.
- Password reset or credential recovery.
- Account lockout or retry limit.
- Refresh token or access token.
- Public self-registration.
- Provisioning verification request lifecycle.
- Creating or completing registered user identity provisioning.
- Role assignment, revocation, or listing.
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
- Changing organization behavior.
