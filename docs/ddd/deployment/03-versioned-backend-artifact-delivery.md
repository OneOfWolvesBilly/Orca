# DDD - Deployment 03 - Versioned Backend Artifact Delivery

Status: Baseline GitHub Packages implementation verified; public audit delivery repair DDD complete / TDD pending; publication proof pending.

This note is derived from the approved contract in
`docs/specs/deployment/03-versioned-backend-artifact-delivery.md`.
It must not introduce behavior beyond that spec.

## Public Audit Delivery Derivation (2026-09-29)

This repair derives only the Public Audit Delivery Repair in
[deployment-03](../../specs/deployment/03-versioned-backend-artifact-delivery.md)
and consumes the Exact Supported Audit API in
[reference-core-03](../../specs/reference-core/03-reusable-audit-recording-boundary.md).
DDD was explicitly authorized after the single-outcome intake and SDD closeout.
The user subsequently authorized the SDD/DDD documentation commit followed by
TDD. Implementation, integration and release mutations remain unauthorized.

### Ownership and Component Placement

| Component | Owner and placement | Spec obligation |
| --- | --- | --- |
| seven audit types | existing backend `referencecore.application`; no rename or extraction | reference-core-03 owns structure, signatures, failures and semantic responsibility |
| consumer typed fixture input and mapper | standalone `deploy/backend-artifact/consumer-fixture`; no backend production dependency on fixture | A1, A5 / D-AUDIT-3 |
| consumer recorder implementations and wiring | fixture-owned test configuration and test assets | A1, A4; explicit recorder, no Orca default |
| audit contract tests | backend plain support tests plus standalone artifact consumer tests | A1-A5; local tests do not replace artifact consumption |
| exact public dependency guard | deployment verification tooling, exercised by its contract tests | D-AUDIT-1; source access limited to three auth and seven audit types |
| artifact/API inventory and compatibility metadata | existing release profile and verification tooling | D-AUDIT-2, D-AUDIT-4 |
| existing auth/migration proof | retained standalone consumer suite and release runtime matrix | D-AUDIT-5; no audit adoption or migration change |

The consumer fixture remains outside the root Maven reactor with its own
Spring Boot parent and exact Orca dependency. It does not depend on backend
test classes or copy `RecordingAuditRecorder` from Orca. Fixture-owned recorder
implementations are newly written test doubles against the approved interface,
not production storage adapters.

### Audit Fixture Composition

Use a separate audit test configuration/path inside the existing standalone
fixture so audit tests do not inherit the auth test class's database seeding,
credential hashing or HTTP request setup. Spring hosts explicit consumer beans
for the typed mapper/caller and selected recorder; no new Orca annotation or
auto-configuration is needed. Tests invoke this consumer path directly.
Use an audit-only Boot test application with explicit consumer configuration,
without embedded-auth enablement, and restrict auto-configuration so datasource
and Flyway startup are not prerequisites of that audit test context. Do not
change the existing embedded-auth application or its runtime configuration.
The existing embedded-auth application and migration tests remain regression
proof and may still need their existing datasource. Their dependencies are not
requirements of the audit API or the audit test path.

The typed fixture input carries the SDD's synthetic actor id, `Instant`, and
confidential test detail. Its mapper builds exactly:

```text
eventType = fixture.audit-recording
actorId = input synthetic actor
occurredAt = input instant
outcome = completed
tenantId/resourceType/resourceId = absent
metadata = { source: standalone-fixture }
```

The confidential detail is never forwarded. A full-envelope test separately
exercises optional identifiers and metadata. Avoid logging the input or record
as a way to assert safety; compare values in memory. Synthetic secret sentinels
are test data, not package credentials or real sessions.

The consumer caller constructs first and calls its supplied recorder second.
A recording double captures the exact immutable record; a second independent
double proves replacement; a throwing double exposes a sentinel exception and
counts calls before throwing. Invalid construction results in zero calls. The
fixture's explicit required-recorder wiring has a negative test with the bean
omitted; startup/configuration failure belongs to this fixture, not a new
Orca-wide startup rule. No no-op fallback is installed.

