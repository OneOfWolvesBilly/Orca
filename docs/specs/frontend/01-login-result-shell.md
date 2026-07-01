# Frontend 01 - Login Result Shell

Status: Approved / React reference implemented / Vue and Angular ports planned.

## Goal

Provide the first frontend delivery slice for Orca: a browser-operated login
shell that lets a registered user submit password login credentials and see the
safe result returned by the existing backend.

This slice demonstrates how a frontend consumes the auth login endpoint and the
stable API error contract without re-implementing backend business rules. It is
not Orca's main product board, not a protected route, and not an organization
command surface.

The login capability is a reusable frontend core. Product identity and
supporting copy may vary without duplicating login submission, result handling,
or stable API error presentation behavior.

The same authoritative login behavior targets three frontend framework
implementations:

- React
- Vue
- Angular

React is the first reference implementation. Vue and Angular are planned ports
of this behavior and are not part of the current production-code delivery.
Framework implementations must remain separately buildable applications.

## Workflow Traceability

- Workflow:
  - Frontend Reference Shell
  - Authentication and Session
  - Error and Exception Handling
- Workflow gap:
  - no frontend login screen exists
  - no frontend stable API error display exists
  - no browser-operated way exists to verify login success and login rejection
- Primary actor:
  - Registered User
- Supporting actors:
  - Frontend application shell
  - Backend API
- Predecessor slices:
  - `auth-08` password login with server-side session
  - `auth-09` protected HTTP session context
  - `auth-10` login failure audit
  - `reference-core-01` stable API error contract

## Frontend Delivery Support Scope

`frontend-01` is a frontend delivery slice for consuming existing backend
capabilities. The frontend support scope does not own auth login rules, session
rules, login failure audit behavior, organization behavior, or API error
classification.

Auth remains authoritative for password login, session creation,
`ORCA_SESSION`, and `loginFailureReferenceId`. Reference-core remains
authoritative for the stable API error response shape.

## User Operation

The frontend entry point for this slice is a login result shell.

The user operation is:

```text
Open frontend
-> see login form
-> submit login identifier and password
-> see login success or safe login rejection result
```

Successful login does not route the user to a board or organization command
surface in this slice. The visible successful outcome is limited to confirmation
that the current login request succeeded and that the backend has established a
server-side session through its existing cookie behavior.

## Backend API Contract Consumed

The frontend submits:

```text
POST /api/auth/login
```

Request body:

```json
{
  "loginIdentifier": "login-id",
  "password": "password"
}
```

Successful response:

- HTTP status `204`
- no response body
- backend may set the existing `ORCA_SESSION` cookie as specified by `auth-08`

Rejected login response:

- HTTP status `401`
- stable error code `LOGIN_REJECTED`
- safe message
- `loginFailureReferenceId` present as specified by `auth-10` and
  `reference-core-01`
- no session cookie

Other unsuccessful API responses use the stable API error contract from
`reference-core-01` when available.

## Scenarios

### Scenario: User opens the frontend login shell

**Given**
- A user opens the Orca frontend entry point.

**When**
- The frontend shell loads.

**Then**
- The user sees a login form.
- The form asks only for login identifier and password.
- The shell does not claim that the user is authenticated.
- The shell does not inspect, display, or depend on the raw session cookie.
- The shell does not show a board, protected route, or organization command UI.

### Scenario: A product presents the reusable login core

**Given**
- A product uses the frontend login core.
- The product provides its product name and supporting login copy.

**When**
- The product renders the login shell.

**Then**
- The shell displays the provided product identity and supporting copy.
- The login form and result behavior remain the same across product
  presentations.
- The shared login core does not require a fixed Orca marketing, workflow, or
  roadmap panel.
- Product-specific presentation does not change login request, session, or
  stable API error behavior.

### Scenario: A framework implements the shared login behavior

**Given**
- React, Vue, and Angular are the selected frontend framework targets.
- This specification is authoritative for all three targets.

