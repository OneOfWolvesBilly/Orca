# DDD Derivation - Frontend 01 Login Result Shell

Status: Approved / derived from amended reusable presentation behavior.

This note is **derived from**
`docs/specs/frontend/01-login-result-shell.md`.
It does not introduce new behavior.

---

## Scope Ownership

**frontend delivery support scope**

Rationale:

- The slice exposes existing auth and reference-core HTTP behavior through a
  frontend shell.
- The shell does not own password login, credential verification, session
  creation, login failure audit, or organization behavior.
- The behavior is cross-context frontend consumption of existing backend
  contracts, not a new domain bounded context.

Auth remains authoritative for:

- login request semantics
- credential verification
- session creation
- `ORCA_SESSION`
- `loginFailureReferenceId`

Reference-core remains authoritative for:

- stable API error response shape
- safe client-visible error fields

The frontend is a delivery adapter and presentation boundary.

## No Aggregate Root

This slice introduces no aggregate root and no domain model.

Why:

- The frontend login result shell does not own persisted state.
- The shell does not enforce business invariants.
- Login success and failure decisions remain backend decisions.
- UI state such as pending, success, and error display is presentation state,
  not domain state.

## Frontend Delivery Boundary

The first frontend module should live under:

```text
orca_frontend/
```

This location is already reserved by the repository structure in `README.md`.

The module is a frontend delivery adapter for the existing backend API. It does
not introduce separate domain, application, or infrastructure layers.

The amended reusable presentation behavior justifies three small component
boundaries:

- `AuthShell` receives product identity and supporting copy and provides the
  authentication page frame.
- `LoginForm` owns credential input, submit progress, and password clearing.
- `LoginResultView` renders safe success, stable error, and generic failure
  results.

The application entry point supplies Orca presentation values and connects the
shared form to the login API adapter. These component boundaries are frontend
presentation composition, not domain layers or a complete frontend
architecture.

## Reusable Product Presentation Boundary

Product-specific inputs for this slice are limited to:

- product name
- supporting login copy

The shared login core owns:

- login identifier and password input behavior
- submit progress behavior
- password clearing after submission
- safe login result presentation
- stable API error and `loginFailureReferenceId` presentation

The shared shell must not require an Orca-specific marketing, workflow, roadmap,
or split-screen information panel. A future product may compose additional
content outside the shared login core, but that content is not required by this
slice.

This boundary does not create:

- a distributable component package
- a theme token system
- a design system
- runtime product configuration

## Minimum Frontend Model

### API Request Shape

- Login request:
  - `loginIdentifier`
  - `password`

The request shape mirrors `auth-08`. The frontend must not add employee,
personnel, role, organization, profile, or account-state fields.

### API Error View Model

The frontend may define a small client-side representation of the stable API
error response:

- `status`
- `code`
- `message`
- optional `loginFailureReferenceId`

This representation is a view/adapter model, not a shared domain model.

### UI State

Minimum UI states:

- idle login form
- submitting
- login success result
- login rejected result
- stable non-login error result
- safe generic transport/unexpected failure result

The UI state must not include raw password values after submission, raw session
cookie values, backend exception details, or inferred authenticated user state.

## Rule Placement

### Auth rules

- Auth decides whether credentials authenticate exactly one registered user.
- Auth creates server-side session state.
- Auth returns `ORCA_SESSION` through HTTP cookie behavior.
- Auth creates login failure audit records and opaque references.

### Reference-core rules

- Reference-core defines the stable API error shape.
- Reference-core preserves `loginFailureReferenceId` only for login rejection.
- Reference-core keeps internal exception details out of client-visible errors.

### Frontend adapter rules

- Submit only the login identifier and password to the backend login endpoint.
- Treat `204` as successful completion of the submitted login request.
- Let the browser handle `Set-Cookie`; do not read or parse `ORCA_SESSION`.
- Parse stable error responses by `status`, `code`, `message`, and optional
  `loginFailureReferenceId`.
- Display `loginFailureReferenceId` only when the stable code is
  `LOGIN_REJECTED`.
- Use a safe generic display for transport failures or malformed responses.
- Do not branch on safe message wording.
- Do not call protected organization commands as a session probe.

### Organization rules

- No organization domain, application, persistence, or web behavior changes are
  derived from this slice.
- Organization command UI belongs to a future organization slice.

## Main Entry Behavior