### Dependency Guard Design

Represent the supported surface as an exact qualified-type allowlist: the
three existing auth types plus the seven reference-core audit types. Audit
membership is derived from the owner spec, not from scanning all `public`
classes. Keep a drift check tying the guard, artifact inventory and compatibility
API record to that same expected set.

Inspect both consumer main and test Java source. A Java syntax/symbol-aware
check should resolve referenced Orca type owners rather than rely only on a
line-start import regular expression. This covers named imports, static member
imports and fully qualified references, while comments and string literals do
not accidentally become imports. Wildcard Orca imports are rejected per SDD.
Positive cases include named allowed imports, allowed static factories and
allowed fully qualified references. Negative fixtures cover forbidden sibling
application types, auth/organization internals, wildcard imports and static/FQN
forms. Deliberately invalid snippets belong to guard test data, outside the
accepted consumer source tree.

The runner gates the copied standalone consumer source before claiming a
successful build. The test harness also compiles positive supported signatures
against the resolved artifact and requires invalid scalar/time/metadata
signatures to fail. Distinguish an expected snippet diagnostic from an absent
compiler, unresolved Orca artifact or broken harness: tooling/setup failure
cannot count as a passing negative test. Compiler diagnostics are evidence,
not a new public error API.

Compilation alone is insufficient because forbidden classes can be physically
present. Conversely, a passing source guard does not prove binary/API
compatibility. Both checks are required before the consumer result is accepted.
Reflection/internal-name access is unsupported by the spec and is excluded
from the fixture by source/provenance review; do not claim a source guard is a
security sandbox for arbitrary consumer programs.

### Artifact and Evidence Design

Extend the existing component inventory to include all seven exact audit class
entries, their source/Javadoc representation and the exact resolved consumer
POM. Include an `audit.public-api` entry in the generated compatibility record,
listing the seven qualified type names; compare it with the expected supported
set in verification. This is delivery metadata, not a new runtime input or
audit behavior. Preserve the existing embedded-auth metadata and compatibility
policy. Other packaged/Javadoc types remain unsupported for direct integration.

Run the same audited consumer sources and A1-A5 matrix at staging and formal
retrieval. Copy source/resources/POM only into a fresh workspace; exclude old
`target` output, reports and cached class files. Use a fresh Maven repository
and an explicit exact coordinate. The copied project's compile/runtime paths
must not point into the Orca checkout. Do not allow an old passing report,
repository-local install or copied JAR to satisfy the artifact proof.

| Evidence | Assembly and checks | Claim boundary |
| --- | --- | --- |
| repository-local | expanded plain model/port tests, guard tests and reactor regressions identified by source state | local behavior only |
| staged artifact | committed source candidate, ordinary JAR/POM/sources/Javadoc, checksums, API and compatibility inventory, fresh external fixture/cache, A1-A5 and auth/migration suites | pre-publication consumption only |
| published version | explicitly authorized immutable version, canonical authenticated GitHub resolution with external settings, matching integrity/provenance and the same fixture matrix | final artifact delivery proof |

Record exact Java/Spring Boot/database versions with the result. H2 consumer
success and the old `TEST-MATRIX-EVIDENCE` value cannot prove MariaDB support.
A version-only compatibility assertion is insufficient without the matching
runtime result. No repository token or settings contents enter reports.

### Failure and Verification Placement