**When**
- A framework implementation delivers the login result shell.

**Then**
- The implementation preserves the request, result, error, cookie, and
  sensitive-data behavior defined by this specification.
- The implementation remains independently installable, buildable, testable,
  and runnable.
- The implementation does not import UI components from another framework.
- React is delivered first as the reference implementation.
- Missing Vue and Angular ports remain explicitly visible as planned work.

### Scenario: User submits valid login credentials

**Given**
- A registered user identity exists in backend auth state.
- Backend auth credential state can verify the submitted login identifier and
  password.
- The user is viewing the frontend login shell.

**When**
- The user submits the login form.

**Then**
- The frontend sends the login identifier and password to
  `POST /api/auth/login`.
- The frontend allows the browser to receive the backend session cookie through
  normal cookie handling.
- The frontend does not read or display the raw `ORCA_SESSION` cookie.
- The frontend shows a safe login success result for this login request.
- The success result does not include user id, employee id, personnel id, name,
  email, department, supervisor status, profile data, auth system role,
  organization role, session id, or session cookie value.
- The frontend does not route to a board, protected route, or organization
  command UI.

### Scenario: User submits rejected login credentials

**Given**
- The user is viewing the frontend login shell.
- Backend auth rejects the submitted login attempt.

**When**
- The user submits the login form.

**Then**
- The frontend displays the stable login rejection result.
- The frontend displays the stable error code.
- The frontend displays the safe backend message.
- The frontend displays the opaque `loginFailureReferenceId`.
- The frontend does not display the submitted password.
- The frontend does not reveal whether the login identifier, password,
  credential state, registered-user state, account state, or ambiguous
  resolution caused the rejection.
- The frontend keeps the user on the login shell so the user may submit another
  attempt.

### Scenario: Frontend receives a non-login stable API error

**Given**
- The user is using the login shell.
- The backend returns an unsuccessful response using the stable API error
  contract, but the error is not `LOGIN_REJECTED`.

**When**
- The frontend receives the response.

**Then**
- The frontend displays the stable error code.
- The frontend displays the safe message.
- The frontend does not display a `loginFailureReferenceId` unless the response
  code is `LOGIN_REJECTED`.
- The frontend does not display raw response details or internal exception
  details.

### Scenario: Frontend cannot receive a stable API error

**Given**
- The user is using the login shell.
- The login request fails because the backend is unreachable, the response is
  not parseable as the stable API error contract, or another transport-level
  failure occurs.

**When**
- The frontend handles the failure.

**Then**
- The frontend displays a safe generic failure result.
- The frontend does not expose raw exception messages, stack traces, raw
  response bodies, request internals, password values, or cookie values.
- The frontend does not infer backend business state from the transport
  failure.

### Scenario: User refreshes the frontend after a previous login success

**Given**
- A previous login request may have succeeded.
- The browser may hold an `ORCA_SESSION` cookie.
- No current-user or session inspection endpoint is defined by this slice.

**When**
- The user refreshes or reopens the frontend.

**Then**
- The frontend returns to the login shell.
- The frontend does not claim the user is authenticated.
- The frontend does not infer session validity from cookie presence.
- The frontend does not call protected organization commands only to determine
  session state.

## Acceptance Criteria

- The frontend MUST provide a browser-operated login form.
- The login form MUST submit only login identifier and password.
- The frontend MUST send the login request to the existing backend login
  endpoint.
- The frontend MUST allow backend cookie handling for successful login.
- The frontend MUST NOT read, display, store, or parse the raw `ORCA_SESSION`
  cookie value.
- A successful login response MUST produce a safe login success result visible
  to the user.
- A successful login result MUST NOT expose user, personnel, role,
  organization, profile, session id, or session cookie details.
- A rejected login response MUST display the stable `LOGIN_REJECTED` code.
- A rejected login response MUST display the safe backend message.
- A rejected login response MUST display the opaque
  `loginFailureReferenceId`.
