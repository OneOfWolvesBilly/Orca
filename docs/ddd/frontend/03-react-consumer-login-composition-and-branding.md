# DDD Derivation - Frontend 03 React Consumer Login Composition and Branding

Status: Approved.

This note is **derived from**
`docs/specs/frontend/03-react-consumer-login-composition-and-branding.md`.
It does not introduce new behavior.

## Scope Ownership

**frontend delivery support scope**

Rationale:

- The slice packages and composes existing React delivery behavior.
- `frontend-01` continues to own login submission and result presentation.
- `frontend-02` continues to own client failure classification, diagnostic
  submission, and safe diagnostic reference presentation.
- Auth continues to own credential decisions, server-side sessions, and the
  opaque `ORCA_SESSION` cookie.
- Reference-core continues to own stable API errors and persisted client
  diagnostics.
- Consumer branding is presentation input, not domain or authenticated state.

No frontend bounded context, product domain, or business aggregate is created.

## No Aggregate Root

This slice introduces no aggregate root.

Why:

- The public React package does not own authoritative business state.
- Branding values are immutable composition input for one render tree.
- Login state remains request and presentation state from `frontend-01` and
  `frontend-02`.
- Logo conformance is a frontend integration contract, not a domain invariant.
- Attribution and copyright are fixed presentation rules from the spec.

The public component may hold existing form progress and result state, but that
state is not persisted and does not become a domain model.

## Dependency Direction

```text
React Minimal Consumer Fixture
  -> @oneofwolvesbilly/orca-react-login package root
     -> frontend-01 login composition
     -> frontend-02 client failure observability
        -> existing backend HTTP contracts

Orca React reference application
  -> @oneofwolvesbilly/orca-react-login package root

@oneofwolvesbilly/orca-react-login
  -> React peer dependency
  -> no consumer host dependency
  -> no backend implementation dependency
```

The dependency must not reverse from the Orca package into the consumer
fixture. The fixture supplies presentation input only; it does not supply login
adapters, error parsers, diagnostic behavior, attribution, or session logic.

## Package and Host Placement

Recommended repository placement:

```text
orca_frontend/
  packages/
    react-login/
      package.json
      src/
        index.ts
        ... internal implementation
  react/
    ... Orca reference application

minimal_consumer_fixture/
  react/
    package.json
    ... independent React consumer host
```

The exact internal filenames are implementation details. Only the npm package
name and package-root exports are supported contracts.

The first repository-local proof may use a package-manager `file:` dependency
or an equivalent normal local package dependency. It must resolve by package
name and package metadata, not by copying source or configuring a TypeScript
alias directly to an Orca `src` directory.

The existing Orca React application should also consume the public package.
This prevents the reference application and external fixture from drifting
into two login implementations.

## Public Export Boundary

Supported package:

```text
@oneofwolvesbilly/orca-react-login
```

Supported root exports:

```text
OrcaLogin
OrcaLoginBranding
```

Recommended package export map shape:

```text
exports
  "." -> built public entry
```

No wildcard or subpath export is derived. In particular, the package must not
export:

- `/src/*`;
- individual component files;
- login request or response adapters;
- stable API error parsing;
- client error catalogs;
- client diagnostic adapters;
- internal CSS or presentation helpers as supported JavaScript imports.

The public `index` entry should explicitly re-export the component and branding
type. Files existing inside the package do not become public merely because
they are present in the published or linked directory.

## Minimum Public Presentation Model

### `OrcaLogin`

`OrcaLogin` is the consumer-facing React composition root.

Responsibilities derived from the spec:

- receive one `OrcaLoginBranding` value;
- render the existing login form and result behavior;
- retain `frontend-02` client failure observability;
- choose customer-logo or neutral-fallback presentation;
- render mandatory Orca attribution and copyright;
- expose no option that replaces or disables fixed Core-owned elements.

It is not a protected route, session controller, product workspace, navigation
shell, or organization command surface.

### `OrcaLoginBranding`

Minimum conceptual fields:

```text
productName
supportingCopy
optional customerLogo
  bundledAssetSource
  alternativeText
```

The exact TypeScript property names may be chosen during implementation as long
as they express only the spec-approved values.

The branding type must not contain:

- endpoint or fetch overrides;
- error or support-reference overrides;
- diagnostic configuration;
- cookie or session configuration;
- arbitrary markup or component children used to replace the shell;
- remote asset configuration;
- attribution or copyright controls;
- product authorization or business data.

The type is a presentation input contract. It is not a product configuration
aggregate or runtime tenant-branding system.

## Customer Logo Conformance Boundary

The browser-visible source produced by a bundler is not sufficient by itself to
prove that a logo originated from a build-time import or that its source file
is no larger than 256 KiB. Conformance is therefore split across two frontend
verification layers without changing the spec behavior.