| Class | Detection placement | Evidence/result |
| --- | --- | --- |
| absent | missing artifact/API member/runtime evidence, required audit fields or fixture recorder | D-AUDIT-2/4 and A1/A2; fail the current proof before claiming availability |
| null | audit constructors/factories and existing runtime configuration tests | A2 plus original deployment matrix; no null-to-empty coercion except specified omission |
| blank | audit identifiers/metadata and version/configuration validators | A2 and original deployment matrix |
| malformed | invalid Java calls, raw metadata, unreadable archive/POM/integrity | A3 and D-AUDIT-2/4; reject before accepted consumer result |
| duplicate | metadata keys, artifact/version/provider ambiguity | A2 and D-AUDIT-4 plus existing provider checks; no audit deduplication policy |
| unsupported | exact source dependency allowlist, signature graph, declared runtime matrix | D-AUDIT-1/5 and A3; no package-wide support claim |
| untyped | erased collections and existing shell/XML/environment boundaries | A3 runtime cases and release validation tests |
| stale | source/GAV/checksum comparison and clean consumer workspace/cache | D-AUDIT-4; preserve caller time without adding event freshness rules |
| unauthorized | forbidden type dependencies, unsafe mapper fields, denied package access | D-AUDIT-1, A5, redacted external denial proof; no new role policy |
| unexpected | recorder exception, compiler/harness/linkage/resolution/startup failure | A4 / D-AUDIT-3/4; observable failure, failed release stage, no global fallback |

| Spec proof | Derived verification placement |
| --- | --- |
| D-AUDIT-1 | release contract suite plus source/symbol guard positive and negative fixtures; standalone compilation uses the resolved artifact |
| D-AUDIT-2 | release component inventory and compatibility metadata assertions, including missing-class and inconsistent-API negatives |
| D-AUDIT-3 | separate standalone audit test path implementing reference-core-03 A1-A5; includes recorder replacement/failure and exact safe mapper output |
| D-AUDIT-4 | verifier orchestration tests and actual staged/published retrieval; controlled missing/corrupt/substituted artifacts and source/version/checksum mismatches |
| D-AUDIT-5 | existing reactor/standalone auth/error/migration suites and verified Java/MariaDB matrix |

Remote denial uses reproducible redacted evidence under the existing spec.
The external outage exception remains unchanged: it cannot deterministically
be injected into GitHub, and release availability stays unclaimed until a
successful retry/retrieval. Audit behavior has no manual-only waiver.

### DDD Closeout and Sequencing

The design covers the seven-type owner contract, one independent consumer
outcome, A1-A5 and D-AUDIT-1 through D-AUDIT-5, all ten failure classes and
three evidence levels. It supersedes the former auth-only consumer dependency
list in this note. No product event catalog, auth/organization emission,
production recorder, audit database/query/retention/export/outbox, logging,
credential migration, React publication or CogniRig integration is introduced.

Selected DELIVERY/ARCH/DOC portions are aligned; unrelated portions and all
other approved deferred items remain active. Both specs, README, product maps,
slice map and the canonical ignored handoff record DDD complete / TDD pending.
This is design completion only. Next authorized-layer decision is TDD; begin
with plain support-boundary tests, then fixture/build contract tests before
implementation. No new domain model requires a separate domain slice.

Logging/correlation still requires its own intake and sequencing decision
before the final V1 candidate. Publication/retrieval still requires separate
authorization and exact release evidence. No commit, merge, push or tag is
performed by completing this DDD.

## Purpose

Derive the implementation boundaries for producing, validating, publishing,
and independently consuming one versioned Orca backend artifact without
turning deployment into a business bounded context or changing existing auth
behavior.

The single actor-visible outcome remains:

- an application developer declares one exact Orca release from GitHub Packages
  and uses the approved embedded-auth and audit APIs without an Orca checkout, copied
  source, copied JAR, or internal-package import.

## Scope Classification

`deployment` remains an approved delivery/runtime support scope.

This slice introduces no aggregate, entity, domain value object, repository,
domain service, or application use case. Its model is a release-delivery model
used by build configuration, release scripts, verification fixtures, and
documentation.

Auth, reference-core, organization, and other contributing Orca scopes retain
ownership of the implementation packaged in the artifact. Packaging does not
transfer behavioral authority to deployment.

## Support-scope Language

