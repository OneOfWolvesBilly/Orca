# Frontend 03 - React Consumer Login Composition and Branding

Status: Approved / Implemented.

## Slice Intake

Slice candidate: `frontend-03` React consumer login composition and branding.

Workflow:

- Frontend Reference Shell.
- Product-agnostic embedded Core consumption.
- Authentication and Session, by consuming existing login behavior only.
- Error and Exception Handling, by preserving existing frontend contracts.

Workflow gap:

- `frontend-01` provides the React login result reference implementation and
  reusable product name and supporting-copy inputs inside the Orca React
  application.
- `frontend-02` extends that implementation with client failure
  observability.
- An independently structured React consumer cannot yet reuse those behaviors
  through a normal, supported frontend build dependency.
- No supported package root or public React exports currently distinguish the
  reusable login contract from Orca implementation internals.
- The existing presentation boundary does not define the approved customer
  logo, mandatory Orca attribution, or copyright behavior.

Primary actor:

- An application developer composing Orca login into a React consumer host.

Supporting actor:

- A registered user signing in through that consumer host.

Successful outcome:

- An independently structured React Minimal Consumer Fixture depends on the
  Orca React login package through a normal repository-local frontend build
  dependency.
- The fixture imports only the supported package root.
- The fixture supplies its product name, supporting login copy, and optional
  compliant customer logo with alternative text.
- The user sees the consumer presentation, existing Orca login behavior, and
  fixed Orca attribution and copyright.
- Changing consumer branding does not copy or change login, session, stable
  error, client diagnostic, audit, or product business behavior.

Failure flows:

- Missing customer logo selects the neutral fallback.
- Missing, blank, or invalid logo alternative text prevents the customer logo
  from rendering and selects the neutral fallback.
- Remote logo URLs, raw markup, SVG, unsupported image formats, and oversized
  logo assets are outside the supported branding contract.
- Existing login rejection, stable API error, malformed response, unexpected
  response, transport failure, and client diagnostic fallback behavior remain
  governed by `frontend-01` and `frontend-02`.
- A consumer that uses a deep or internal import does not satisfy the
  supported integration contract.

Existing supported slices:

- `frontend-01` React login result shell.
- `frontend-02` React client failure observability.
- `auth-08` password login with server-side session.
- `auth-09` protected HTTP session context.
- `auth-10` login failure audit.
- `reference-core-01` stable API error contract.
- `reference-core-02` client diagnostics foundation.

Planned predecessor slices:

- None.

Unknowns:

- Production package publication coordinates and registry.
- Production frontend hosting model.
- A future SVG sanitization and external-resource policy.
- Localization and future translation ownership.
- Vue and Angular consumer delivery.
- A future design system or theme-token model.

Non-goals:

- Redefining or duplicating `frontend-01` login behavior.
- Product-neutral protected fixture command or actor resolution.
- Logout integration or post-logout protected-command rejection.
- Protected routes, current-user inspection, or session restoration.
- Product workspace, navigation, or organization UI.
- Complete white-label, design-system, or theme-token support.
- Package registry publication.
- Backend behavior or schema changes.

Decision: enter SDD.

## Goal

Allow an independently structured React consumer host to compose the existing
Orca login behavior through one explicit public frontend package while
supplying limited product branding and preserving mandatory Orca attribution.

This slice proves reuse through a React Minimal Consumer Fixture. It does not
reimplement login, introduce protected session lifecycle behavior, or publish
the package to a registry.

## Frontend Delivery Support Scope

`frontend-03` is a frontend delivery slice. It does not create a frontend
domain bounded context and does not own auth, session, error, diagnostic,
audit, or consuming-product business rules.

`frontend-01` remains authoritative for:

- login identifier and password input;
- password login request submission;
- browser-managed `ORCA_SESSION` handling;
- safe login success and stable error presentation;
- `loginFailureReferenceId` presentation;
- sensitive-data exclusions.

`frontend-02` remains authoritative for:

- client failure classification;
- client diagnostic submission;
- `clientFailureReferenceId` presentation;
- diagnostic failure fallback.

This slice owns only:

- the supported React package boundary;
- independent consumer composition proof;
- the approved branding inputs and fallback;
- mandatory Orca attribution and copyright presentation;
- protection against source copying and unsupported internal imports.

## Public Frontend Integration Contract

