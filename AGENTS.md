# Orca agent instructions

## Git delivery workflow

The Git history owned by the user must remain linear. Codex worktrees and
their branches are temporary implementation environments only; they are not
part of the repository's intended branch structure.

- The user's local `main` is the delivery branch.
- `origin/main` is only the remote synchronization reference.
- Start every implementation task from the current local `main`.
- When using a worktree, create a temporary branch from local `main` and do all
  implementation and commits in that temporary worktree branch.
- Before delivery, rebase the temporary branch onto the current local `main`
  and rerun the required tests.
- Deliver only by fast-forwarding local `main`. The integration operation must
  be equivalent to `git merge --ff-only <temporary-branch>`.
- After successful delivery, delete the temporary branch. Do not manually
  remove any Codex App-managed worktree; its cleanup must follow the retention
  period configured by the user.
- Leave local `main` ready for user review.
- Never push unless the user explicitly requests a push.

When the user says `merge to main`, `land on main`, or `deliver to main`, this
does not authorize a merge commit. It means: rebase the temporary branch onto
the current local `main`, test, fast-forward local `main`, delete the temporary
branch, and leave the app-managed worktree under Codex App retention control.

Never:

- use `git merge --no-ff` for worktree delivery;
- create a merge commit when delivering worktree changes;
- use a normal merge when fast-forward is impossible;
- create or preserve permanent Codex feature-branch topology;
- push a temporary worktree branch;
- merge `origin/main` into the temporary branch;
- replace the required rebase with a merge;
- rebase, reset, or otherwise rewrite the user's local `main`;
- force-push local `main`;
- push local `main` without explicit user authorization;
- modify this workflow without explicit user authorization;
- manually remove the current chat worktree or any Codex App-managed worktree;
- bypass or shorten the user's configured worktree retention period;
- delete, modify, rebase, or clean a worktree or branch that the user marked
  frozen, protected, retained, or comparison-only.

If fast-forward delivery is impossible, stop delivery, rebase the temporary
branch onto the current local `main`, resolve conflicts only when safe, rerun
the required tests, and retry the fast-forward. If delivery still cannot be
completed without changing history or workflow, stop and report the condition
to the user. Do not invent another Git strategy.

Do not rewrite existing merge commits that predate this rule merely to make
history linear. This policy governs future work.

Before reporting completion, verify all of the following:

- local `main` contains the delivered commits;
- no merge commit was introduced by the delivery;
- the temporary branch was deleted;
- the current chat and other app-managed worktrees remain available under the
  user's configured retention policy;
- no temporary branch was pushed;
- local `main` was not pushed unless explicitly requested.