### Composition-time safety

The public package is responsible for:

- rendering no customer logo when the logo input is absent;
- rendering no customer logo when alternative text is missing, blank, or
  whitespace only;
- never accepting raw markup as the logo representation;
- not rendering explicitly remote, inline-data, or otherwise unsupported
  source forms;
- selecting the neutral fallback for invalid render input;
- bounding the rendered image to 64 x 64 CSS pixels;
- using contain behavior without stretching, clipping, or overflow.

### Consumer build and contract safety

The independent fixture contract is responsible for proving:

- the logo is imported from a consumer-owned build-time asset;
- the source file extension is PNG or WebP;
- the source file size is no larger than 256 KiB;
- no remote URL, same-origin runtime configuration, raw markup, inline data, or
  SVG is supplied;
- alternative text is provided with the asset.

Source dimensions of 512 x 512 pixels and a 1:1 aspect ratio remain recommended
input guidance. The required rendered-size and overflow behavior is enforced by
the public composition.

This two-layer placement keeps filesystem and bundler inspection out of a React
rendering component while still making the integration contract testable.

## Logo Fallback Model

The fallback is a fixed package-owned presentation element.

It must:

- be generic and neutral;
- remain visually distinct from customer-provided branding;
- not use an Orca logo;
- keep the product name visible;
- avoid a broken image element;
- be selected for absent or invalid customer-logo input.

The fallback carries no claim that it is the customer logo. It is not generated
from product-name initials because the approved decision selected a generic
neutral mark.

## Orca Attribution Model

Attribution is package-owned fixed presentation, not branding input.

Package-owned constants conceptually include:

```text
attribution text: Powered by Orca
attribution URL: https://github.com/OneOfWolvesBilly/Orca
copyright: © 2026 Chen Chih-hao
```

The attribution link opens in a new browsing context with `noopener` and
`noreferrer` relationship protection.

`OrcaLoginBranding` must have no field for:

- attribution visibility;
- attribution text or translation;
- attribution URL or target;
- copyright text, owner, or year;
- replacement footer content.

The component owns the placement of these elements within the supported login
composition. Consumer inputs cannot disable them through the public API.

## Existing Login Behavior Reuse

The package must contain one implementation path for the completed React
behavior.

Responsibilities retained from `frontend-01`:

- credential form and submission progress;
- password clearing after submission;
- `POST /api/auth/login` through the existing adapter;
- `credentials: include` browser cookie behavior;
- safe `204` success result;
- stable error parsing;
- login failure reference presentation;
- generic safe failure presentation;
- no session inspection after refresh.

Responsibilities retained from `frontend-02`:

- transport, malformed-response, and unexpected-response classification;
- at most one client diagnostic submission attempt;
- `REQUEST_UNAVAILABLE` presentation;
- client failure reference only after successful persistence;
- no recursive diagnostic reporting, retry, queue, or browser persistence.

The existing React reference application should become a thin composition host
that supplies Orca branding to the same `OrcaLogin` used by the fixture. It
must not retain a second private login composition after extraction.

## Rule Placement

### Auth rules

- Auth decides login success or rejection.
- Auth owns server-side session creation and the opaque `ORCA_SESSION` cookie.
- Auth owns `loginFailureReferenceId`.
- No auth rule changes are derived.

### Reference-core rules

- Reference-core owns stable API errors.
- Reference-core owns client diagnostic persistence and
  `clientFailureReferenceId`.
- No reference-core rule changes are derived.

### Existing frontend adapter rules

- Preserve the `frontend-01` request, result, and sensitive-data behavior.
- Preserve the `frontend-02` classification, diagnostic, and fallback
  behavior.
- Keep raw session values outside application code and public types.

### Public package rules

- Expose only the approved package-root component and branding type.
- Own the complete reusable React login composition.
- Normalize branding into customer-logo or neutral-fallback presentation.
- Render fixed attribution and copyright outside consumer-controlled input.
- Provide no session lifecycle or product behavior.

### Consumer fixture rules

- Declare a normal package dependency.
- Import only from the package root.
- Supply only approved branding values.
- Bundle a compliant customer asset for the positive contract example.
- Contain no copied Orca login or diagnostic implementation.
- Invoke no protected fixture command or logout in this slice.

### Build configuration rules

- Package metadata exposes only the root entry.
- The package, Orca React reference host, and fixture build independently.
- Local dependency wiring does not require registry publication.
- TypeScript aliases to Orca internal source do not substitute for a package
  dependency.

## Sensitive Data Design

Reuse the structural boundaries from `frontend-01` and `frontend-02` rather
than adding redaction after data reaches branding or fixture code.

Branding objects and package public types must never receive:

- login request objects;
- submitted passwords;
- raw responses or headers;
- cookie or browser storage access;
- raw session or session id values;
- exception objects;
- user, actor, personnel, role, organization, membership, or profile objects;
- login audit or diagnostic record details.

