# Orca

Orca is a demonstration project that showcases a disciplined **Specification-Driven Development (SDD)**, **Domain-Driven Design (DDD)**, and **Test-Driven Development (TDD)** workflow.

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

Specifications live in `docs/specs/` and are the **authoritative source of truth**.

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

Each slice:

* adds or extends code *inside* an existing bounded context
* does **not** introduce new layers or ad-hoc modules

This prevents architectural sprawl and keeps ownership clear.

---

## Repository Structure

```text
orca/
├─ docs/
│  ├─ specs/                 # Authoritative behavior specifications
│  ├─ constraints.md         # Non-negotiable engineering rules
│  └─ document-map.md        # Documentation authority and reading order
│
├─ orca_backend/
│  └─ src/main/java/io/github/oneofwolvesbilly/orca/
│     └─ <bounded-context>/
│        ├─ domain/
│        ├─ application/
│        ├─ infrastructure/
│        └─ web/
│
├─ orca_frontend/
├─ deploy/
└─ README.md
```

Bounded contexts are introduced by behavior slices.
This README intentionally does not enumerate them.
The authoritative behavior list lives in docs/specs/*

---

## Documentation Entry Point

New contributors should read documents in the following order:

1. `docs/document-map.md` — document authority and structure
2. `docs/constraints.md` — non-negotiable engineering constraints
3. `docs/specs/` — behavior definitions
4. Derived documents (architecture, design notes), if present

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