## Read this first
Use ONLY the following files as authoritative sources, in this order:
1. README.md
2. docs/document-map.md
3. docs/constraints.md
4. docs/product/orca-sa-baseline.md
5. docs/product/workflow-map.md
6. docs/product/capability-map.md
7. docs/product/slice-intake-gate.md
8. docs/specs/*.md

Derived only:
- docs/ddd/*.md
- architecture / system-design style documents

Do not use archived, legacy, pre-reset, or reference-only documents as implementation authority.

## Mandatory read confirmation
Before analysis for any repo / file / version task, print exactly:

【讀取確認】
來源：
版本：
讀取狀態：成功 / 失敗 / 僅部分可見
是否為最新：是 / 否 / 無法判定
多版本存在：有 / 無

If read failed, content is partial, latest cannot be determined when required, or multiple versions are unresolved, stop and print:

【CI違反】
違反項目：
違反原因：
技術風險：
需補充資料：
修正方式：
是否允許繼續：否

## No guessing
Do not infer code, files, rules, or project state that you did not read.

If a hypothesis is unavoidable, print:

【假設】
內容：
驗證方式：

## Draft planning gate
- `docs/drafts/slice-planning-handoff.md` is the single local,
  non-authoritative planning handoff for unresolved repair and slice
  candidates.
- Do not create additional draft files unless the user explicitly requests a
  separate draft.
- Before creating or replacing the handoff because it appears missing, inspect
  every registered Git worktree for the same relative path, including untracked
  copies. If another copy exists, compare and reconcile it before editing or
  staging; do not assume an untracked file is absent from the project history.
- Before planning a new session or selecting the next behavior slice, read the
  `Active Items` section of the planning handoff when it exists.
- `Completed History` entries are one-line strikethrough tombstones only. Do
  not treat them as draft-candidate planning input or recreate their removed
  details. When a tombstone says `promoted`, inspect its target authoritative
  spec through the current-capability check and continue the recorded next
  required layer. Reopen the draft item only when the user explicitly asks or
  repository evidence proves the candidate disposition was wrong.
- Reading the draft does not authorize implementation and does not make draft
  content authoritative. Revalidate every candidate against README.md,
  docs/document-map.md, docs/constraints.md, docs/product/*, and docs/specs/*.
- Before selecting a draft candidate, first inspect whether the current
  capability or slice is incomplete, missing a required layer, failing its done
  definition, or has an authoritative next step that should be continued.
- Continue the current capability when required work remains. Select a draft
  candidate only when the current capability is complete and the candidate
  passes the slice intake gate.
- At the start of next-session planning, report which path applies and why:
  `continue current capability`, `select draft candidate`, or `stop`.
- From slice planning through SDD completion, perform two handoff checkpoints:
  1. Intake checkpoint: identify every active item that overlaps the proposed
     slice and ask the user to decide `include now`, `predecessor required`,
     `defer with reason`, or `stop` before writing the spec.
  2. SDD closeout checkpoint: before calling the spec complete, reconcile every
     selected item against the spec acceptance criteria, error cases,
     non-goals, verification requirements, and affected/superseded documents.
- An unrelated active item must not be silently added to the current slice.
  Leave it active with a short reason when the one-slice boundary excludes it.
- An active item is a problem record, not automatically one behavior slice.
  Before selection, prove that it has one actor-visible, client-visible, or
  operator-visible outcome. Split a problem cluster when independent decisions
  or outcomes would otherwise enter the same slice.
- Every active item must record the commit where it was observed, concrete
  repository evidence, candidate shape, and target spec or `TBD`. Record the
  intake disposition and target spec before writing SDD.
- If draft content conflicts with authoritative sources, follow the
  authoritative sources and correct the draft before relying on it again.
- Only after the SDD closeout checkpoint passes and the item is incorporated
  into a completed authoritative spec, remove its detailed active entry and add
  one one-line strikethrough tombstone under `Completed History` containing the
  item id, title, disposition, target spec, next required layer, and date.
  `promoted` closes only the draft candidate; it does not mean tests or
  implementation are complete. The authoritative spec and derived DDD note
  replace the removed details as the maintained project record.

## Dependency ownership gate
- Dependency ownership is a required slice-intake check, not a new bounded
  context, service, or implementation layer.
- For every mechanism the candidate needs, record:
  - the owning bounded context or approved support scope;
  - the authoritative predecessor that defines the behavior;
  - the public port, API, or contract the slice is allowed to consume;
  - whether the required predecessor is implemented and complete.
- If a required mechanism has no authoritative owner or public boundary, stop
  the candidate and select or discover the predecessor slice. A downstream
  adapter must not infer, copy, or redefine the missing rule from current
  implementation details.
- Non-goals prevent scope expansion but do not satisfy dependency ownership.

## Spec failure-set gate
- Derive negative tests from authoritative acceptance criteria and error cases,
  not only from values accepted by the implementation language or type system.
- For every external or public boundary, identify applicable absent, null,
  blank, malformed, duplicate, unsupported, untyped, stale, unauthorized, and
  unexpected inputs before SDD closeout.
- TypeScript types are compile-time guidance, not runtime validation for a
  public package boundary. When JavaScript or untyped consumers can reach the
  boundary, tests must include malformed runtime values authorized by the spec
  failure set.
- Each normative failure outcome must map to an automated test, a reproducible
  manual proof, or an explicit reason that it cannot be verified in the current
  slice.

## Orca workflow
- One session = one behavior slice.
- Do not jump layers.
- Default order:
  1. SDD
  2. DDD
  3. TDD (domain)
  4. Domain implementation
  5. Application layer
  6. Infrastructure last

Unless the user explicitly authorizes a later phase, stop at the allowed phase.

## Slice commit cadence
Follow the commit pattern established by organization-01 through organization-06 unless the user explicitly asks for a different cadence.

For each behavior slice, use at least two commits:
1. Commit the SDD spec and derived DDD note together.
2. Commit the TDD tests and implementation together.

Use smaller red / green / refactor commits only when the user explicitly requests that granularity or when the slice risk is high enough that splitting the work materially improves reviewability.

Do not collapse an entire behavior slice into one commit unless the user explicitly asks for a single commit.

## Pre-commit document-alignment gate

- After TDD and verification finish, and before staging or committing, inspect
  every repository-owned status, index, map, and lifecycle record affected by
  the slice. At minimum, check:
  - `docs/document-map.md`;
  - `docs/product/workflow-map.md`;
  - `docs/product/capability-map.md`;
  - the authoritative spec verification mapping and affected/superseded
    documents;
  - the derived DDD status;
  - the private planning handoff disposition.
- Before executing an authorized commit, explicitly report:
  - which mapping or lifecycle documents were modified;
  - which were checked and did not require modification, with the reason;
  - whether the spec and DDD use the repository's completed status marker;
  - whether the private handoff is aligned and remains ignored/untracked.
- Do not stage or commit while any required mapping, status marker,
  verification mapping, supersession record, or handoff disposition is stale.
- Do not merge or push a commit produced by the slice until this gate passes.
  Existing local-main and remote authorization rules still apply separately;
  passing this gate does not grant merge or push authority.

## Current repo direction
- Existing authoritative behavior lives in docs/specs/.
- Existing backend bounded context currently implemented is organization.
- Do not jump to issue/auth/frontend work unless the authoritative docs for that slice are already in place.

## Engineering constraints
- Domain code must not depend on Spring, JPA, DB schema, or security framework.
- Domain tests must be plain unit tests.
- Cross-aggregate consistency belongs in application layer.
- No business logic inside controllers.
- Frontend must not re-implement backend business rules.

## Output format for change suggestions
Every suggestion must include:
- 修改點
- 修改理由（風險 / 相容性 / 測試性 / 理解成本）
- 影響範圍
- 驗證方式

Use Traditional Chinese for explanations.
Use English for code and code comments.

## Done definition
A slice is not done unless:
- spec is aligned
- derived DDD note is aligned
- affected and superseded active documents are aligned
- selected active planning items have a recorded disposition
- each promoted tombstone points to its completed authoritative spec and the
  next required layer, or records that every required layer is complete
- every normative success and failure outcome has a test, reproducible manual
  proof, or explicit verification exception
- dependency ownership and allowed public boundaries are explicit
- relevant tests exist
- tests pass
- no forbidden layer jump happened
