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
- `docs/specs/*`

Behavior changes must start here.

---

### 1.2 Derived (Explains, Must Not Add Behavior)

These documents explain structure and decisions derived from specs and tests.
They must not introduce new behavior.

Examples:

- `docs/orca-architecture.md`
- `docs/ddd/*` (derivation notes per slice)
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
  1) update `docs/specs/*` first
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
4) `docs/specs/*`
5) Derived documents (`docs/ddd/*`, `docs/orca-architecture.md`, etc.)

If a document cannot be placed in this order, it likely does not belong in the repo.
