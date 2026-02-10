# Engineering Constraints

These constraints are **authoritative**.
Violating them is considered a design defect.

They exist to keep Orca verifiable:
behavior is specified first, domain rules are testable in isolation,
and infrastructure is an adapter rather than the source of truth.

---

## 1. Authority & Workflow

- `docs/specs/*` defines behavior and is the source of truth.
- Derived documents must not introduce new behavior.
- Implementation must follow the workflow:

  1) SDD: spec first  
  2) DDD: derive model boundaries and rule placement  
  3) TDD: domain tests before domain code  
  4) Application layer  
  5) Infrastructure last

The workflow must be followed strictly. 
Exceptions are allowed only to fix defects caused by not following the workflow.

---

## 2. Domain Isolation (Non-negotiable)

Domain code must not depend on:

- Spring / Spring Boot
- JPA / ORM annotations
- database schemas or migrations
- security frameworks
- network / HTTP concerns

Domain logic must be testable with plain unit tests.

---

## 3. Rule Placement

- **Domain invariants**:
  - enforceable inside an aggregate without external state
  - verified by domain tests (pure Java)

- **Application rules**:
  - require repository checks or coordination across aggregates
  - verified by application tests using fakes/in-memory implementations

- Controllers must not contain domain logic.
- Infrastructure must not define business behavior.

---

## 4. API Semantics

- Any operation that depends on user context **must not use GET**.
- GET is allowed only for:
  - public metadata
  - static or non-authenticated information
- Health and info endpoints must be accessible without authentication.

---

## 5. Persistence & Schema Ownership

- Database schema is owned by Flyway migrations.
- Application code must not mutate schema implicitly.
- No shared database access across bounded contexts.

---

## 6. Consistency & Transactions

- Aggregates enforce their own invariants.
- Cross-aggregate consistency belongs in the application layer.
- If a rule cannot be enforced without external state, it is not a domain invariant.

---

## 7. Frontend Discipline

- The frontend must not re-implement business rules.
- UI behavior must be driven by backend responses and capabilities.
- Frontend terminology must mirror backend domain terminology.

---

## 8. Testing Requirements

- Domain tests come first.
- No Spring context in domain tests.
- Integration tests come after domain correctness is proven.
