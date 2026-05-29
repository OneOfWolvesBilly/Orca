# Slice Map

This document is a derived index.
It helps contributors find behavior slices by bounded context.
It does not introduce behavior.

Authoritative behavior remains in `docs/specs/<bounded-context>/*`.

---

## Naming Rules

Slice ids are scoped by bounded context:

```text
<bounded-context>-<NN>
```

Spec and DDD note files use matching names:

```text
docs/specs/<bounded-context>/<NN>-<behavior-or-integration>.md
docs/ddd/<bounded-context>/<NN>-<behavior-or-integration>.md
```

The Spec column links to authoritative behavior definitions. The DDD column
links to derived notes that explain model decisions and must not introduce
behavior.

Frontend work is normally a delivery slice inside the bounded context it exposes.
For example, an organization UI command console should be tracked as an
organization slice, not as a separate frontend bounded context.

---

## Organization

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `organization-01` | [Create Group](specs/organization/01-create-group.md) | [DDD](ddd/organization/01-create-group.md) | Done |
| `organization-02` | [Invite Member](specs/organization/02-invite-member.md) | [DDD](ddd/organization/02-invite-member.md) | Done |
| `organization-03` | [Accept Invitation](specs/organization/03-accept-invitation.md) | [DDD](ddd/organization/03-accept-invitation.md) | Done |
| `organization-04` | [Reject Invitation](specs/organization/04-reject-invitation.md) | [DDD](ddd/organization/04-reject-invitation.md) | Done |
| `organization-05` | [Revoke Invitation](specs/organization/05-revoke-invitation.md) | [DDD](ddd/organization/05-revoke-invitation.md) | Done |
| `organization-06` | [Application Integration](specs/organization/06-application-integration.md) | [DDD](ddd/organization/06-application-integration.md) | Done |
| `organization-07` | [Persistence Integration](specs/organization/07-persistence-integration.md) | [DDD](ddd/organization/07-persistence-integration.md) | Done |
| `organization-08` | [Web API Integration](specs/organization/08-web-api-integration.md) | [DDD](ddd/organization/08-web-api-integration.md) | Done |

## Auth

| Slice | Spec | DDD | Status |
| --- | --- | --- | --- |
| `auth-01` | [Establish Authenticated User Context](specs/auth/01-establish-authenticated-user-context.md) | [DDD](ddd/auth/01-establish-authenticated-user-context.md) | Done |
| `auth-02` | [HTTP Current User Context Integration](specs/auth/02-http-current-user-context-integration.md) | [DDD](ddd/auth/02-http-current-user-context-integration.md) | Done |
| `auth-03` | [HTTP Request Current User Context Access](specs/auth/03-http-request-current-user-context-access.md) | [DDD](ddd/auth/03-http-request-current-user-context-access.md) | Done |
| `auth-04` | [Protected HTTP Command Boundary Mapping](specs/auth/04-protected-http-command-boundary-mapping.md) | [DDD](ddd/auth/04-protected-http-command-boundary-mapping.md) | Done |
| `auth-05` | [Registered User Identity Integration](specs/auth/05-registered-user-identity-integration.md) | [DDD](ddd/auth/05-registered-user-identity-integration.md) | Done |
| `auth-06` | [Admin-managed User Provisioning](specs/auth/06-admin-managed-user-provisioning.md) | [DDD](ddd/auth/06-admin-managed-user-provisioning.md) | Done |
| `auth-07` | [Provisioning Identity Verification](specs/auth/07-provisioning-identity-verification.md) | [DDD](ddd/auth/07-provisioning-identity-verification.md) | Done |
| `auth-08` | [Password Login with Server-side Session](specs/auth/08-password-login-with-server-side-session.md) | [DDD](ddd/auth/08-password-login-with-server-side-session.md) | Done |
| `auth-09` | [Protected HTTP Session Context](specs/auth/09-protected-http-session-context.md) | [DDD](ddd/auth/09-protected-http-session-context.md) | Planned |

## Planned Contexts

No additional bounded contexts beyond `organization` and `auth` are
authoritative yet.
Introduce `issue` or other bounded contexts only by adding their first
authoritative spec under `docs/specs/<bounded-context>/`.
