# DDD Derivation - 10 Login Failure Audit

This note is **derived from**
`docs/specs/auth/10-login-failure-audit.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- Password login is auth-owned.
- Credential verification state is auth-owned.
- Server-side session state is auth-owned.
- Login failure audit records and troubleshooting references support auth login
  failure handling.
- Organization does not own login credentials, login failure state, session
  state, or troubleshooting references.

---

## Aggregate Root

**LoginFailureAuditRecord**

Why:
- The slice introduces persisted auth-owned state for one rejected login
  attempt.
- The record owns the relationship between an opaque login failure reference id
  and server-side troubleshooting details.
- The record owns the invariant that client-visible troubleshooting references
  carry no encoded identity, personnel, role, organization, profile, IP address,
  credential, session, or failure-reason meaning.
- The record owns the invariant that forbidden secret material is not stored in
  audit state.

This aggregate does not create, mutate, renew, revoke, or resolve authenticated
sessions.

---

## Minimum Model Additions

### Domain Model

- `LoginFailureAuditRecord`
  - auth-owned server-side state for one rejected password login attempt
  - has one opaque login failure reference id
  - has an occurrence timestamp
  - may contain server-side troubleshooting details
  - must not contain submitted password values, raw credential secrets, raw
    session cookie values, or plaintext credential secret material

- `LoginFailureReferenceId`
  - opaque reference id value object
  - must not encode login identifier, user, personnel, role, organization,
    profile, IP address, credential, session, account state, or failure-reason
    information

- `LoginFailureReason`
  - server-side-only classification for troubleshooting
  - must not appear in client responses
  - should collapse to the same client-visible login failure outcome regardless
    of the internal reason

### Application Ports

- `LoginFailureAuditRecordRepository`
  - persists login failure audit records
  - lookup/query behavior is not required by this slice

- `LoginFailureReferenceIdGenerator`
  - creates opaque login failure reference ids

- Time source
  - supplies the occurrence timestamp for audit records

### Application Use Case

- `PasswordLoginUseCase`
  - remains the login orchestration boundary introduced by auth-08
  - on successful credential verification, preserves auth-08 behavior and
    creates no login failure audit record
  - on rejected login, creates and persists one login failure audit record
  - returns only the opaque login failure reference id as the additional
    failure result data
  - keeps all failure reasons indistinguishable to the client

### Web Adapter

- `POST /api/auth/login`
  - request body remains `loginIdentifier` and `password`
  - successful response remains the auth-08 session cookie behavior
  - failed response includes the opaque login failure reference id
  - failed response does not expose login failure reason or audit details
  - failed response issues no session cookie

---

## Rule Placement

### Auth domain rules

- `LoginFailureReferenceId` is opaque.
- `LoginFailureAuditRecord` contains exactly one login failure reference id.
- `LoginFailureAuditRecord` records a failed login occurrence.
- `LoginFailureAuditRecord` must not store submitted password values, raw
  credential secrets, raw session cookie values, or plaintext credential secret
  material.

### Auth application rules

- Every rejected password login attempt creates one login failure audit record.
- Successful password login attempts create no login failure audit record.
- Failed login attempts create no authenticated session state.
- Failed login attempts return one indistinguishable failure category.
- The only audit-related value returned to the client is the opaque login
  failure reference id.
- Internal failure reasons may be recorded server-side for troubleshooting, but
  they must not change the client-visible failure response.

### Infrastructure rules

- Persistence stores login failure audit records in auth-owned storage.
- Database schema is owned by Flyway migrations.
- Infrastructure must not define which login failures are auditable; it only
  persists the application result.
- Reference id generation must produce opaque values.
- Audit lookup, retention, export, and access policy are not derived in this
  slice.

### Web adapter rules

- HTTP mapping preserves the auth-08 login request shape.
- HTTP mapping preserves the auth-08 successful login cookie behavior.
- HTTP mapping returns a single indistinguishable failed login response shape.
- HTTP mapping includes the opaque login failure reference id on failed login.
- HTTP mapping must not expose internal login failure reason, registered-user
  state, credential state, account state, user data, personnel data, role data,
  organization data, profile data, IP address, session id, or session cookie
  value.

### Organization rules

- No organization domain, application, infrastructure, or web behavior changes
  are derived from this slice.

---

## Sensitive Data Boundary

Forbidden in login failure audit state:

- submitted password values
- raw credential secrets
- plaintext credential secret material
- raw session cookie values

Forbidden in client-visible failed login responses:

- login failure reason
- audit details other than the opaque reference id
- authenticated user id
- employee id or personnel id
- name, email, department, supervisor status, or profile data
- auth system role or organization role
- credential state
- registered-user state
- account state
- IP address
- session id or session cookie value

Allowed server-side audit data, when needed for troubleshooting:

- opaque login failure reference id
- occurred timestamp
- submitted login identifier as auth-owned server-side audit detail
- internal login failure reason or category
- input condition details such as missing or blank input

The allowed server-side data list is not a client response contract.

---

## Explicitly Not In This Slice

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
- Organization behavior changes.

---

## Unknown / To Be Discovered

- The support or operator actor who can inspect audit records.
- Audit retention period.
- Audit access policy.
- Audit lookup workflow.
- Whether audit records are stored with application logs or in a separate audit
  store.
- Privacy and security policy beyond the sensitive data boundaries specified by
  this slice.

---

## Test Layer Placement

Domain tests validate:
- login failure reference id validation and opacity constraints
- login failure audit record creation invariants
- audit record rejection of forbidden secret material, if represented directly
  in the domain model

Application tests validate:
- rejected login creates and persists one login failure audit record
- rejected login returns an opaque login failure reference id
- missing or blank input creates audit while preserving indistinguishable
  failure behavior
- unknown identifier, wrong password, invalid credential state, invalid
  registered-user state, no authenticated user result, and ambiguous user result
  create audit while preserving indistinguishable failure behavior
- successful login creates no login failure audit record
- successful login behavior and session creation remain unchanged

Infrastructure tests may validate:
- login failure audit persistence round trip
- generated reference ids are stored and retrieved as opaque values
- forbidden secret material is not persisted when represented at the persistence
  boundary

Web integration tests validate:
- failed `POST /api/auth/login` returns one indistinguishable failure response
  shape with an opaque login failure reference id
- failed login does not issue `ORCA_SESSION`
- failed login response does not expose failure reason or audit details
- successful login response remains the auth-08 cookie behavior and does not
  include a login failure reference id
- protected command session context behavior remains unchanged
