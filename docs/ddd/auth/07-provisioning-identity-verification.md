# DDD Derivation - 07 Provisioning Identity Verification

This note is **derived from**
`docs/specs/auth/07-provisioning-identity-verification.md`.
It does not introduce new behavior.

---

## Bounded Context

**auth**

Rationale:
- Provisioning identity verification is auth-owned.
- The behavior verifies server-side auth verification state before later
  registered user identity provisioning can occur.
- Organization roles, group memberships, and GroupAdmin behavior remain outside
  this slice.

---

## Aggregate Root

**ProvisioningVerificationRequest**

Why:
- The verification request owns the lifecycle from pending to verified.
- It owns whether the request is expired, already verified, and eligible to be
  verified.
- It owns the invariant that a request can become verified at most once.
- It keeps personnel, role, organization, and profile details server-side.

This aggregate does not create a registered user identity, login state, session,
token, system role assignment, or organization group role assignment.

---

## Minimum Model Additions

### Domain Model

- `ProvisioningVerificationRequest`
  - has an opaque verification request id
  - has server-side verification state
  - can be pending or verified
  - can determine whether it is expired
  - can verify a submitted verification code
  - rejects verification when already verified or expired

- `ProvisioningVerificationRequestId`
  - UUID-backed value object
  - opaque reference only
  - carries no encoded personnel, role, organization, or profile information

- `VerificationCode`
  - value object for submitted verification code input
  - must be present and non-blank
  - comparison details should remain inside auth-owned verification logic

### Application Ports

- `ProvisioningVerificationRequestRepository`
  - loads a request by verification request id
  - saves the verified request state

- Time source
  - supplies current time for expiration checks
  - may be represented as a simple clock abstraction if needed for tests

### Application Use Case

- `ConfirmProvisioningIdentityVerificationUseCase`
  - receives verification request id and verification code
  - loads the request from auth-owned state
  - asks the domain model to confirm verification
  - persists the verified state
  - returns no login/session/token/user provisioning result

---

## Rule Placement

### Auth domain rules

- Verification request id is an opaque UUID-backed reference.
- Verification code input must be present and non-blank.
- Pending, unexpired requests with a matching code can become verified.
- Expired requests cannot become verified.
- Already verified requests cannot be verified again.
- A request can become verified at most once.

### Auth application rules

- Missing or malformed verification request id is rejected before domain
  verification.
- Unknown verification request id is rejected without revealing that the id is
  unknown.
- Rejections for unknown, expired, already verified, or code-mismatched requests
  should be exposed through one indistinguishable application failure category.
- The use case persists only the verified request state.

### Infrastructure rules

- Persistence stores auth-owned verification request state.
- The stored request id remains opaque and must not encode personnel, role,
  organization, or profile meaning.
- Verification-code storage details are infrastructure concerns as long as the
  specified behavior is preserved.

### Web adapter rules

- A future HTTP adapter for this behavior should accept only:
  - verification request id
  - verification code
- The adapter must not accept user id, employee id, name, email, department,
  supervisor status, system role, organization role, or profile fields for this
  behavior.
- Error responses should not disclose which verification condition failed.

### Organization rules

- No organization domain, application, or web behavior changes are derived from
  this slice.

---

## Explicitly Not In This Slice

- Authorization or permission assignment.
- IT admin lifecycle.
- GroupAdmin lifecycle.
- DBM, ITSM, or external approval workflow integration.
- Creating or initiating provisioning verification requests.
- Completing registered user identity provisioning.
- Creating the first bootstrap IT admin.
- Public self-registration.
- Login, logout, sessions, cookies, tokens, OAuth, and persistent authenticated
  client state.
- Password storage, password verification, password reset, or credential
  recovery.
- User profile behavior.
- Auth system role assignment, revocation, or listing.
- Organization group role assignment, revocation, or listing.
- User deletion, disabling, suspension, or reactivation.
- Email, SMS, or other verification-code delivery.
- Frontend UI.
- Organization command behavior changes.

---

## Test Layer Placement

- Domain tests validate:
  - verification request id UUID parsing/validation
  - verification code input validation
  - successful transition from pending to verified
  - expired request rejection
  - already verified request rejection
  - code mismatch rejection
  - verified-at-most-once invariant

- Application tests validate:
  - unknown request id rejection
  - malformed request id rejection
  - successful load, verify, and save flow
  - indistinguishable failure category for unknown, expired, already verified,
    and code-mismatched requests
  - no registered user identity provisioning result is produced

- Infrastructure tests may validate:
  - persistence of pending and verified verification request state
  - lookup by opaque verification request id
  - expiration data round trip

- Web integration tests should be added only if this slice exposes an HTTP
  adapter.
