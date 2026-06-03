# Spec 10 - Login Failure Audit

## Goal

Record auth-owned server-side audit state for rejected password login attempts
and allow the failed login response to include an opaque troubleshooting
reference.

This slice extends the failure path of auth-08 password login. It does not
change successful login behavior, credential verification rules, session
creation, or protected command session context behavior.

The troubleshooting reference is safe for the client to receive because it is an
opaque lookup reference only. It must not encode login identifier, registered
user, personnel, role, organization, profile, IP address, or failure-reason
information.

## Workflow Traceability

- Workflow: Login Failure Support / Audit.
- Workflow gap: auth-08 and auth-09 explicitly exclude login failure reference
  ids and login audit records.
- Predecessor slices: auth-08 password login and auth-09 protected HTTP session
  context.
- Primary actor: login user.
- Support or operator actor: unknown / to be discovered.
- Audit retention and access policy: unknown / to be discovered.
- Audit lookup workflow: unknown / to be discovered.

## Domain Terms

- Login Failure Audit Record
  Auth-owned server-side state created for one rejected password login attempt.
  It records troubleshooting information that must not be returned to the
  client.

- Login Failure Reference Id
  An opaque server-issued reference for a login failure audit record. It carries
  no encoded login identifier, registered-user, personnel, role, organization,
  profile, IP address, session, credential, or failure-reason information.

- Troubleshooting Reference
  The client-visible use of the login failure reference id. The client may
  provide it to a future support workflow, but this slice does not define that
  workflow.

- Login Failure Reason
  A server-side-only classification of why a login attempt was rejected. It may
  be stored for troubleshooting, but it must never be exposed in the failed
  login response.

- Indistinguishable Login Failure Response
  The single response shape used for all rejected login attempts. It does not
  reveal whether login identifier input, password input, credential state,
  registered-user state, account state, or ambiguous credential resolution
  caused the rejection.

## HTTP Contract

This slice extends the failed response behavior of:

```text
POST /api/auth/login
```

The request body remains the auth-08 request body:

```text
loginIdentifier
password
```

All failed login attempts are rejected with the same indistinguishable failure
response. The failed response may include:

```text
loginFailureReferenceId
```

The login failure reference id is opaque. The response must not include the
login failure reason, authenticated user id, employee id, personnel id, name,
email, department, supervisor status, system role, organization role, profile
data, credential state, account state, IP address, session id, or session cookie
value.

Failed login still returns no session cookie. Successful login behavior remains
the auth-08 behavior.

## Scenarios

### Scenario: Failed login creates audit record and returns opaque reference

**Given**
- A user submits a login identifier and password to the login endpoint.
- The submitted values do not authenticate exactly one registered user identity.

**When**
- Auth rejects the login attempt.

**Then**
- Auth creates one login failure audit record in auth-owned server-side state.
- Auth assigns the audit record an opaque login failure reference id.
- Auth returns the same indistinguishable login failure response used for other
  login failures.
- The response includes the opaque login failure reference id.
- No server-side session is created.
- No session cookie is issued.
- The response does not reveal which condition caused the login failure.

### Scenario: Blank or missing login input remains indistinguishable

**Given**
- A login request is missing the login identifier, has a blank login identifier,
  is missing the password, or has a blank password.

**When**
- Auth rejects the login attempt.

**Then**
- Auth creates one login failure audit record.
- Auth returns an opaque login failure reference id.
- The client receives the same failed login response shape as any other login
  failure.
- The response does not reveal that input was missing or blank.

### Scenario: Credential or registered-user failure remains indistinguishable

**Given**
- A login request has an unknown login identifier, an incorrect password,
  credential state that does not map to a registered user identity, no
  authenticated user result, more than one authenticated user result, or another
  invalid credential or registered-user condition.

**When**
- Auth rejects the login attempt.

**Then**
- Auth creates one login failure audit record.
- Auth returns an opaque login failure reference id.
- The client receives the same failed login response shape as any other login
  failure.
- The response does not reveal whether login identifier, password, credential
  state, registered-user state, account state, or ambiguous resolution caused
  the failure.

### Scenario: Successful login does not create login failure audit

