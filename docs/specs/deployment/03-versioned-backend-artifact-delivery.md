# Deployment 03 - Versioned Backend Artifact Delivery

Status: Approved / GitHub Packages implementation verified / publication proof pending.

## Slice Intake

Planning path: `select draft candidate`.

Slice candidate:

- Formally publish one versioned, product-neutral Orca backend artifact so an
  independent application developer can consume the existing embedded-auth
  public boundary without copying Orca source or importing internal packages.

Workflow:

- Embedded Core Authentication Consumption.
- Build and release delivery support.

Workflow gap:

- `auth-12` proves same-process embedded-auth behavior through a
  repository-local Maven reactor dependency.
- The Minimal Consumer Fixture is built in the same reactor and resolves
  `io.github.oneofwolvesbilly:orca:0.0.1-SNAPSHOT` from that reactor.
- No authoritative release coordinate, public artifact repository,
  publication workflow, compatibility contract, or isolated cross-project
  retrieval proof exists.

Primary actor:

- Application developer integrating Orca into any supported consumer project.

Successful outcome:

- The developer declares one exact, non-SNAPSHOT Orca dependency from the Orca
  GitHub Packages Maven registry and uses the supported `auth-12` embedded-auth
  public API in an independently built Spring Boot application.
- The consumer neither copies Orca source nor imports an Orca internal package.

Failure flows:

- The coordinate, artifact, repository, version, or required consumer-owned
  runtime configuration is missing, blank, malformed, inaccessible, stale, or
  unsupported.
- Multiple Orca versions, duplicate providers, or incompatible dependency
  resolution makes the consumer classpath ambiguous.
- The artifact resolves but its metadata, contents, migration resources, or
  public entry points are incomplete.
- The public entry point cannot start with the supported runtime combination.
- Existing login, actor resolution, logout, or rejection behavior regresses.

Existing supported slices:

- `auth-08` password login with server-side session.
- `auth-09` protected HTTP session context.
- `auth-11` logout and session revocation.
- `auth-12` embedded auth and actor-context integration.
- `auth-13` client session expiry coordination.
- `reference-core-01` stable API error contract.
- `deployment-02` local MariaDB login runtime.

Planned predecessor slices:

- None. The behavior and public API consumed by this delivery slice are
  already authoritative and implemented.

Dependency owners:

- Auth owns embedded login, logout, protected-command, session, and actor
  behavior.
- Reference-core owns stable public error responses.
- Each Orca bounded context owns the code and resources it contributes to the
  backend module.
- The approved `deployment` support scope owns packaging, publication,
  distribution verification, and compatibility evidence. It does not own or
  redefine business behavior.
- The consuming application owns environment-specific datasource, credentials,
  runtime values, and its product behavior.
- Flyway migrations packaged by Orca remain the only schema-definition owner
  for Orca-owned tables.

Authoritative dependency predecessors:

- `docs/specs/auth/12-embedded-auth-actor-context-integration.md` defines the
  supported embedded-auth boundary.
- `docs/specs/auth/08-password-login-with-server-side-session.md`,
  `docs/specs/auth/09-protected-http-session-context.md`, and
  `docs/specs/auth/11-logout-session-revocation.md` define the existing HTTP and
  session behavior that publication must preserve.
- `docs/specs/reference-core/01-stable-api-error-contract.md` defines public
  rejection responses.
- `docs/specs/deployment/02-local-mariadb-login-runtime.md` proves the current
  MariaDB/Flyway runtime path but does not define public artifact delivery.

Allowed public boundaries:

- `io.github.oneofwolvesbilly.orca.auth.api.EnableOrcaEmbeddedAuth`
- `io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand`
- `io.github.oneofwolvesbilly.orca.auth.api.AuthenticatedActor`
- Existing auth HTTP endpoints and the `ORCA_SESSION` contract only as defined
  by their authoritative auth specs.

Predecessor completion evidence:

- `auth-12` is marked `Approved / Implemented` and has public API, web, and
  Minimal Consumer Fixture contract tests.
- The current backend suite and Minimal Consumer Fixture suite pass on the
  intake baseline.
- The deployment-02 shell contract suite passes all 24 checks on the intake
  baseline.

Unknowns resolved by this SDD:

- GitHub Packages is the approved Maven distribution source associated with
  the public Orca GitHub repository.
- The stable coordinate family remains `io.github.oneofwolvesbilly:orca`.
- A release version is an exact, immutable, non-SNAPSHOT semantic version.
- The first release number is not guessed by this SDD. It must be an unused
  version selected during the authorized release implementation and recorded
  in release evidence before any external publication.
- Compatibility is claimed only for combinations present in the published
  release compatibility matrix and proven by that release's verification.

Unknowns intentionally deferred:

- Frontend/npm package distribution.
- A non-Spring or separately deployed integration style.
- Broader cross-context package enforcement tracked by `ORCA-ARCH-01`.
- Production cloud topology and production identity-platform readiness.

Non-goals:

- CogniRig business behavior, package names, configuration, or UI behavior.
- A CogniRig-only integration shortcut or CogniRig ownership of the contract.
- React/npm package publication.
- A separately deployed Orca API.
- Changes to auth/session behavior or credential encoding.
- Changes to schema or migration ownership.
- Production cloud deployment.
- Combined backend and frontend distribution delivery.
- Treating the whole `ORCA-DELIVERY-01` problem cluster as one slice.

Overlapping active handoff items:

- `ORCA-DELIVERY-01`: include only its backend artifact outcome now.
- `ORCA-ARCH-01`: overlap exists at the supported-public-versus-internal import
  boundary; defer its broader package-enforcement outcome because the
  `auth-12` API already supplies the required public boundary.
- `ORCA-SECURITY-01`: not included; artifact delivery neither adds nor changes
  the log-redaction behavior tracked there.
- `ORCA-DOC-01`: not included; this spec records its own consumer contract but
  does not take ownership of repository-wide documentation discovery.

Handoff disposition:

- Keep `ORCA-DELIVERY-01` active through TDD, implementation, and final
  verification.
- Record this backend outcome as approved for deployment-03.
- Leave the frontend distribution remainder active and do not close or rewrite
  unrelated items.

Candidate shape: single behavior.

Target spec:

- New numbered spec:
  `docs/specs/deployment/03-versioned-backend-artifact-delivery.md`.
- Matching future DDD:
  `docs/ddd/deployment/03-versioned-backend-artifact-delivery.md`.
- Do not amend `auth-12`: auth owns the already-complete public behavior;
  deployment-03 owns formal delivery of that behavior.

Decision: enter SDD.

## Goal

Allow an application developer in an independent project to declare one exact,
released Orca backend dependency from GitHub Packages and start the supported
embedded-auth public boundary without copying Orca source or importing Orca
internal packages.

This is a delivery-support behavior slice. It makes existing Orca behavior
consumable; it does not add or reinterpret auth, reference-core, organization,
or consumer-product behavior.

## Distribution Contract

### Canonical Source

The Apache Maven registry in GitHub Packages for
`https://github.com/OneOfWolvesBilly/Orca` is the canonical distribution source
for this slice.

The consumer contract must not require:

- a repository-local Maven reactor;
- `mvn install` of an Orca checkout;
- a file, system-path, Git-submodule, or copied-JAR dependency;
- access to an Orca source checkout;
- a Sonatype account, Maven Central namespace, or Central publication service;
  or
- repository credentials committed to consumer source.

GitHub currently requires authentication for Maven package installation even
when the source repository and package are public. The consuming product owns
its GitHub identity and supplies a token through external Maven settings or its
CI secret store. The token is a delivery credential, not part of the Orca
artifact or public API.

A local or private staging repository may be used only for pre-publication
verification. Passing against staging does not satisfy the final independent
retrieval acceptance criterion.

### Stable Coordinate Family

The public backend coordinate family is:

```text
io.github.oneofwolvesbilly:orca:<release-version>
```

`<release-version>` must:

- be explicit in release evidence and consumer verification;
- be a non-blank `MAJOR.MINOR.PATCH` semantic version;
- not contain `SNAPSHOT`;
- identify one immutable set of artifact bytes and metadata; and
- never be reused or overwritten after public publication.

Consumers must use an exact released version in the acceptance fixture. Maven
version ranges, `LATEST`, `RELEASE`, snapshots, and locally substituted
versions do not prove this contract.