The supported package name is:

```text
@oneofwolvesbilly/orca-react-login
```

The only supported import path is the package root:

```ts
import {
  OrcaLogin,
  type OrcaLoginBranding,
} from "@oneofwolvesbilly/orca-react-login";
```

Supported package-root exports:

- `OrcaLogin`
- `OrcaLoginBranding`

The package must not expose `/src/*`, component-file, API-adapter, error-parser,
diagnostic-adapter, or other deep import paths as supported exports.

The public package must own and reuse the existing React login composition,
including the `frontend-02` behavior that is part of the implemented React
reference. A consumer must not assemble a second login flow from copied Orca
components or adapters.

The first proof uses a normal repository-local frontend build dependency.
Publishing to npm or another package registry is not required.

## Consumer Branding Contract

`OrcaLoginBranding` supplies only:

- product name;
- supporting login copy;
- optional customer logo from a consumer build-time bundled asset;
- required customer-logo alternative text when a customer logo is supplied.

Branding input must not contain:

- login endpoint or request behavior overrides;
- error-code, error-message, or reference-label overrides;
- diagnostic category or endpoint overrides;
- cookie or session configuration;
- raw HTML or other markup;
- remote asset URLs;
- Orca attribution or copyright replacement controls;
- product authorization or business rules.

Consumer branding may change presentation identity only. It must not change
login, session, stable error, client diagnostic, audit, or consuming-product
business behavior.

## Customer Logo Contract

A supported customer logo:

- is optional;
- is supplied as a consumer build-time bundled asset;
- uses PNG or WebP;
- is no larger than 256 KiB;
- should use 512 x 512 source pixels and a 1:1 aspect ratio;
- renders no larger than 64 x 64 CSS pixels;
- uses `object-fit: contain` or equivalent behavior;
- must not be stretched, clipped, or allowed to overflow the login layout;
- has consumer-supplied, trimmed, non-blank alternative text.

The supported contract does not accept:

- remote URLs;
- same-origin runtime static-asset configuration;
- raw HTML or raw image markup;
- inline image data;
- SVG;
- a logo larger than 256 KiB;
- a customer logo without valid alternative text.

Logo conformance must be verifiable through the consumer build and contract
test boundary. A nonconforming logo must not become the rendered customer logo.

## Logo Fallback

When no valid customer logo is supplied:

- the login composition displays a generic neutral mark;
- the product name remains visible;
- the fallback must not claim or imply that it is a customer logo;
- the fallback must not use an Orca logo;
- the fallback must not look like customer-provided branding;
- no broken-image placeholder is displayed.

Missing, blank, or invalid customer-logo alternative text selects this same
fallback rather than rendering an inaccessible customer logo.

## Mandatory Orca Attribution

Every supported `OrcaLogin` composition displays the exact, untranslated text:

```text
Powered by Orca
```

The text:

- uses this exact capitalization;
- has no terminal punctuation;
- links to `https://github.com/OneOfWolvesBilly/Orca`;
- opens in a new browsing context;
- uses `noopener noreferrer` protection.

Every supported composition also displays the exact copyright notice:

```text
© 2026 Chen Chih-hao
```

The year is fixed at `2026` in this slice.

Consumer branding inputs must not replace, hide, translate, relabel, or remove
either the Orca attribution or the copyright notice. The public API must not
provide an attribution-disable or copyright-override option.

## Scenarios

### Scenario: Consumer composes Orca login through the public package

**Given**

- The React Minimal Consumer Fixture declares a normal repository-local build
  dependency on `@oneofwolvesbilly/orca-react-login`.
- The fixture imports `OrcaLogin` and `OrcaLoginBranding` from the package root.

**When**

- The fixture renders its login entry point.

**Then**

- The user sees the existing Orca login form and result behavior.
- The fixture does not contain a copied Orca login form, API adapter, stable
  error parser, or client diagnostics implementation.
- The fixture uses no deep or internal Orca frontend import.

### Scenario: Consumer supplies compliant product branding

**Given**

- The consumer supplies a product name and supporting login copy.
- The consumer supplies a compliant bundled PNG or WebP logo.
- The consumer supplies non-blank logo alternative text.

**When**

- `OrcaLogin` renders.

**Then**

- The product name, supporting copy, logo, and alternative text are presented.
- The logo fits within the specified presentation boundary without stretching,
  clipping, or layout overflow.