- **Coordinate Family**
  The stable `groupId:artifactId` pair
  `io.github.oneofwolvesbilly:orca`.

- **Release Version**
  One exact, unused, non-SNAPSHOT semantic version selected for publication.

- **Release Candidate**
  The binary JAR, flattened consumer POM, sources JAR, Javadoc/documentation
  JAR, compatibility record, and provenance produced from one source commit
  before publication.

- **GitHub Package**
  The versioned Maven component associated with the public Orca GitHub
  repository and stored by GitHub Packages.

- **User-managed Deployment**
  An explicit Maven deploy invocation that activates the GitHub Packages
  profile only after separate publication authorization.

- **Compatibility Record**
  Version-specific resolved metadata naming the Java, Spring Boot, MariaDB,
  and migration combinations actually verified for the release.

- **Independent Consumer Fixture**
  A standalone Spring Boot Maven project that is not a reactor module and
  resolves Orca only through a supplied Maven repository boundary.

- **Clean Repository Proof**
  A consumer build using a newly created isolated Maven local repository with
  no preinstalled Orca artifact.

- **Package Retrieval Proof**
  The post-publication clean repository proof whose Orca source is GitHub
  Packages and whose read credential remains outside consumer source.

- **Release Evidence**
  Redacted machine-readable and human-readable output that binds source commit,
  exact coordinate, compatibility record, validation, publication, and
  consumer results.

These terms belong to delivery support. They must not appear as auth or product
domain concepts.

## Release Consistency Boundary

One Release Candidate is the consistency boundary for delivery verification.
It is identified by:

```text
sourceCommit
groupId
artifactId
releaseVersion
artifactChecksums
compatibilityRecord
```

All files in the candidate must agree on the same GAV and originate from the
same source commit and build invocation. Mixing files, metadata, or
compatibility evidence from different versions or commits invalidates the
candidate.

The release states are:

```text
Development Snapshot
  -> Candidate Built
  -> Candidate Locally Verified
  -> Publication Authorized
  -> GitHub Package Uploaded
  -> Published
  -> Package Retrieval Verified
```

Rules:

- A failure stops progression and records the failed state.
- Only explicit user authorization may upload the candidate to GitHub Packages.
- `Published` is immutable; correction creates a new Release Version.
- The actor-visible outcome is complete only at `Package Retrieval Verified`.
- A local bundle or staging proof must never be presented as public delivery.

## Component Placement

Recommended implementation placement:

```text
pom.xml
orca_backend/pom.xml
minimal_consumer_fixture/pom.xml
deploy/backend-artifact/
  README.md
  bin/
    build-release-candidate.sh
    verify-release-candidate.sh
    verify-github-package.sh
  consumer-fixture/
    pom.xml
    src/main/...
    src/test/...
  test/
    verify-release-candidate.test.sh
```

The exact filenames are implementation guidance, but the ownership boundaries
are normative:

- Maven POMs own Maven coordinates, metadata, dependency scopes, and build
  plugins.
- `deploy/backend-artifact` owns orchestration, release gates, fixture
  isolation, and redacted evidence.
- The independent fixture is deliberately omitted from the root `<modules>`
  list and has no Orca parent POM.
- Auth and reference-core tests continue to own behavioral regression.
- Flyway migration files remain in their current backend resource location.

No release script may modify a consumer product repository.

## Maven Version Design

### CI-friendly project version

The reactor, backend artifact, and repository-local Minimal Consumer Fixture
should use Maven's supported CI-friendly form:

```text
${revision}${changelist}
```

Default development values preserve snapshot behavior:

```text
revision=0.0.1
changelist=-SNAPSHOT
```

An authorized release invocation supplies:

```text
-Drevision=<unused-major.minor.patch>
-Dchangelist=
```

The version validator runs before packaging and rejects:

