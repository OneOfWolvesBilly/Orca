# Document Map & Authority Levels

This document defines **which files are authoritative** and how documentation
in this repository should be read and maintained.

If documents conflict, implementation must follow the highest-authority source.

---

## 1. Authority Levels

### 1.1 Authoritative (Drives Implementation)

These documents define project truth and may directly guide future code and
tests:

- `README.md`
- `docs/document-map.md`
- `docs/constraints.md`
- `docs/product/orca-sa-baseline.md`
- `docs/product/workflow-map.md`
- `docs/product/capability-map.md`
- `docs/product/slice-intake-gate.md`
- `docs/specs/<bounded-context>/*`
- `docs/specs/reference-core/*` for cross-cutting support behavior
- `docs/specs/frontend/*` for approved frontend delivery support behavior
- `docs/specs/deployment/*` for approved delivery/runtime support behavior

Product / SA documents define product positioning, workflow gaps, capability
maps, and slice intake rules. Behavior changes still require authoritative
specs under `docs/specs/<bounded-context>/*` before implementation.
Cross-cutting reference-core behavior may be specified under
`docs/specs/reference-core/*` only when slice intake confirms that no single
domain bounded context owns the behavior.
Frontend and deployment behavior may be specified under their support scopes
only after slice intake confirms that the work is delivery/runtime support,
not domain bounded-context behavior. Deployment is a delivery/runtime support
scope, not a bounded context, and must not own or redefine auth,
organization, reference-core, or frontend business rules.

---

### 1.2 Derived (Explains, Must Not Add Behavior)

These documents explain structure and decisions derived from specs and tests.
They must not introduce new behavior.

Examples:

- `docs/orca-architecture.md`
- `docs/ddd/<bounded-context>/*` (derivation notes per slice)
- `docs/ddd/reference-core/*` (cross-cutting support derivation notes)
- `docs/ddd/frontend/*` (frontend delivery support derivation notes)
- `docs/ddd/deployment/*` (deployment runtime support derivation notes)
- `docs/slice-map.md` (derived slice index)
- any roadmap/system-design notes

Derived documents may be rewritten for clarity, but must remain consistent with specs.

---

### 1.3 Reference Only (Non-authoritative)

These documents are retained for historical context only.
They must never drive implementation unless explicitly requested.

- `docs/archive/**`

---

## 2. Update Rules

- To change behavior:
  1) verify the slice traces to a product workflow gap in `docs/product/*`
  2) update `docs/specs/<bounded-context>/*` or the approved
     `docs/specs/<support-scope>/*` support scope
  3) regenerate/update derived notes if needed
  4) update tests and code to match the spec

- To add deployment support behavior:
  1) pass slice intake as a delivery/runtime support scope
  2) define the required local environment preflight inventory before any
     install, upgrade, manifest, script, secret, Docker, or Kubernetes action
  3) update `docs/specs/deployment/*` before any manifests, scripts, secrets,
     Docker, or Kubernetes execution
  4) update `docs/ddd/deployment/*` only as derived notes
  5) preserve auth, organization, reference-core, and frontend business rules

- Architecture documents must be updated only after specs/tests are stable.
- Code must not introduce behavior not described by specs.
- Future slices that cannot be traced to a workflow gap must not enter SDD.

---

## 3. Reading Order

Recommended reading order for new contributors:

1) `README.md`
2) `docs/document-map.md`
3) `docs/constraints.md`
4) `docs/product/orca-sa-baseline.md`
5) `docs/product/workflow-map.md`
6) `docs/product/capability-map.md`
7) `docs/product/slice-intake-gate.md`
8) `docs/specs/<bounded-context>/*` and approved `docs/specs/<support-scope>/*`
9) `docs/ddd/<bounded-context>/*` and approved `docs/ddd/<support-scope>/*`
10) `docs/slice-map.md`
11) Other derived documents (`docs/orca-architecture.md`, etc.)

If a document cannot be placed in this order, it likely does not belong in the repo.

---

## 4. Slice Naming

Behavior slices are scoped by bounded context, not by a single global sequence.

Use this file layout:

```text
docs/specs/<bounded-context>/<NN>-<behavior-or-integration>.md
docs/ddd/<bounded-context>/<NN>-<behavior-or-integration>.md
```

Cross-cutting support behavior that does not belong to one domain bounded
context uses:

```text
docs/specs/reference-core/<NN>-<behavior-or-integration>.md
docs/ddd/reference-core/<NN>-<behavior-or-integration>.md
```

`reference-core` is a support scope, not a domain bounded context. It must not
own or redefine bounded-context business rules.

Delivery/runtime support behavior uses an approved support scope:

```text
docs/specs/<support-scope>/<NN>-<behavior-or-integration>.md
docs/ddd/<support-scope>/<NN>-<behavior-or-integration>.md
```

Approved support scopes currently include:

- `reference-core` for cross-cutting backend support behavior
- `frontend` for frontend delivery support behavior
- `deployment` for delivery/runtime support behavior

`deployment` slices describe runtime wiring, configuration boundaries, and
operational enablement for already-specified behavior. They must not create or
change auth, organization, reference-core, or frontend business rules.

The spec file is authoritative for behavior. The matching DDD file is derived
from the spec and explains model boundaries, rule placement, and test-layer
placement without adding behavior.

Use this slice id format when referring to a slice in prose:

```text
<bounded-context>-<NN>
```

Examples:

- `organization-01`
- `organization-08`
- `auth-01`
- `reference-core-01`
- `frontend-01`
- `deployment-01`

Frontend slices are delivery slices, not a bounded context by default.
Place frontend specs under the bounded context whose behavior they expose.
Only introduce a separate frontend/platform context for cross-context shell behavior
after that behavior is explicitly specified.

Future slice ids should be assigned only after the slice intake gate confirms
the slice is traceable to a workflow gap or existing workflow protection need.