For this slice, the frontend entry point is the login result shell.

It is not a product board. It is not a post-login workspace. It does not define
Orca's eventual main navigation.

After successful login, the shell displays a safe result such as "login request
succeeded" or "session established for this request" without showing user or
session details. The shell does not route anywhere else because no protected
route or current-user behavior is specified yet.

On refresh, the shell returns to the login form because cookie presence alone is
not an authoritative session state signal for frontend behavior.

## Technology Selection

Recommended first frontend stack:

- Vite
- React
- TypeScript
- native `fetch`
- component-local state
- plain CSS

Rationale:

- The slice needs a small form, result panels, and typed error parsing.
- TypeScript helps keep the stable API error adapter explicit.
- Native `fetch` is sufficient for one endpoint and avoids introducing a data
  fetching framework before the workflow needs it.
- Component-local state is enough for a single login result shell.
- A router, global state management library, design system, generated API
  client, and localization framework would be premature for this slice.

This selection is a reference implementation choice, not a project-wide
frontend framework comparison. Additional framework adapters should wait until
the reference flow is stable and a portability workflow is explicitly selected.

## Local Development Boundary

The backend currently does not define a CORS policy. This slice should avoid
requiring backend CORS behavior.

Recommended local development approach:

- The frontend development server serves the UI.
- `/api` requests are proxied to the backend during development.
- Frontend requests include credentials so browser cookie behavior can be
  verified.

Cookie notes:

- `ORCA_SESSION` is `HttpOnly`, so application JavaScript cannot inspect it.
- The cookie is `Secure` and `SameSite=Lax` according to the backend contract.
- Local visual verification should use a consistent local host name for the
  frontend and proxied backend flow to avoid cookie-origin confusion.

If local cookie behavior fails, that is a development-environment verification
finding unless a future slice specifies backend CORS or deployment behavior.

## Sensitive Data Boundary

Frontend application code and UI must not expose:

- submitted password after submission
- raw session cookie value or session id
- backend exception messages
- stack traces
- raw response bodies
- credential state
- registered-user state
- account state
- authenticated user id
- employee id or personnel id
- name, email, department, supervisor status, or profile data
- auth system role
- organization role or membership details
- organization aggregate internals
- login failure reason
- auth-10 audit details beyond the opaque `loginFailureReferenceId`

## Test Layer Placement

Frontend tests should validate presentation and adapter behavior:

- product name and supporting copy can change without changing login behavior
- login form submits `loginIdentifier` and `password`
- successful `204` response shows a safe success result
- successful result does not display user, role, organization, profile, session
  id, or cookie value
- `401 LOGIN_REJECTED` displays code, safe message, and
  `loginFailureReferenceId`
- non-login stable API errors display code and safe message without login
  failure reference
- malformed error response or transport failure displays a safe generic result
- error handling does not expose raw exception or response details
- refresh/initial load shows the login shell and does not infer session state

Backend tests are not required by this slice unless implementing the frontend
reveals a defect in existing backend behavior.

Visual verification should cover:

- rejected login result with an opaque reference
- successful login result through the browser
- browser-managed cookie receipt without displaying cookie details
- safe generic failure when the backend is unavailable

## Future Slice Boundaries

Likely follow-up slices:

- Session-aware frontend entry:
  - may require current-user or session inspection intake
  - should define whether the frontend can restore session state after refresh
- Organization frontend command console:
  - should be an organization slice because it exposes organization behavior
  - must not change organization domain/application rules unless separately
    specified
- Logout or session revocation:
  - should be an auth slice when the workflow is selected

Board/main workspace behavior is not derived in this slice.

## Unknown / To Be Discovered

- final route map
- current-user/session inspection endpoint need
- logout and session revocation behavior
- production hosting and CORS/deployment policy
- design system
- localization
- generated API contract tooling
- local credential provisioning for manual verification

## Non-Goals / Out of Scope

- Reference-core domain model or aggregate.
- Auth domain/application/infrastructure changes.
- Organization domain/application/infrastructure/web changes.
- Current-user endpoint.
- Session restoration after refresh.
- Protected route.
- Board page.
- Organization command console.
- Admin provisioning UI.
- Logout or session revocation.
- Router, global state management, design system, or localization framework.
- Cross-repository package publication or a complete branding framework.
- Backend CORS behavior.
- OpenAPI generation.
