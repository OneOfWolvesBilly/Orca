# Orca

Orca is a portfolio-grade full-stack architecture showcase built around a
reusable authentication, session, audit, and logging core.

The project demonstrates how a product foundation can be specified, modeled,
tested, implemented, exposed through backend APIs, consumed by frontend clients,
backed by database migrations, and prepared for local and future cloud runtime
support.

Orca is not positioned as a production-ready identity platform. It is a
verifiable reference project: capabilities exist only when they are described by
authoritative specifications, reflected in derived design notes, covered by
tests, and implemented in the correct layer.

---

## What It Demonstrates

Orca is designed to show end-to-end engineering capability across:

* frontend UI and client behavior
* backend architecture across domain, application, infrastructure, and web
  layers
* authentication and server-side session workflows
* API error handling and safe client diagnostics
* audit and logging interface boundaries for future consuming products
* database schema ownership through migrations
* local runtime support with Docker and MariaDB direction
* cloud-oriented deployment boundaries without premature cloud coupling
* specification-first development with DDD and TDD discipline

The goal is not to collect framework features. The goal is to show how a
serious product core can grow from explicit behavior, tested boundaries, and
reviewable architectural decisions.

---

## Current Status

Orca is under active development. The current focus is not feature completeness,
but building a foundation where every behavior has an explicit specification,
every invariant is enforced in the correct layer, and every change can be
reasoned about without guessing.

Already supported by authoritative slices:

* auth-owned registered user identity
* password login with server-side sessions
* protected command actor context from the `ORCA_SESSION` cookie
* auth-owned login failure audit with an opaque troubleshooting reference
* logout and session revocation
* auth-owned client session expiry coordination for browser presentation
* embedded consumer auth and actor-context integration through a public Orca
  boundary
* organization group creation and invitation lifecycle commands
* stable API error contract for backend HTTP APIs
* safe client diagnostic ingestion and protected lookup
* reusable product-neutral audit recording boundary
* reusable React login composition with bounded consumer branding
* React frontend login, client failure observability, and product-neutral
  protected session lifecycle reference
* local Docker Compose and MariaDB login runtime support

Planned or gap areas:

* account lifecycle checks
* credential setup, password reset, and recovery flows
* MFA, including future TOTP or QR-based authentication slices if specified
* future Passkey / WebAuthn support if specified
* structured application logging and correlation support
* broader frontend protected workflows
* production-oriented cloud deployment work

Planned capabilities are not implied to exist. They must enter through the same
SDD -> DDD -> TDD workflow before implementation.

---

## Product Direction

The concrete product direction is a reusable authentication and session core for
Billy's current and future products.

Orca's authentication and session direction includes:

* registered user identity
* password authentication
* server-side sessions
* session validation and protected command context
* logout and session revocation
* account lifecycle checks
* TOTP, recovery codes, QR-based authentication, or Passkey / WebAuthn support
  when separately specified

Orca may also expose small, stable support APIs for cross-cutting operational
needs. A reusable audit boundary should define product-neutral interfaces and
record envelopes so consuming products can record their own events without Orca
owning those products' business semantics.

Consuming products own their product-specific events, metadata, storage choices,
and audit decisions. Orca must not centrally define events such as
`alarm.triggered`, `permission.revoked`, or `evidence.added`.

Application logging and audit recording remain separate concerns. Application
logs support diagnostics and operations. Audit records answer who acted, what
action occurred, when it occurred, which resource was affected, and what outcome
was recorded. Future slices must not expose passwords, raw session values,
credential secrets, recovery codes, private keys, or full authentication tokens
through logs, diagnostics, or audit metadata.

---

## Engineering Method

Orca follows a strict SDD -> DDD -> TDD workflow.

This is not a guideline. It is the way the project is developed:

1. Specification-Driven Development defines behavior first.
2. Domain-Driven Design derives model boundaries and rule placement from the
   specification.
3. Test-Driven Development protects behavior before implementation details
   settle.

If a behavior is not specified, it does not exist. If a rule is not testable, it
does not belong in the domain.

Specifications live under `docs/specs/<bounded-context>/` or an approved
support scope such as `docs/specs/reference-core/`, `docs/specs/frontend/`, or
`docs/specs/deployment/`. They are the authoritative source of truth.

Derived DDD notes live under `docs/ddd/<bounded-context>/` and
`docs/ddd/<support-scope>/`. They explain modeling decisions derived from specs
and must not introduce new behavior.

Behavior slices are scoped by bounded context or approved support scope:

```text
organization-01
auth-08
reference-core-02
frontend-01
deployment-01
```

Domain code must stay independent from Spring, JPA, database schemas, security
frameworks, and HTTP concerns. Cross-aggregate coordination belongs in the
application layer. Infrastructure remains an adapter, not the source of
business behavior.

---

## Repository Guide

```text
orca/
├─ docs/
│  ├─ specs/                 # Authoritative behavior specifications
│  ├─ ddd/                   # Derived design notes
│  ├─ product/               # Product / SA baseline and workflow maps
│  ├─ slice-map.md           # Derived slice index
│  ├─ constraints.md         # Non-negotiable engineering rules
│  └─ document-map.md        # Documentation authority and reading order
│
├─ orca_backend/
│  └─ src/main/java/io/github/oneofwolvesbilly/orca/
│     ├─ auth/
│     ├─ organization/
│     └─ referencecore/
│
├─ orca_frontend/
│  ├─ react/
│  ├─ vue/
│  └─ angular/
├─ deploy/
└─ README.md
```

Recommended reading order:

1. `docs/document-map.md` - document authority and structure
2. `docs/constraints.md` - non-negotiable engineering constraints
3. `docs/product/orca-sa-baseline.md` - product / SA baseline
4. `docs/product/workflow-map.md` - workflow support, gaps, and unknowns
5. `docs/product/capability-map.md` - workflow-derived capability map
6. `docs/product/slice-intake-gate.md` - gate for future slices
7. `docs/specs/<bounded-context>/` and approved `docs/specs/<support-scope>/`
8. `docs/ddd/<bounded-context>/` and approved `docs/ddd/<support-scope>/`
9. `docs/slice-map.md`

If a document cannot be placed in this order, it likely does not belong in the
repository.
