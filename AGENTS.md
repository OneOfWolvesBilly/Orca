# Orca agent instructions

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
  non-authoritative planning handoff for unresolved slice candidates.
- Do not create additional draft files unless the user explicitly requests a
  separate draft.
- Before planning a new session or selecting the next behavior slice, read the
  planning handoff when it exists.
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
- If draft content conflicts with authoritative sources, follow the
  authoritative sources and correct the draft before relying on it again.
- Remove completed or promoted candidate details from the draft. The resulting
  spec and derived DDD note replace the draft as the maintained project record.

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
- relevant tests exist
- tests pass
- no forbidden layer jump happened