- The frontend MUST NOT display `loginFailureReferenceId` for non-login error
  categories.
- The frontend MUST parse stable API error responses by `status`, `code`,
  `message`, and optional `loginFailureReferenceId`.
- The frontend MUST NOT branch behavior by parsing safe message text.
- The frontend MUST NOT expose raw backend exception details, stack traces,
  raw response bodies, password values, credential details, session values, user
  details, role details, organization details, or profile details.
- The frontend MUST NOT re-implement backend login, credential verification,
  session, authorization, or organization business rules.
- The frontend MUST allow product name and supporting login copy to be supplied
  separately from shared login form and result behavior.
- Shared login behavior MUST NOT depend on fixed Orca marketing, workflow, or
  roadmap content.
- Product presentation changes MUST NOT duplicate or alter login submission,
  stable API error parsing, or login result behavior.
- React, Vue, and Angular implementations MUST conform to the same
  authoritative behavior in this specification.
- Each framework implementation MUST remain independently installable,
  buildable, testable, and runnable.
- A framework implementation MUST NOT depend on UI components or framework
  runtime code from another framework implementation.
- The repository MUST identify React as implemented and Vue and Angular as
  planned until their ports pass their own verification.
- The frontend MUST NOT define Orca's main board or any post-login product
  workspace.
- The frontend MUST NOT claim session validity after refresh without a future
  session inspection behavior.
- This slice MUST NOT require backend behavior changes.

## Invariants

- Auth remains authoritative for login success, login rejection, session
  creation, session cookie attributes, and login failure references.
- Reference-core remains authoritative for stable API error response shape.
- The frontend is an API consumer and presentation layer only.
- Product identity is presentation input and is not part of auth or
  reference-core behavior.
- Shared login form and result behavior remain independent of product-specific
  supporting copy.
- Framework parity means behavioral equivalence, not shared UI component code.
- Client-visible login failure details are limited to the stable error response
  and the opaque `loginFailureReferenceId`.
- The raw `ORCA_SESSION` cookie value remains opaque and is not client-visible
  through frontend application code.

## Error Cases

- Backend returns `401 LOGIN_REJECTED` -> display safe login rejection and
  `loginFailureReferenceId`.
- Backend returns a stable non-login error -> display safe error code and
  message without login failure reference.
- Backend is unreachable -> display safe generic failure.
- Backend returns an unexpected or malformed error body -> display safe generic
  failure.
- Browser cannot persist or send the session cookie in the local development
  environment -> surface as a local verification concern, not as a backend
  behavior change in this slice.

## Local Development Constraints

- The frontend should consume `/api` through a local development arrangement
  that allows browser cookie behavior to be verified.
- The slice does not require adding backend CORS behavior.
- If the frontend and backend run on different local origins, the development
  setup must account for credentials and cookie handling without changing
  backend business behavior.

## Unknown / To Be Discovered

- localization and final UI copy
- complete frontend route structure
- current-user or session inspection endpoint
- logout and session revocation
- production frontend deployment model
- design system
- how local test credentials are provisioned for manual visual testing

## Non-Goals

- Board page or main product workspace.
- Protected route.
- Current-user endpoint.
- Session inspection or session restoration after refresh.
- Logout or session revocation.
- Organization command console.
- Admin provisioning UI.
- User profile UI.
- Complete frontend architecture.
- Design system.
- A distributable cross-repository component package.
- A complete product theming or branding framework.
- Vue or Angular production implementation in the React reference delivery.
- A framework abstraction layer that wraps React, Vue, and Angular.
- Localization.
- OpenAPI generation.
- Backend CORS policy.
- Backend auth credential verification changes.
- Backend session creation or protected session context changes.
- Removing, renaming, or generalizing `loginFailureReferenceId`.
- Changing organization domain, application, web, or persistence behavior.