The release implementation must select the next unused version only after
checking the canonical repository and release history. This SDD deliberately
does not guess that external state.

### Published Component Set

For one release coordinate, the publication must include at least:

- the executable-class JAR used as the Maven dependency;
- its effective consumer POM with correct dependency scopes;
- a sources JAR;
- a Javadoc JAR or an explicitly justified documentation JAR;
- repository-generated integrity metadata; and
- required project, license, developer, source-control, and dependency
  metadata.

The public artifact is licensed under Apache License 2.0. The repository-root
`LICENSE` text and the published POM license identity and URL must agree.

The exact coordinate must be accepted by GitHub Packages and then resolved by
an authorized clean consumer before the release is called available. A
successful local package build alone is not publication.

### Artifact Content Boundary

The artifact may contain the current backend module's implementation classes,
adapters, auto-configuration metadata, and Flyway migrations required to run
the already-supported Orca behavior.

Packaging a class does not make that class public API. The supported embedded
integration API for this slice is limited to the three `auth.api` types listed
under Allowed public boundaries. Existing HTTP contracts remain public only
through their own authoritative specs.

The artifact must not contain:

- test classes, fixture application classes, local test credentials, or local
  environment overrides;
- CogniRig code, names, configuration, or branding;
- generated secrets, signing keys, repository tokens, or session values; or
- frontend/npm assets presented as part of the Java public contract.

## Ownership And Dependency Boundaries

| Mechanism | Owner | Authoritative predecessor | Allowed public boundary | Required completion proof |
| --- | --- | --- | --- | --- |
| embedded declaration and actor value | auth | `auth-12` | three `auth.api` types | existing auth and consumer contract tests |
| login and session creation | auth | `auth-08` | existing login HTTP and `ORCA_SESSION` contracts | regression tests |
| protected session resolution | auth | `auth-09` | auth-owned resolution invoked by the embedded boundary | regression tests |
| logout and revocation | auth | `auth-11` | existing logout HTTP contract | regression tests |
| stable rejection response | reference-core | `reference-core-01` | existing API error contract | regression tests |
| artifact classes and resources | contributing Orca scopes | their current authoritative specs | packaged implementation behind approved public APIs | content inventory and test suite |
| artifact packaging and delivery | `deployment` support scope | this spec | GitHub Packages Maven coordinate | staging and authenticated retrieval proofs |
| build/release mechanism | `deployment` support scope | this spec | repeatable release command/workflow; secrets remain external | clean build, validation, and release evidence |
| runtime datasource and values | consuming application | `auth-12` integration boundary and Spring runtime | consumer-supplied `DataSource` and external values | isolated startup proof |
| Orca-owned schema evolution | Flyway migrations contributed by owning Orca scopes | current migration-backed specs | packaged `db/migration` resources | migration inventory and database verification |

No new bounded context or support scope is introduced.

The consumer uses Orca internal infrastructure at runtime because the public
entry point composes the implementation, but the consumer must not import,
instantiate, copy, configure by internal type name, or depend on internal
packages directly.

## Consumer Integration Contract

An independent Maven consumer declares:

```xml
<dependency>
    <groupId>io.github.oneofwolvesbilly</groupId>
    <artifactId>orca</artifactId>
    <version>${orca.version}</version>
</dependency>
```

For acceptance, `${orca.version}` is resolved to the exact released version in
the consumer's effective POM. It is not inherited from Orca's reactor.

The consumer may:

- annotate its Spring Boot application with `@EnableOrcaEmbeddedAuth`;
- annotate a supported command handler with `@OrcaProtectedCommand`;
- receive `AuthenticatedActor` at that handler boundary;
- call the existing login and logout HTTP boundaries; and
- provide application-owned datasource and external runtime values.

The consumer must not:

- import any Orca package except the approved `auth.api` package for direct
  source-level integration;
- scan or instantiate Orca internal configuration classes by name;
- parse or persist `ORCA_SESSION` itself;
- copy Orca Java source or migration files;
- override auth/session behavior through a product-specific adapter; or
- define or mutate Orca-owned database tables outside packaged Flyway
  migrations.

## Runtime Configuration And Migration Contract

The consuming application owns:

- datasource location and connectivity;
- database and application credentials;
- secret storage and injection;
- environment-specific ports, hostnames, profiles, and deployment topology;
- the lifecycle of its runtime process; and
- product-specific configuration and behavior.

