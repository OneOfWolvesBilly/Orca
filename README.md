# Orca

Orca is an enterprise application core reference project that showcases a disciplined **Specification-Driven Development (SDD)**, **Domain-Driven Design (DDD)**, and **Test-Driven Development (TDD)** workflow.

It is not a production system; all design and implementation decisions are derived from explicit specifications to ensure the workflow is verifiable and repeatable.

---

## Project Status

Orca is under active development.

The current focus is **not feature completeness**, but establishing a foundation where:

* every behavior has an explicit specification
* every invariant is enforced in the correct layer
* every change can be reasoned about without guessing

Features are added only when their behavior can be clearly specified and tested.

---

## Core Design Position

Orca follows a **strict SDD → DDD → TDD workflow**.

This is not a guideline.
It is the way the project is developed.

If a behavior is not specified, it does not exist.
If a rule is not testable, it does not belong in the domain.

---

## 1. Specification-Driven Development (SDD)

Development always starts from **behavior**, not from architecture or data models.

Each user-visible behavior is captured as a specification using:

* **Given / When / Then**
* acceptance criteria
* invariants
* error cases
* explicit non-goals

Specifications live under `docs/specs/<bounded-context>/` or an explicitly
approved support scope such as `docs/specs/reference-core/`,
`docs/specs/frontend/`, or `docs/specs/deployment/`. They are the
**authoritative source of truth**.

A specification answers one question only:

> *What must the system guarantee, regardless of implementation?*

Specifications intentionally avoid:

* class names
* frameworks
* APIs
* database schemas

Those details belong downstream.

---

## 2. Domain-Driven Design (DDD)

Domain models are **derived from specifications**, never designed in isolation.

From each spec, we determine:

* the bounded context it belongs to
* the aggregate root responsible for enforcing invariants
* which rules are **true domain invariants**
* which rules require coordination with external state (application rules)

Each bounded context is represented as a top-level module.

```text
<bounded-context>/
  domain/
  application/
  infrastructure/
  web/
```

This structure is intentional.

If code feels hard to place, that is a design signal — not a reason to “just put it somewhere”.

---

## 3. Test-Driven Development (TDD)

Tests exist to **protect behavior**, not to validate implementation details.

The project distinguishes clearly between test layers:

* **Domain tests**

  * pure Java
  * no Spring, no database
  * verify invariants and state transitions
* **Application tests**

  * verify orchestration and cross-aggregate rules
* **Infrastructure tests**

  * added last, only when necessary

Domain code is written **only** to satisfy existing tests and specifications.

If a rule is not tested at the correct layer, it is considered unstable.

---

## Behavior Slices vs. Bounded Contexts

Orca grows **behavior by behavior**, not module by module.

* A **behavior slice** is a single user action
  (e.g. `create-group`, `invite-member`)
* A **bounded context** is a semantic domain
  (e.g. `organization`, `issue`)

Each domain behavior slice:

* adds or extends code *inside* an existing bounded context
* does **not** introduce new layers or ad-hoc modules

This prevents architectural sprawl and keeps ownership clear.

Cross-cutting support behavior that protects or exposes multiple bounded
contexts may use the `reference-core` support scope after passing slice intake.
`reference-core` is not a domain bounded context and must not own or redefine
bounded-context business rules.

Delivery and runtime support behavior may use approved support scopes after
passing slice intake. `frontend` is a delivery support scope for client-facing
behavior that consumes backend APIs. `deployment` is a delivery/runtime support
scope for local or production runtime wiring, configuration boundaries, and
operational enablement. Neither scope is a domain bounded context.

Deployment support slices must not own, redefine, or rewrite auth,
organization, reference-core, or frontend business behavior. They may describe
how already-specified behavior is run, configured, exposed, or protected at
runtime, but the owning spec remains the source of business truth.

Slice identifiers are scoped by bounded context or an approved support scope:

```text
<bounded-context>-<NN>
```

Examples:

```text
organization-01
organization-08
auth-01
reference-core-01
frontend-01
deployment-01
```

