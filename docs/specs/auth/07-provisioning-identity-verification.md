# Spec 07 - Provisioning Identity Verification

## Goal

Confirm that the target person for a user provisioning flow can prove possession
of a server-issued verification code using only an opaque verification request
id and the verification code.

This slice prevents clients from submitting or inferring personnel information
during identity verification. It does not define login, persistent
authenticated state, authorization, role assignment, or completion of registered
user provisioning.

## Domain Terms

- Provisioning Verification Request
  An auth-owned server-side record for a pending identity verification step in a
  user provisioning flow.

- Verification Request Id
  An opaque UUID that identifies a provisioning verification request. It carries
  no encoded personnel, role, organization, or authorization information.

- Verification Code
  A server-issued code known to the target person through an out-of-band
  verification channel.

- Target Person
  The person whose identity is being verified before a registered user identity
  can be provisioned in a later behavior.

- Verified Provisioning Request
  A provisioning verification request that has successfully confirmed the target
  person using the verification request id and verification code.

## Preconditions

- A provisioning verification request already exists in auth-owned server-side
  state.
- The target person has received the verification request id and verification
  code through a channel outside this slice.
- The client has no access to auth-owned personnel details, internal user ids,
  role assignments, organization memberships, or supervisor/staff
  classifications.

## Scenarios

### Scenario: Target person confirms a provisioning verification request

**Given**
- A pending provisioning verification request exists.
- The request is not expired.
- The request has not already been verified.
- The target person has the verification request id.
- The target person has the matching verification code.

**When**
- The target person submits the verification request id and verification code.

**Then**
- Auth verifies the submitted values against server-side verification state.
- The provisioning verification request is marked as verified.
- No login state, session, token, cookie, system role, organization role, or
  registered user identity is created by this behavior.

### Scenario: Invalid verification attempt is rejected without exposing state

**Given**
- A client submits a verification request id and verification code.
- The submitted values do not identify a pending request that can be verified.

**When**
- Auth evaluates the verification attempt.

**Then**
- The verification attempt is rejected.
- The provisioning verification request is not marked as verified.
- The response does not reveal whether the verification request id, verification
  code, expiration state, or prior verification state caused the rejection.

## Acceptance Criteria

- The client MUST submit only a verification request id and verification code
  for this verification behavior.
- The verification request id MUST be an opaque UUID.
- The verification request id MUST NOT encode or expose personnel type,
  employment type, department, organization, supervisor status, system role,
  group role, internal user id, name, email, or other profile information.
- Auth MUST verify the submitted verification request id and verification code
  against auth-owned server-side verification state.
- A pending, unexpired request with a matching verification code MUST become
  verified.
- An unknown, expired, already verified, or code-mismatched request MUST be
  rejected.
- Rejection responses MUST NOT reveal which verification condition failed.
- A verified request MUST NOT create login state.
- A verified request MUST NOT issue a session, cookie, access token, refresh
  token, or any other persistent authenticated browser/client state.
- A verified request MUST NOT assign or revoke auth system roles.
- A verified request MUST NOT assign or revoke organization group roles.
- A verified request MUST NOT create a registered user identity in this slice.
- This slice MUST NOT change organization behavior.

## Invariants

- A verification request id is an opaque reference only.
- Personnel, role, organization, and profile information remain server-side.
- Verification proves only that the submitted code matches the server-side
  verification request state.
- Verification does not grant authorization for later protected operations.
- Verification does not establish logged-in user state.
- A provisioning verification request can become verified at most once.

## Error Cases

- Missing verification request id -> rejected.
- Malformed verification request id -> rejected.
- Missing verification code -> rejected.
- Blank verification code -> rejected.
- Unknown verification request id -> rejected without revealing that the id is
  unknown.
- Incorrect verification code -> rejected without revealing that the code is
  incorrect.
- Expired verification request -> rejected without revealing that the request is
  expired.
- Already verified request -> rejected without revealing that the request was
  already verified.

## Non-Goals

- Authorization or permission assignment.
- IT admin lifecycle.
- GroupAdmin lifecycle.
- DBM, ITSM, or external approval workflow integration.
- Creating or initiating provisioning verification requests.
- Completing registered user identity provisioning.
- Creating the first bootstrap IT admin.
- Public self-registration.
- Login.
- Logout.
- Session management.
- Cookie issuance.
- Token issuance or refresh.
- Persistent authenticated client state.
- OAuth or external identity provider flows.
- Password storage or hashing.
- Password reset or credential recovery.
- User profile behavior.
- Assigning, revoking, or listing auth system roles.
- Assigning, revoking, or listing organization group roles.
- Disabling, deleting, suspending, or reactivating users.
- Email, SMS, or other verification-code delivery.
- Frontend UI.
- Changing organization command domain rules.