The package may retain the existing opaque support-reference presentation
values only as already allowed by `frontend-01` and `frontend-02`.

Customer assets and alternative text remain isolated presentation inputs. They
do not enter request, diagnostic, audit, or session state.

## Test Layer Placement

### Public package unit and component tests

Validate:

- `OrcaLogin` and `OrcaLoginBranding` are exported at the package root;
- compliant branding renders product name, supporting copy, logo, and alt;
- absent logo renders the neutral fallback;
- missing, blank, or whitespace-only alt prevents customer-logo rendering;
- invalid render source selects the fallback;
- customer logo is bounded without stretching, clipping, or overflow;
- exact attribution text, link, target, and relationship protection;
- exact copyright text;
- the branding type/API has no attribution override;
- branding changes do not change login requests or result behavior;
- raw session values remain absent.

### Existing behavior regression tests

Move or adapt the implemented React behavior tests so they execute against the
public composition and continue validating:

- safe login success;
- stable login rejection and login reference;
- stable non-login errors;
- transport, malformed, and unexpected response diagnostics;
- diagnostic failure fallback;
- password clearing;
- sensitive-data exclusions.

Tests must not be duplicated into independent implementations for the Orca
reference host and fixture.

### Package contract tests

Validate package metadata and resolution:

- the root import resolves;
- the two approved exports are present;
- `/src/*` and representative internal subpaths do not resolve as exports;
- built declaration output exposes no internal adapter or session type;
- React is consumed with the intended package dependency direction.

### Consumer fixture contract tests

Validate:

- the fixture package manifest declares the normal local dependency;
- fixture imports use only the package root;
- no copied login form, login adapter, error parser, client error catalog, or
  diagnostic adapter exists in fixture source;
- the positive fixture logo is a bundled PNG or WebP at or below 256 KiB;
- no remote, same-origin runtime, inline-data, raw-markup, or SVG input is
  configured;
- compliant branding and fixed Orca attribution render together;
- invalid/missing logo input uses the neutral fallback;
- login behavior is supplied by the dependency;
- no protected fixture command, logout, actor context, or session inspection
  is invoked.

### Build verification

Validate independently:

- public package build;
- Orca React reference application build;
- React Minimal Consumer Fixture build;
- fixture dependency resolution without registry publication.

### Backend test boundary

No backend, domain, application, persistence, migration, or HTTP integration
test changes are derived. Existing backend contracts are consumed unchanged.

## TDD Order

When implementation is authorized, the recommended frontend TDD sequence is:

1. package export and subpath contract tests;
2. branding, logo fallback, attribution, and accessibility component tests;
3. existing login and client-diagnostic regression tests against
   `OrcaLogin`;
4. independent fixture dependency and source-boundary contract tests;
5. fixture rendering and login-consumption tests;
6. package, reference-host, and fixture build verification.

This is frontend TDD. No backend domain test phase is needed because the slice
introduces no domain behavior.

## Implementation Sequencing Boundary

After tests define the public contract, implementation may:

1. create the package metadata and root entry;
2. move the existing reusable React login implementation behind that entry;
3. add the approved branding, fallback, and fixed attribution composition;
4. migrate the Orca React reference host to the package;
5. create the independent React fixture using the normal dependency;
6. verify package and host builds.

This sequence must preserve one login implementation and must not create
temporary copied implementations as the final state.

## Unknown / To Be Discovered

- Production registry and publication coordinates.
- Production package versioning and release automation.
- Production frontend hosting and CORS/deployment policy.
- Future SVG sanitization and external-resource policy.
- Localization and translation ownership.
- Vue and Angular consumer package strategy.
- Future design-system or theme-token ownership.

These unknowns do not change or block the repository-local contract in this
slice.

## Non-Goals

- Frontend domain model or aggregate.
- A second login or client diagnostics implementation.
- Deep or internal package exports.
- Cross-framework UI abstraction.
- Vue or Angular implementation.
- Runtime tenant-branding service.
- Complete white-label behavior.
- Design-system, theme-token, or customer CSS override API.
- SVG sanitization or remote logo fetching.
- Runtime same-origin logo configuration.
- Raw markup or inline-image branding input.
- Protected fixture command or actor resolution.
- Logout or post-logout rejection.
- Protected route, session inspection, or restoration.
- Product workspace, navigation, organization UI, or profile UI.
- Backend, auth, reference-core, audit, database, or organization changes.
- Registry publication.
- Production deployment behavior.

## Follow-up Boundary

Provisional `frontend-04` remains responsible for the protected session
lifecycle proof after `frontend-03` is complete. This DDD note derives no
protected command, authenticated actor, logout, post-logout rejection, route,
session inspection, or restoration behavior.