- absent, null-equivalent, or blank version input;
- whitespace or characters outside the selected semantic-version grammar;
- `SNAPSHOT`, a Maven range, `LATEST`, or `RELEASE`;
- a version different from the generated POM or artifact filenames; and
- a coordinate already visible in GitHub Packages.

Because Orca currently uses Maven 3.9.12, the backend publishing module must
generate a flattened consumer POM for install, staging, and deploy. The
published POM must contain the resolved release version, not
`${revision}${changelist}`.

The root aggregator and Minimal Consumer Fixture are never published. Their
version alignment exists only to preserve the current reactor regression
build.

## Maven Publication Design

### Backend release profile

Only `orca_backend` produces the public component. A release-only Maven profile
owns:

- flattening the consumer POM;
- attaching sources;
- attaching Javadoc or an allowed documentation JAR;
- defining the standard Maven deploy boundary; and
- activating the GitHub Packages publication profile only when the selected
  command permits it.

All build and publishing plugin versions must be pinned. Publication uses the
standard Maven deploy plugin and repository-scoped GitHub Packages endpoint.

The profile must configure:

```text
serverId=github
repositoryUrl=https://maven.pkg.github.com/OneOfWolvesBilly/Orca
publicationProfile=backend-artifact-github-packages
```

The publication profile must not activate by default. No committed profile,
script, CI job, or default property may turn validation success into automatic
publication. Its enforcer requires the release profile and rejects a snapshot
project version before deploy can reach GitHub Packages.

### GitHub package metadata

The flattened consumer POM owns:

- project name and description;
- project URL;
- license identity and URL;
- developer identity;
- source-control connection and URL;
- `jar` packaging;
- exact GAV; and
- correct compile/runtime/optional dependency scopes.

The selected public license is Apache License 2.0. The root license text and
the flattened POM metadata are one release component contract and must remain
aligned.

The release verifier must inspect the flattened POM rather than assuming the
source POM is the consumer result.

### Secret boundary

GitHub credentials live under server id `github` in an external Maven
`settings.xml` or GitHub Actions secret boundary. Publication and read tokens
must not be committed to the repository.

Scripts may validate only presence and successful use. They must not echo,
serialize, copy, archive, checksum, or include secrets in release evidence.

Token creation, package upload, permission changes, and publication are external
mutations. Each requires the relevant user authorization; DDD or implementation
authorization alone is insufficient.

## Artifact Assembly Design

The public dependency is the ordinary backend classes JAR. It must remain
usable on a consumer compile/runtime classpath and must not be replaced by a
Spring Boot executable fat JAR.

The component set includes:

```text
orca-<version>.jar
orca-<version>.pom
orca-<version>-sources.jar
orca-<version>-javadoc.jar
repository-generated integrity metadata
```

The release verifier inspects the binary JAR and rejects:

- missing approved auth API classes or any of the seven audit classes;
- missing Spring auto-configuration metadata;
- missing or reordered `db/migration` resources;
- fixture classes, test classes, local environment files, credentials, or
  frontend/npm content;
- duplicate entries or unreadable archive content; and
- a manifest or compatibility version different from the GAV.

Internal implementation classes may be present because the public entry point
composes them at runtime. Presence does not grant source-level consumer access.

## Compatibility Record Design

Each candidate generates one resolved, version-specific record at:

```text
META-INF/orca/backend-artifact-compatibility.properties
```

The record is packaged in the binary JAR and copied into redacted release
evidence. It contains at least:

```text
orca.version
orca.source-commit
java.release
java.verified-runtimes
spring-boot.supported
database.product
database.verified-versions
migration.current
migration.supported-starts
embedded-auth.public-api
audit.public-api
unsupported-combinations
```

The record is generated from explicit release inputs and verification results;
it is not a hand-edited claim. Packaging fails when a required value is absent,
blank, unresolved, duplicated, or inconsistent with the candidate.

`orca.source-commit` is provenance, not authority to publish an uncommitted
tree. A release build must reject a dirty or mismatched source state before
external upload.