Orca owns:

- interpretation of its supported embedded-auth API;
- auth/session behavior already defined by auth specs;
- migration resources for Orca-owned tables; and
- validation failures produced by its public integration boundary.

Spring's standard `DataSource` mechanism is the allowed datasource boundary.
Orca must not require a consumer to configure an Orca internal persistence
class. Literal null, blank, malformed, or missing required runtime values must
fail before protected behavior is claimed available and must not reveal secret
values.

Flyway remains the only schema-definition mechanism for Orca-owned tables. The
published JAR must preserve the ordered migration resources present in the
release. The consumer supplies access to a compatible database; it does not
copy, rename, reorder, or edit Orca migrations.

The first public release must prove both:

- migration from an empty supported schema to the release's current schema;
  and
- startup against a schema already at that release's current migration state.

Later releases must additionally prove every upgrade path declared supported
by their compatibility matrix. Downgrade is unsupported unless a later spec
defines it.

## Compatibility Contract

Every public release must publish or link one version-specific compatibility
record containing:

- exact Orca release version;
- minimum Java runtime and each verified Java runtime;
- supported Spring Boot version or bounded version range;
- supported database product and verified versions;
- supported migration starting states;
- the public embedded-auth API surface; and
- known unsupported combinations.

The initial implementation baseline is Java release level 21, Spring Boot
4.0.1, and MariaDB, because those are the current backend build and runtime
inputs. This baseline is not itself a compatibility claim: the release may
claim only combinations exercised by its automated matrix or a recorded
reproducible proof.

Compatibility rules:

- patch releases preserve the supported public API and behavior;
- minor releases may add backward-compatible public capability;
- after `1.0.0`, breaking public API or required-runtime changes require a
  major release;
- while the project remains at major version zero, an incompatible change
  requires at least a minor-version increment, an explicit compatibility
  warning, and a migration path; a patch release remains compatible;
- an unsupported consumer combination must fail with actionable dependency,
  build, startup, or compatibility evidence rather than silently running an
  unverified configuration; and
- publishing an artifact does not change the README statement that Orca is not
  yet a production-ready identity platform.

## Publication Workflow Contract

The build/release mechanism must provide a repeatable path that:

1. starts from one identified source commit on the user's local `main` after
   separately authorized delivery;
2. selects an unused release version and records the compatibility matrix;
3. runs all required backend, fixture, packaging, and migration verification;
4. builds release JAR, POM, sources, and Javadoc/documentation without
   embedding release credentials;
5. validates the component set through a non-public staging step;
6. requires explicit human authorization before irreversible public
   publication;
7. publishes the validated component set to GitHub Packages through an
   explicitly activated Maven profile that requires the release gate and
   rejects snapshot publication;
8. waits until the exact coordinate is retrievable from the canonical GitHub
   Packages repository with consumer-owned credentials; and
9. runs the isolated consumer proof with a clean local repository.

No SDD, DDD, test, or implementation authorization implicitly grants GitHub
package creation, credential creation, upload, publication, tagging, push, or
other external release mutation.

If validation or publication fails, the workflow must leave the failed release
unclaimed. If a coordinate has become public, it must never be replaced; fixes
use a new version.

## Independent Consumer Verification Boundary

The existing Minimal Consumer Fixture remains useful for behavior regression,
but it cannot be the final delivery proof because it shares the Orca reactor
and version.

The final proof must build a standalone consumer that:

- lives outside the Orca Maven reactor;
- has no Orca source checkout, module relationship, or file dependency;
- starts with a clean, isolated Maven local repository;
- uses the Orca GitHub Packages Maven registry as the only Orca artifact
  source;
- pins the exact released Orca version;
- imports only the three approved `auth.api` types;
- provides its own datasource and runtime values;
- allows packaged Flyway migrations to establish Orca-owned schema;
- starts the public embedded-auth entry point;
- verifies login, protected actor resolution, logout, and post-logout
  rejection; and
- records dependency resolution and test results without printing secrets.

A pre-publication standalone fixture may resolve the release candidate from a
temporary staging repository to find packaging defects. It is additional
evidence, not a substitute for post-publication GitHub Packages retrieval.

## Runtime And Public Failure Set