The file layout mirrors that scope:

```text
docs/specs/organization/08-web-api-integration.md
docs/ddd/organization/08-web-api-integration.md
```

Cross-cutting support slices mirror the same layout:

```text
docs/specs/reference-core/01-stable-api-error-contract.md
docs/ddd/reference-core/01-stable-api-error-contract.md
```

Delivery/runtime support slices also mirror the same layout:

```text
docs/specs/deployment/01-secure-local-runtime-boundary.md
docs/ddd/deployment/01-secure-local-runtime-boundary.md
```

Frontend work is not a bounded context by default.
Frontend slices should live under the bounded context whose behavior they expose
(for example `organization/09-frontend-command-console.md`).
A frontend-only cross-context shell/navigation slice may be introduced separately
only when its behavior is explicitly specified.

---

## Repository Structure

```text
orca/
├─ docs/
│  ├─ specs/                 # Authoritative behavior specifications
│  │  ├─ <bounded-context>/   # Context-scoped behavior slices
│  │  └─ <support-scope>/     # Approved support scopes, e.g. reference-core, frontend, deployment
│  ├─ ddd/                   # Derived design notes
│  │  ├─ <bounded-context>/
│  │  └─ <support-scope>/
│  ├─ product/               # Product / SA baseline and workflow maps
│  ├─ slice-map.md           # Derived slice index
│  ├─ constraints.md         # Non-negotiable engineering rules
│  └─ document-map.md        # Documentation authority and reading order
│
├─ orca_backend/
│  └─ src/main/java/io/github/oneofwolvesbilly/orca/
│     ├─ <bounded-context>/
│     │  ├─ domain/
│     │  ├─ application/
│     │  ├─ infrastructure/
│     │  └─ web/
│     └─ referencecore/
│        └─ web/             # Cross-cutting HTTP support, not a domain context
│
├─ orca_frontend/              # Framework application container
│  ├─ react/                   # Reference implementation
│  ├─ vue/                     # Planned framework port
│  └─ angular/                 # Planned framework port
├─ deploy/
└─ README.md
```

Bounded contexts are introduced by behavior slices.
This README intentionally does not enumerate them.
The authoritative behavior definitions live in `docs/specs/<bounded-context>/*`
and approved `docs/specs/<support-scope>/*` support slices such as
`reference-core`, `frontend`, and `deployment`. Derived DDD notes live in
`docs/ddd/<bounded-context>/*` and `docs/ddd/<support-scope>/*`; they explain
modeling decisions derived from specs and must not introduce behavior.
Product / SA baseline documents live in `docs/product/*`; they explain product
positioning, workflow gaps, capability maps, and future slice intake rules.
The derived slice index lives in `docs/slice-map.md`.

---

## Documentation Entry Point

New contributors should read documents in the following order:

1. `docs/document-map.md` — document authority and structure
2. `docs/constraints.md` — non-negotiable engineering constraints
3. `docs/product/orca-sa-baseline.md` — product / SA baseline
4. `docs/product/workflow-map.md` — workflow support, gaps, and unknowns
5. `docs/product/capability-map.md` — workflow-derived capability map
6. `docs/product/slice-intake-gate.md` — gate for future slices
7. `docs/specs/<bounded-context>/` and approved `docs/specs/<support-scope>/` — authoritative behavior definitions
8. `docs/ddd/<bounded-context>/` and approved `docs/ddd/<support-scope>/` — derived DDD notes for those specs
9. `docs/slice-map.md` — derived slice index
10. Other derived documents (architecture, design notes), if present

If a document cannot be placed in this order,
it likely does not belong in the repository.

---

## What This Repository Is (and Is Not)

This repository **is**:

* a demonstration of disciplined backend and frontend engineering
* a reference implementation of SDD / DDD / TDD working together
* a system that evolves from verified behavior, not assumptions

This repository is **not**:

* a feature checklist
* a framework comparison
* a big-design-up-front exercise

If something feels strict, it is probably intentional.