**Given**
- A registered user identity exists in auth-owned state.
- Auth-owned login credential state can verify the submitted login identifier
  and password for that registered user identity.

**When**
- The user submits valid credentials to the login endpoint.

**Then**
- Auth performs the successful login behavior from auth-08.
- Auth creates server-side session state for the authenticated registered user.
- Auth returns the auth-08 session cookie.
- Auth does not create a login failure audit record.
- Auth does not return a login failure reference id.

## Acceptance Criteria

- A rejected password login attempt MUST create one auth-owned login failure
  audit record.
- A rejected password login attempt MUST have an opaque login failure reference
  id.
- The failed login response MUST include the opaque login failure reference id.
- The login failure reference id MUST NOT encode or expose login identifier,
  user id, employee id, personnel id, name, email, department, supervisor
  status, system role, organization role, profile data, IP address, credential
  state, account state, failure reason, session id, or session cookie value.
- Login failure responses MUST remain indistinguishable across missing input,
  blank input, unknown identifier, incorrect password, invalid credential state,
  invalid registered-user state, invalid account state, no authenticated user
  result, and ambiguous authenticated user results.
- The failed login response MUST NOT include the login failure reason.
- The failed login response MUST NOT include audit details other than the
  opaque login failure reference id.
- A failed login MUST NOT create a server-side session.
- A failed login MUST NOT issue a session cookie.
- A successful login MUST NOT create a login failure audit record.
- A successful login MUST NOT return a login failure reference id.
- This slice MUST NOT change auth-08 successful login behavior.
- This slice MUST NOT change auth-08 credential verification rules.
- This slice MUST NOT change auth-09 protected command session context
  behavior.
- This slice MUST NOT change organization behavior.
- Login failure audit state MUST NOT store submitted password values, raw
  credential secrets, raw session cookie values, or plaintext credential secret
  material.
- Login failure audit details beyond the reference id MUST remain server-side.

## Invariants

- Login failure audit state is auth-owned.
- A login failure reference id is an opaque lookup reference only.
- A login failure reference id carries no encoded identity, personnel, role,
  organization, profile, IP address, credential, session, or failure-reason
  meaning.
- Failed login attempts create no authenticated session state.
- Successful login attempts create no login failure audit state.
- Client-visible login failure data is limited to the indistinguishable failure
  response and the opaque login failure reference id.
- Troubleshooting details stored in audit state are not client-visible.

## Error Cases

- Missing login identifier -> rejected as login failure with audit record and
  opaque reference.
- Blank login identifier -> rejected as login failure with audit record and
  opaque reference.
- Missing password -> rejected as login failure with audit record and opaque
  reference.
- Blank password -> rejected as login failure with audit record and opaque
  reference.
- Unknown login identifier -> rejected as login failure with audit record and
  opaque reference, without revealing that the identifier is unknown.
- Incorrect password -> rejected as login failure with audit record and opaque
  reference, without revealing that the password is incorrect.
- Credential state does not map to a registered user identity -> rejected as
  login failure with audit record and opaque reference, without revealing the
  registered-user state.
- Credential verification yields no authenticated user -> rejected as login
  failure with audit record and opaque reference.
- Credential verification yields more than one authenticated user -> rejected as
  login failure with audit record and opaque reference.

All rejected login attempts use the same client-visible failure response shape
and do not identify the failed condition.

## Non-Goals

- Login credential verification rule changes.
- Successful login behavior changes.
- Session creation behavior changes.
- Session creation on failed login.
- Protected HTTP session context changes.
- Session renewal or sliding expiration.
- Logout or session revocation.
- Refresh token or access token behavior.
- OAuth, SSO, OIDC, or external identity provider flows.
- MFA.
- Password reset or credential recovery.
- Account lockout or retry limit.
- Full log management framework.
- General application logging or correlation id propagation.
- Audit lookup, query, listing, or export endpoint.
- Audit retention policy.
- Audit access policy.
- Support or operator actor lifecycle.
- Authorization permission model changes.
- Role assignment, revocation, or listing.
- IT_ADMIN lifecycle.
- GroupAdmin lifecycle.
- DBM, ITSM, or approval workflow behavior.
- Frontend UI.
- User profile or current-user endpoint.
- Changing organization domain behavior.
- Changing organization application command behavior.