The initial candidate may declare only combinations actually exercised. Java
21, Spring Boot 4.0.1, and MariaDB 11 are candidate inputs from the current
repository, but they do not become release claims until their verification
jobs pass and the resolved MariaDB version is recorded.

## Independent Consumer Design

The checked-in independent fixture is a test asset, not a reactor module or
bounded context.

It has:

- its own POM with no Orca parent;
- an exact Orca version supplied by the verifier;
- one repository URL supplied by the verifier;
- its own Spring Boot application and datasource values;
- direct source dependencies limited to `EnableOrcaEmbeddedAuth`,
  `OrcaProtectedCommand`, `AuthenticatedActor`, and the seven exact audit types
  approved by reference-core-03;
- one product-neutral protected command; and
- integration tests for login, actor resolution, logout, and post-logout
  rejection; and
- separate public audit consumption tests for A1-A5 without auth test seeding.

The verification runner copies only fixture source/resources/POM, excluding
generated output and cached reports, into a temporary directory and
creates a second temporary directory for `maven.repo.local`. It must validate
both directories before cleanup and must never use a workspace root, `$HOME`,
or unresolved broad path as a cleanup target.

Pre-publication mode points the fixture at the candidate's generated Maven
repository layout. Post-publication mode points it at GitHub Packages and uses
external Maven settings for authentication. Neither
mode may run `mvn install` on Orca or fall back to the developer's ordinary
local Maven repository.

The runner may use Orca's Maven wrapper as test tooling, but the copied fixture
POM, source, dependency resolution, and runtime classpath must not reference the
Orca checkout.

## Migration Verification Design

Migration ownership is verified separately from business behavior.

The release harness provides a disposable supported MariaDB runtime and proves:

1. the independent consumer starts against an empty schema;
2. the artifact-packaged migrations create the expected current schema;
3. a second startup against the already-current schema succeeds without schema
   drift; and
4. consumer source contains no copied migration file or schema mutation.

Later releases add a fixture snapshot for each declared supported migration
starting state. Unsupported downgrade or skipped-version paths remain explicit
compatibility exceptions rather than inferred support.

MariaDB container identity, exact server version, connection values, and
credentials are runtime evidence. Credentials remain in temporary or ignored
runtime state and never enter artifact or test output.

## Rule Placement

### Deployment support rules

Deployment owns:

- release-version input validation;
- component assembly and metadata validation;
- candidate state progression;
- GitHub Packages profile activation and user-managed publication gates;
- artifact-content and secret-leak inspection;
- compatibility evidence assembly;
- isolated consumer orchestration;
- clean-repository and public-retrieval proof; and
- safe failure reporting.

### Auth rules

Auth continues to own:

- login and logout behavior;
- `ORCA_SESSION` creation, resolution, expiration, and revocation;
- embedded enablement and protected-command semantics;
- authenticated actor establishment; and
- indistinguishable authentication rejection.

Deployment invokes auth contract tests and independent consumer flows. It does
not restate or implement those rules.

### Reference-core rules

Reference-core remains authoritative for stable API errors and the public
audit contract. Delivery tests consume the exact seven audit types and preserve
its construction, recorder failure and sensitive-data responsibility rules;
they must not introduce a release-specific audit policy or error envelope.

### Consumer rules

The consuming application owns datasource and environment configuration,
secret delivery, process lifecycle, product routes, and product behavior. The
fixture demonstrates those responsibilities without becoming their owner.

### Flyway rules

Contributing Orca scopes own migration content. Flyway owns ordered schema
application. Deployment owns only the proof that the published artifact
contains and executes those migrations through the public runtime boundary.

## Dependency Direction

```text
Release orchestrator
  -> Maven release profile
  -> backend artifact assembly
  -> GitHub Packages Maven deploy boundary

Independent Consumer Fixture
  -> Maven repository boundary
  -> io.github.oneofwolvesbilly:orca:<exact-version>
  -> exact approved auth and audit public types
  -> packaged Orca implementation

Packaged Orca implementation
  -> existing auth/reference-core/application ports
  -> existing domain and infrastructure

Orca domain
  -> no deployment, release, GitHub Packages, fixture, or consumer dependency
```

