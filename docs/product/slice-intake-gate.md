# Slice Intake Gate

Every future Orca slice must pass this gate before entering SDD.

The gate exists to prevent future work from being derived only from technical
ideas, framework features, or AI-generated roadmaps.

---

## Required Questions

Before creating a new spec, answer:

1. Which workflow does this slice belong to?
2. Which workflow gap does it close?
3. Who is the primary actor?
4. What successful result is visible to that actor, an API client, or an
   operator?
5. What are the alternative or failure flows?
6. Does it change an existing workflow?
7. Is it only internal technical work?
8. If it is internal technical work, which existing workflow does it protect,
   improve, or make supportable?
9. Which authoritative docs prove predecessor slices already exist?
10. Does the prompt background conflict with repo state?
11. Are any business rules being guessed instead of discovered?

If any required answer is missing, the slice should not enter SDD.

---

## Allowed Slice Sources

A slice may enter SDD when it is derived from:

- a user workflow gap
- an operational workflow gap
- an existing workflow protection need
- a supportability, auditability, reliability, performance, or frontend
  integration need that is tied to an existing workflow

Examples:

- Login failure audit is allowed because `auth-08` and `auth-09` explicitly
  identify it as a missing operational support behavior.
- Global exception handling is allowed if it defines a stable API error
  contract needed by current HTTP workflows and future frontend consumption.
- Positive registered-user existence cache is allowed if it preserves the
  existing invitation workflow while reducing repeated lookup load.

---

## Disallowed Slice Sources

A slice must not enter SDD only because:

- a framework supports it
- a database table could be added
- a cache technology should be demonstrated
- a controller endpoint list looks convenient
- a generic enterprise checklist says the feature exists
- an AI suggestion names a future feature
- another product domain, such as CRM or project management, would need it

Those ideas may become slices only after they are tied to a specific Orca
workflow gap.

---

## Technical / Infrastructure Slice Rule

Infrastructure slices are allowed only when they protect or enable an existing
or planned workflow.

They must specify:

- the workflow they support
- the behavior they must preserve
- what they must not change
- the authoritative source of truth after the infrastructure is added
- the stale-data, failure, or fallback policy when relevant

For cache slices, the intake must also state:

- what data is cached
- what data is explicitly not cached
- whether negative results are cached
- how stale results are avoided or bounded
- whether the cache is local or shared
- how cache miss falls back to the authoritative source

For logging and audit slices, the intake must also state:

- who uses the recorded data
- what sensitive data is forbidden
- retention or access-policy unknowns
- whether the record is an application log, audit record, or support reference

---

## Reference Core Intake Checklist

When a slice is proposed as part of the enterprise application core reference
direction, answer:

- Does this capability already exist in Orca?
- Is it a user-facing workflow, an operator workflow, or a supporting pattern?
- Is it required before a frontend shell can consume the backend safely?
- Does it introduce business behavior or only preserve existing behavior?
- Does it require new actors?
- Does it require new role or permission semantics?
- Does it store or expose sensitive data?
- Does it need an explicit non-goal section to prevent scope creep?

---

## Hard Stop Rules

Stop before SDD when:

- no workflow gap can be named
- the proposed slice contradicts authoritative repo docs
- predecessor slices do not exist
- the prompt says a slice exists but the repo does not show it
- the work is only a technical demonstration with no workflow traceability
- sensitive data would be logged, cached, exposed, or returned without an
  explicit security rule
- CRM, project-management, or other product-domain behavior is being imported
  into Orca without an explicit Orca product decision

---

## Required Output Before SDD

Before writing a spec, produce a short intake note containing:

```text
Slice candidate:
Workflow:
Workflow gap:
Primary actor:
Successful outcome:
Failure flows:
Existing supported slices:
Planned predecessor slices:
Unknowns:
Non-goals:
Decision: enter SDD / stop
```

Only candidates with `Decision: enter SDD` should become
`docs/specs/<bounded-context>/<NN>-<name>.md` or an approved
`docs/specs/<support-scope>/<NN>-<name>.md`.