| Failure class | Applicable boundary and example | Normative outcome | Required proof |
| --- | --- | --- | --- |
| absent | artifact, canonical repository result, exact version, datasource, or required runtime value is absent | resolution, build, or startup fails; protected behavior is unavailable | automated negative test where local; reproducible repository proof where external |
| null | a public runtime configuration source supplies literal null for a required value or object | validation/startup fails without coercing null or exposing a secret | automated configuration/startup test |
| blank | groupId, artifactId, version, repository input, or required configuration is blank | release validation or consumer startup fails | automated metadata/configuration test |
| malformed | coordinate, POM metadata, artifact archive, datasource value, or migration is malformed | staging, dependency resolution, or startup fails before availability is claimed | automated validation/corruption test or reproducible staging proof |
| duplicate | conflicting Orca versions, duplicate embedded providers, duplicate bean/configuration providers, or repeated publication coordinate | dependency/build/startup validation rejects ambiguity; a public coordinate is never overwritten | dependency-tree/startup tests and repository immutability proof |
| unsupported | Java, Spring Boot, database, migration state, or Orca combination is outside the compatibility record | no support claim; verified guard fails where detectable, otherwise documentation and matrix identify the exception | compatibility matrix test or explicit verification exception |
| untyped | environment, JVM property, YAML, XML, or JavaScript-generated build input reaches a public runtime/build boundary without compile-time type safety | runtime/build validation rejects invalid values explicitly | automated boundary test; not dismissed because Java API types exist |
| stale | snapshot/cached local artifact, old migration set, obsolete metadata, or consumer built for an incompatible public API | clean-repository proof or checksum/version comparison detects the mismatch; unsupported startup is not accepted | clean-cache retrieval and migration/compatibility tests |
| unauthorized | GitHub Packages publication or retrieval access is denied, the token is absent, or its scope is insufficient | publication or retrieval stops without leaking credentials; no availability claim is made | automated missing-settings proof plus reproducible denied-access proof with redacted output |
| unexpected | download interruption, checksum mismatch, dependency-resolution error, build error, Flyway error, startup exception, or public integration regression | release is not called available; evidence identifies the failed stage without secrets | automated failure injection where practical, otherwise reproducible manual proof with exception rationale |

No failure class is inapplicable. Although Maven XML has no native null token,
literal null remains reachable through external configuration and programmatic
public inputs, so it requires explicit runtime coverage.

## Acceptance Criteria

1. One exact, non-SNAPSHOT `io.github.oneofwolvesbilly:orca:<version>` release
   is available from the Orca GitHub Packages Maven registry and is not reused
   or overwritten by the release workflow.
2. The release contains the required JAR, POM, source/documentation artifacts,
   repository integrity metadata, and valid public metadata.
3. The release publishes a compatibility record backed by verification for
   every claimed combination.
4. A clean standalone consumer retrieves the artifact without an Orca checkout,
   repository-local install, copied source, or copied JAR; GitHub credentials
   are supplied only through external Maven settings.
5. Consumer source imports only the three approved `auth.api` types.
6. The consumer starts `@EnableOrcaEmbeddedAuth`, declares one supported
   `@OrcaProtectedCommand`, and receives exactly one `AuthenticatedActor`.
7. Existing login, actor resolution, logout, rejection, and session behavior
   remain unchanged and pass their authoritative regression suites.
8. Consumer-owned configuration and Orca-owned Flyway migration responsibilities
   are explicit and verified on an empty and already-current schema.
9. Conflicting versions/providers and unsupported runtime combinations cannot
   silently satisfy the compatibility claim.
10. Publication and verification output contains no GitHub credentials,
    session values, or consumer secrets.
11. Frontend/npm delivery and CogniRig-specific integration remain outside the
    release contract.

## Verification Mapping

