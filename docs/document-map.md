# Document Map & Authority Levels

This document defines **which files are authoritative** and how documentation
in this repository should be read and maintained.

If documents conflict, implementation must follow the highest-authority source.

---

## 1. Authority Levels

### 1.1 Authoritative (Drives Implementation)

These documents define truth and may directly drive code and tests:

- `README.md`
- `docs/document-map.md`
- `docs/constraints.md`
- `docs/specs/<bounded-context>/*`

Behavior changes must start here.

---

### 1.2 Derived (Explains, Must Not Add Behavior)

These documents explain structure and decisions derived from specs and tests.
They must not introduce new behavior.

Examples:

- `docs/orca-architecture.md`
- `docs/ddd/<bounded-context>/*` (derivation notes per slice)
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
  1) update `docs/specs/<bounded-context>/*` first
  2) regenerate/update derived notes if needed
  3) update tests and code to match the spec

- Architecture documents must be updated only after specs/tests are stable.
- Code must not introduce behavior not described by specs.

---

## 3. Reading Order

Recommended reading order for new contributors:

1) `README.md`
2) `docs/document-map.md`
3) `docs/constraints.md`
4) `docs/specs/<bounded-context>/*`
5) `docs/ddd/<bounded-context>/*`
6) `docs/slice-map.md`
7) Other derived documents (`docs/orca-architecture.md`, etc.)

If a document cannot be placed in this order, it likely does not belong in the repo.

---

## 4. Slice Naming

Behavior slices are scoped by bounded context, not by a single global sequence.

Use this file layout:

```text
docs/specs/<bounded-context>/<NN>-<behavior-or-integration>.md
docs/ddd/<bounded-context>/<NN>-<behavior-or-integration>.md
```

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

Frontend slices are delivery slices, not a bounded context by default.
Place frontend specs under the bounded context whose behavior they expose.
Only introduce a separate frontend/platform context for cross-context shell behavior
after that behavior is explicitly specified.