- `Powered by Orca` and `© 2026 Chen Chih-hao` remain visible.

### Scenario: Consumer supplies no customer logo

**Given**

- The consumer supplies product name and supporting login copy.
- The consumer supplies no customer logo.

**When**

- `OrcaLogin` renders.

**Then**

- A generic neutral fallback mark is displayed.
- The product name remains visible.
- The fallback does not appear to be a customer or Orca logo.
- Mandatory Orca attribution and copyright remain visible.

### Scenario: Customer logo alternative text is invalid

**Given**

- The consumer supplies a customer logo.
- The logo alternative text is missing, blank, or whitespace only.

**When**

- `OrcaLogin` renders.

**Then**

- The customer logo is not rendered.
- The generic neutral fallback is displayed.
- No inaccessible or broken customer-logo image is displayed.

### Scenario: Consumer branding changes

**Given**

- Two consumer configurations provide different product names, supporting
  copy, and compliant logos.

**When**

- Each configuration renders and submits login requests.

**Then**

- Each user sees the configured consumer branding.
- Both consumers use the same Orca login request, stable error, result, and
  client diagnostics behavior.
- Both consumers display the same mandatory Orca attribution and copyright.
- Neither consumer reads, parses, displays, or persists raw `ORCA_SESSION`.

### Scenario: Login succeeds in the consumer host

**Given**

- A registered user submits valid credentials through `OrcaLogin`.

**When**

- The existing backend returns the `frontend-01` successful login response.

**Then**

- The browser receives and manages the existing opaque session cookie.
- The existing safe login success result is displayed.
- No user id, actor id, session id, cookie value, profile, role, or
  organization detail is displayed.
- No protected fixture command, logout, navigation, or session inspection is
  performed.

### Scenario: Login or client request fails in the consumer host

**Given**

- The consumer uses `OrcaLogin`.
- The backend rejects login, returns another stable error, or the client
  encounters a classified `frontend-02` failure.

**When**

- The public login package handles the result.

**Then**

- Existing `frontend-01` and `frontend-02` behavior is preserved.
- The consumer does not replace the error parser, reference rules, diagnostic
  classification, or safe fallback.
- Branding does not change the resulting code, message, support reference, or
  sensitive-data boundary.

### Scenario: Consumer attempts an unsupported integration

**Given**

- A consumer copies Orca login source or imports a deep/internal Orca frontend
  path.

**When**

- Consumer contract conformance is evaluated.

**Then**

- The consumer does not satisfy the `frontend-03` public integration contract.
- Only the package-root dependency and exports are treated as supported.

## Acceptance Criteria

- Orca MUST provide the package
  `@oneofwolvesbilly/orca-react-login`.
- The package MUST support root imports of `OrcaLogin` and
  `OrcaLoginBranding`.
- The package MUST NOT support `/src/*` or other deep/internal import paths.
- The React Minimal Consumer Fixture MUST use a normal frontend build
  dependency on the package.
- The fixture MUST be independently structured from the Orca React reference
  application.
- The fixture MUST NOT copy Orca login components, API adapters, stable error
  parsing, client error catalog, or client diagnostics implementation.
- `OrcaLogin` MUST preserve the implemented `frontend-01` and `frontend-02`
  React behavior.
- Branding MUST be limited to product name, supporting login copy, optional
  compliant customer logo, and required logo alternative text.
- A customer logo MUST be a build-time bundled PNG or WebP no larger than
  256 KiB.
- A customer logo MUST have trimmed, non-blank consumer-provided alternative
  text.
- Missing or invalid logo alternative text MUST prevent the customer logo from
  rendering and MUST select the neutral fallback.
- Missing customer logo MUST select the neutral fallback.
- The neutral fallback MUST NOT appear to be a customer or Orca logo.
- The customer logo MUST render no larger than 64 x 64 CSS pixels and MUST NOT
  stretch, clip, or overflow the login layout.
- Remote URLs, runtime same-origin static-asset configuration, raw markup,
  inline image data, and SVG MUST NOT be supported logo inputs.
- Every supported composition MUST display exact `Powered by Orca` attribution.
- The attribution MUST link to the Orca GitHub repository in a new browsing
  context with `noopener noreferrer` protection.