| Normative outcome | Verification |
| --- | --- |
| artifact can be built | clean release-profile Maven build; inspect expected component files |
| stable versioned coordinates | automated effective-POM and filename assertions reject blank, range, alias, and SNAPSHOT versions |
| GitHub Packages publication is valid | explicit Maven deploy plus exact authenticated package retrieval |
| isolated consumer obtains dependency | standalone project with clean local Maven repository and no Orca checkout |
| no copied Orca source or JAR | fixture content/path assertion and build provenance inspection |
| no internal imports | source/import scan plus compile rule that permits only approved `auth.api` imports |
| public embedded-auth entry point starts | standalone consumer startup/integration test |
| login and actor resolution are unchanged | existing auth-08/auth-09/auth-12 tests plus standalone happy path |
| logout and rejection are unchanged | existing auth-11/reference-core tests plus standalone post-logout rejection |
| migration ownership is preserved | packaged-resource inventory, empty-schema migration, and already-current-schema startup |
| configuration ownership is preserved | standalone consumer supplies datasource/runtime values; null/blank/malformed/missing negative tests |
| missing artifact | clean-repository resolution against a guaranteed-unused version; build must fail |
| incompatible version/runtime | compatibility matrix negative job or explicit unsupported-combination startup/build proof |
| conflicting dependency/provider | dependency-tree convergence rule and duplicate-provider startup test |
| unauthorized repository operation | redacted staging/publication denial proof; external credential behavior is a reproducible manual proof because the remote service owns it |
| stale cache or artifact | clean local repository retrieval, exact version, and published checksum comparison |
| unexpected download/publication outage | explicit verification exception: external outage cannot be deterministically injected; release remains incomplete until retry and public retrieval succeed |

Every acceptance criterion must map to an automated test or reproducible proof
before SDD closeout. External GitHub Packages availability and authorization
failures may use reproducible redacted evidence because Orca does not own that
service, but they may not be omitted.

## Security And Secret Boundary

GitHub publication and package-read credentials are environment-owned secrets.
They must never appear in repository files, generated artifacts, logs, test
reports, command arguments captured by the repository, or consumer examples.

Verification may assert that a value exists or that an operation was denied,
but must redact credential values. Consumers use a read-scoped credential and
must not receive the publication credential.

## Affected And Superseded Documents

At implementation verification:

- this new deployment-03 spec is added;
- `docs/drafts/slice-planning-handoff.md` records the intake disposition;
- no authoritative spec is superseded;
- `auth-12` remains the public embedded behavior authority;
- `docs/document-map.md` is checked and needs no change because its existing
  deployment spec and DDD patterns already cover deployment-03;
- `docs/product/orca-sa-baseline.md`, `docs/product/workflow-map.md`, and
  `docs/product/capability-map.md` record the verified local release
  implementation, its Core V1 milestone role, and the remaining GitHub Packages
  publication proof;
- `docs/slice-map.md` is checked and needs no change because this milestone
  alignment does not add or renumber a behavior slice;
- README records the Core V1 milestone while continuing to state that Orca is
  under active development and the milestone is not released; and
- the matching deployment-03 DDD remains aligned as the derived design
  authority.

SDD closeout reconciled `ORCA-DELIVERY-01` against these acceptance criteria,
failure cases, non-goals, verification requirements, and affected documents on
2026-09-12. The backend artifact outcome is fully represented here. The
frontend distribution remainder stays active and outside this slice.

On 2026-09-17 the distribution decision was corrected after re-reading the
original intake: Maven consumption required an approved repository boundary but
did not authorize Maven Central. The user selected GitHub-only source and
artifact delivery for consumer products. GitHub Packages therefore replaces
Maven Central without changing the slice's actor, Maven coordinate, packaged
behavior, or one-slice boundary.

The corrected implementation was verified on 2026-09-20 with GitHub-specific
profile contract tests, external-settings failure tests, effective-POM
inspection, a staged artifact resolved by the standalone consumer, and the full
Maven reactor. Maven enforcer also rejects snapshot publication when the GitHub
profile is selected without the release gate. Actual GitHub Packages publication
remains pending and requires separate authorization.

## Remaining Release Boundary

TDD and release implementation were explicitly authorized and verified on
2026-09-16. That authorization does not grant:

- creating GitHub package credentials or changing package permissions;
- uploading or publishing a GitHub Package;
- tagging, committing, rebasing, merging, or pushing; or
- closing or tombstoning `ORCA-DELIVERY-01` before GitHub package publication
  and authenticated retrieval evidence exists.

The next required release proof is a candidate built from a committed source
state with exact verified Java and MariaDB versions. GitHub Packages publication
and authenticated package retrieval still require separate explicit
authorization.