No production source dependency may point from Orca into
`deploy/backend-artifact` or the independent fixture.

## Test Layer Placement

No new domain model or use-case rule is introduced. The audit amendment extends
plain support-model/port contract tests under reference-core and standalone
consumer/build tests under deployment, as mapped above.

Maven/build contract tests validate:

- exact resolved release coordinates;
- flattened consumer POM metadata and dependency scopes;
- required component classifiers and repository metadata;
- ordinary classes-JAR packaging;
- compatibility record completeness and consistency;
- the GitHub Packages profile is inactive by default; and
- snapshot/default development builds remain available without activating
  release publication.

Shell contract tests use controlled Maven, archive, checksum, Docker, and
GitHub repository adapters where network or secret-bearing behavior must not run. Test
names should remain behavior oriented, including:

- `builds one immutable backend release candidate`
- `rejects an invalid release version`
- `keeps GitHub Packages publication user managed`
- `rejects incomplete GitHub package metadata`
- `rejects artifact content outside the backend boundary`
- `verifies an isolated consumer from staged artifacts`
- `verifies an isolated consumer from GitHub Packages`
- `rejects internal Orca imports in the consumer`
- `rejects conflicting Orca dependency versions`
- `rejects duplicate embedded providers`
- `migrates an empty supported MariaDB schema`
- `starts against an already-current schema`
- `redacts release and runtime secrets from every result`

Existing Maven tests remain authoritative for auth and reference-core
regression. The independent fixture provides cross-project HTTP evidence but
must not replace those suites.

## Failure Placement

| Failure class | Detection placement | Result placement |
| --- | --- | --- |
| absent | version validator, component inventory, fixture configuration, dependency resolution | fail current candidate state |
| null | public/runtime configuration test adapter | fail build or startup before protected behavior |
| blank | version, metadata, compatibility, and runtime validators | fail without coercion |
| malformed | POM, archive, checksum, migration, and datasource validators | fail before validation/public claim |
| duplicate | Maven convergence rule, archive scan, Spring startup validation, GitHub coordinate check | reject ambiguity or reuse |
| unsupported | compatibility matrix and runtime matrix | record unsupported; never claim passing support |
| untyped | shell/environment/XML/YAML boundary validators | reject before invoking secret-bearing or runtime action |
| stale | clean local repository, checksum comparison, source-commit check, migration matrix | fail provenance or compatibility proof |
| unauthorized | external settings preflight and GitHub response adapter | stop with redacted actionable failure |
| unexpected | command wrapper and candidate state controller | stop at named stage; preserve non-secret diagnostics |

External outage and real GitHub authorization behavior use reproducible,
redacted manual evidence. Controlled adapters test Orca's reaction without
requiring publication during ordinary tests.

## Release Command Separation

Implementation must expose separate commands or explicit modes for:

1. build candidate without network publication;
2. verify candidate through local staging;
3. publish to GitHub Packages after external-mutation authorization; and
4. verify authenticated GitHub Packages retrieval with a clean local cache.

One command must not silently cross these boundaries. In particular, ordinary
`test`, `package`, `verify`, or repository-local `deploy` execution must never
publish publicly.

## Design Decisions

### Decision: Preserve the existing coordinate family

The current backend already builds as `io.github.oneofwolvesbilly:orca`. Keeping
that family avoids an unnecessary module extraction or artifact rename. Public
API remains constrained by package contract rather than by pretending every
packaged class is supported.

### Decision: Use GitHub Packages

GitHub Packages keeps source and artifact delivery on the user-selected GitHub
boundary while preserving normal Maven coordinates and dependency resolution.
GitHub requires a token for Maven package installation, so each consuming
product owns its read credential outside source control.