- Every supported composition MUST display exact
  `© 2026 Chen Chih-hao` copyright.
- Consumer branding MUST NOT replace, hide, translate, relabel, or remove the
  attribution or copyright through the public API.
- Consumer branding MUST NOT change login, session, stable error, client
  diagnostic, audit, or product business behavior.
- Frontend application code MUST NOT read, parse, display, or persist raw
  `ORCA_SESSION`.
- This slice MUST NOT invoke a protected fixture command or logout.
- This slice MUST NOT introduce protected route, current-user inspection,
  session restoration, workspace, navigation, or organization UI behavior.
- This slice MUST NOT publish the package to a registry.
- This slice MUST NOT require backend or database changes.

## Invariants

- `frontend-01` remains authoritative for login request and result behavior.
- `frontend-02` remains authoritative for client failure observability.
- Auth remains authoritative for credential decisions, session creation, and
  `ORCA_SESSION`.
- Reference-core remains authoritative for stable backend errors and persisted
  client diagnostic references.
- Consumer presentation input cannot become auth, session, error, diagnostic,
  audit, or product business input.
- Public package consumption does not make Orca implementation internals part
  of the supported contract.
- Mandatory Orca attribution remains distinct from customer branding.
- The raw session cookie remains opaque to frontend application code.

## Sensitive Data Boundary

The public package, branding input, fixture source, frontend state, rendered
output, logs, and tests must not expose or persist:

- password after the existing submission lifecycle;
- raw `ORCA_SESSION` value or session id;
- request or response headers;
- raw request or response bodies;
- credential, registered-user, or account state;
- actor, user, personnel, role, organization, membership, or profile details;
- raw exception messages, exception types, or stack traces;
- browser storage contents;
- auth-owned login audit details beyond the existing opaque login failure
  reference;
- reference-core diagnostic details beyond the existing opaque client failure
  reference.

Customer logo assets and alternative text are presentation inputs. They must
not carry executable markup, remote resource references, credential data,
session data, or product business rules.

## Verification Requirements

Public package tests must verify:

- package-root exports are usable;
- deep/internal paths are not exported;
- existing login success, stable error, login reference, client diagnostics,
  and safe fallback behavior remains unchanged;
- consumer branding changes presentation only;
- mandatory attribution and copyright cannot be disabled through public input;
- logo fallback and alternative-text behavior;
- logo layout limits and overflow protection;
- raw session values remain absent from public types, state, and output.

Consumer contract tests must verify:

- the independent fixture declares a normal package dependency;
- fixture source imports only the package root;
- fixture source contains no copied login form, login API adapter, stable error
  parser, client error catalog, or client diagnostic adapter;
- compliant branding renders with mandatory Orca attribution;
- missing or invalid logo input selects the safe fallback;
- login success and failure behavior comes from the package;
- no protected fixture command, logout, or session lifecycle behavior is
  introduced.

Build verification must prove:

- the public package builds;
- the existing Orca React reference application builds against the supported
  composition;
- the independent React Minimal Consumer Fixture builds through the normal
  dependency;
- no registry publication is required.

## Non-Goals

- A second implementation of `frontend-01` or `frontend-02`.
- Source copying or deep/internal imports.
- Product-neutral protected fixture command.
- Authenticated actor resolution in the React fixture.
- Logout or post-logout command rejection.
- Protected route or product navigation.
- Current-user or session inspection.
- Refresh session restoration.
- Product workspace or organization UI.
- User profile UI.
- Complete white-label behavior.
- Design system or theme-token system.
- Customer CSS override system.
- SVG sanitization.
- Remote logo fetching.
- Runtime same-origin logo configuration.
- Raw HTML or markup branding input.
- Localization.
- Vue or Angular implementation.
- Package registry publication.
- Production hosting, CORS, CDN, or deployment behavior.
- Backend API, auth, session, error, diagnostic, audit, database, or
  organization behavior changes.

## Follow-up Boundary

The protected session lifecycle remains a separate future slice. Only after
`frontend-03` is complete may provisional `frontend-04` enter intake for:

- invoking the existing product-neutral protected fixture command;
- resolving an authenticated actor through the existing `auth-12` boundary;
- invoking existing Orca logout;
- verifying post-logout command rejection;
- preserving stable unauthenticated error behavior.

`frontend-04` must not be implemented as part of this slice.