### Decision: Use CI-friendly versions with a flattened consumer POM

This preserves the normal snapshot development build while allowing an exact
release version to be injected without hand-editing source files. Maven 3
requires the flattened POM so consumers receive resolved coordinates.

### Decision: Keep publication user managed

Candidate construction and local validation are reversible review steps. The
GitHub Packages profile is inactive by default, preserving a distinct human
authorization boundary before Maven deploy performs an external mutation.

### Decision: Keep the independent fixture outside the reactor

A reactor fixture can receive artifact and version state from the same build.
The standalone fixture and clean local repository prove that the distribution
boundary, POM metadata, transitive dependencies, and artifact contents work
without that hidden coupling.

### Decision: Publish the ordinary classes JAR

An executable fat JAR is an application deployment form, not a normal library
dependency. The embedded-auth consumer needs the ordinary classes JAR and its
dependency metadata.

### Decision: Package the compatibility record with the artifact

A resolved record inside the immutable artifact binds compatibility claims to
the exact released bytes. Release evidence copies the same record so the proof
can be reviewed without inferring versions from the current source tree.

## Risk Notes

- Publishing the current snapshot coordinate would make builds non-repeatable
  and fail the release contract.
- Reusing a public GAV would violate repository immutability.
- Publishing an unresolved CI-friendly POM would make the artifact unusable or
  misleading to consumers.
- Activating the GitHub Packages profile in ordinary builds would collapse the
  required human gate.
- Repackaging as a fat JAR could hide or break Maven dependency behavior.
- Testing against the ordinary developer Maven cache could accidentally consume
  a locally installed Orca artifact.
- Leaving the standalone fixture in the reactor would repeat the existing proof
  gap.
- Treating every packaged internal class as public API would silently expand
  compatibility obligations and absorb `ORCA-ARCH-01`.
- Hand-writing compatibility claims without running the matrix could advertise
  unsupported Java, Spring Boot, MariaDB, or migration combinations.
- Logging Maven settings, token values, datasource credentials,
  or session cookies could leak secrets.
- Calling local staging “published” would report the actor
  outcome before it exists.
- Combining frontend/npm delivery would cross the one-slice boundary.

## Non-goals Confirmed

- No new bounded context, aggregate, entity, or use case.
- No auth/session or credential behavior change.
- No schema-definition or migration ownership change.
- No internal-package refactor under `ORCA-ARCH-01`.
- No CogniRig-specific code, package, configuration, or UI.
- No React/npm artifact.
- No separately deployed Orca API.
- No production cloud topology.
- No automatic GitHub Packages publication.
- No token, package upload, publication, tag, commit, merge, or
  push authorization.

## Baseline DDD Closeout Evidence

DDD closeout on 2026-09-13 confirmed:

- every component maps to a deployment-03 acceptance criterion;
- no rule moved out of its authoritative owner;
- all ten failure classes have detection and evidence placement;
- the independent fixture has no reactor or internal-package coupling;
- release and publication commands remain separately authorized;
- the compatibility record cannot claim an unverified combination;
- `ORCA-DELIVERY-01`, `ORCA-ARCH-01`, `ORCA-SECURITY-01`, and `ORCA-DOC-01`
  remain reconciled without closing unrelated work; and
- workflow, capability, slice, status, verification, and private handoff
  records are identified for the documentation commit and later implementation
  closeout.

The distribution correction on 2026-09-17 replaced the over-specific Maven
Central decision with GitHub Packages while preserving the Maven coordinate,
artifact contents, compatibility evidence, independent consumer, and public API
boundaries. The corrected TDD and implementation are verified locally;
publication remains a separately authorized external mutation.

Verification completed on 2026-09-20 confirms that Maven resolves the inactive-
by-default GitHub Packages profile, an isolated staged consumer resolves the
ordinary artifact, external settings remain outside source, and all existing
reactor regressions remain green.
