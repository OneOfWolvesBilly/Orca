# DDD - Deployment 03 - Versioned Backend Artifact Delivery

Status: Approved / TDD pending.

This note is derived from the approved contract in
`docs/specs/deployment/03-versioned-backend-artifact-delivery.md`.
It must not introduce behavior beyond that spec.

## Purpose

Derive the implementation boundaries for producing, validating, publishing,
and independently consuming one versioned Orca backend artifact without
turning deployment into a business bounded context or changing existing auth
behavior.

The single actor-visible outcome remains:

- an application developer declares one exact Orca release from Maven Central
  and uses the approved embedded-auth API without an Orca checkout, copied
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
  JAR, signatures, checksums, compatibility record, and provenance produced
  from one source commit before public publication.

- **Central Bundle**
  The Maven repository-layout bundle submitted to Central Publisher Portal.

- **User-managed Deployment**
  A Portal deployment that may be uploaded and validated but cannot publish
  automatically. Explicit human action remains required after validation.

- **Compatibility Record**
  Version-specific resolved metadata naming the Java, Spring Boot, MariaDB,
  and migration combinations actually verified for the release.

- **Independent Consumer Fixture**
  A standalone Spring Boot Maven project that is not a reactor module and
  resolves Orca only through a supplied Maven repository boundary.

- **Clean Repository Proof**
  A consumer build using a newly created isolated Maven local repository with
  no preinstalled Orca artifact.

- **Public Retrieval Proof**
  The post-publication clean repository proof whose Orca source is Maven
  Central.

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
same source commit and build invocation. Mixing files, metadata, signatures, or
compatibility evidence from different versions or commits invalidates the
candidate.

The release states are:

```text
Development Snapshot
  -> Candidate Built
  -> Candidate Locally Verified
  -> Portal Uploaded
  -> Portal Validated
  -> Publication Authorized
  -> Published
  -> Publicly Verified
```

Rules:

- A failure stops progression and records the failed state.
- `Portal Validated` is not `Published`.
- Only explicit user authorization may move a validated deployment toward
  publication.
- `Published` is immutable; correction creates a new Release Version.
- The actor-visible outcome is complete only at `Publicly Verified`.
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
    verify-public-release.sh
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
- a coordinate already visible in Maven Central.

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
- signing every required component;
- building the Central repository-layout bundle; and
- invoking the Central publishing extension only when the selected command
  permits it.

All build and publishing plugin versions must be pinned. The Central extension
is `org.sonatype.central:central-publishing-maven-plugin`; implementation must
pin the then-current officially documented stable version and record the
verification source when it is added.

The profile must configure:

```text
publishingServerId=central
autoPublish=false
checksums=all
```

`autoPublish=false` is mandatory. No committed profile, script, CI job, or
default property may turn validation success into automatic public
publication.

### Central-required metadata

The flattened consumer POM owns:

- project name and description;
- project URL;
- license identity and URL;
- developer identity;
- source-control connection and URL;
- `jar` packaging;
- exact GAV; and
- correct compile/runtime/optional dependency scopes.

The release verifier must inspect the flattened POM rather than assuming the
source POM is the consumer result.

### Secret boundary

Central user-token credentials live under server id `central` in an external
Maven `settings.xml`. Signing key material and passphrases also remain outside
the repository.

Scripts may validate only presence and successful use. They must not echo,
serialize, copy, archive, checksum, or include secrets in release evidence.

Namespace verification, user-token creation, signing-key creation, Portal
upload, and publication are external mutations. Each requires the relevant
user authorization; DDD or implementation authorization alone is insufficient.

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
required .asc signatures
required checksum files
```

The release verifier inspects the binary JAR and rejects:

- missing `io/github/oneofwolvesbilly/orca/auth/api` classes;
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
- direct imports limited to `EnableOrcaEmbeddedAuth`,
  `OrcaProtectedCommand`, and `AuthenticatedActor`;
- one product-neutral protected command; and
- integration tests for login, actor resolution, logout, and post-logout
  rejection.

The verification runner copies only the fixture into a temporary directory and
creates a second temporary directory for `maven.repo.local`. It must validate
both directories before cleanup and must never use a workspace root, `$HOME`,
or unresolved broad path as a cleanup target.

Pre-publication mode points the fixture at the candidate's generated Maven
repository layout. Post-publication mode points it at Maven Central. Neither
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
- Central bundle creation and user-managed publication gates;
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

Reference-core remains authoritative for stable API errors. Delivery tests may
assert the existing public result but must not introduce a release-specific
error envelope.

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
  -> Central bundle / Portal boundary

Independent Consumer Fixture
  -> Maven repository boundary
  -> io.github.oneofwolvesbilly:orca:<exact-version>
  -> auth.api public boundary
  -> packaged Orca implementation

Packaged Orca implementation
  -> existing auth/reference-core/application ports
  -> existing domain and infrastructure

Orca domain
  -> no deployment, release, Portal, fixture, or consumer dependency
```

No production source dependency may point from Orca into
`deploy/backend-artifact` or the independent fixture.

## Test Layer Placement

No domain or application test is added because this slice introduces no domain
or use-case rule.

Maven/build contract tests validate:

- exact resolved release coordinates;
- flattened consumer POM metadata and dependency scopes;
- required component classifiers, signatures, and checksums;
- ordinary classes-JAR packaging;
- compatibility record completeness and consistency;
- Central configuration keeps `autoPublish=false`; and
- snapshot/default development builds remain available without activating
  release publication.

Shell contract tests use controlled Maven, GPG, archive, checksum, Docker, and
Portal adapters where network or secret-bearing behavior must not run. Test
names should remain behavior oriented, including:

- `builds one immutable backend release candidate`
- `rejects an invalid release version`
- `keeps Central publication user managed`
- `rejects incomplete Central metadata`
- `rejects artifact content outside the backend boundary`
- `verifies an isolated consumer from staged artifacts`
- `verifies an isolated consumer from Maven Central`
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
| malformed | POM, archive, signature, checksum, migration, and datasource validators | fail before validation/public claim |
| duplicate | Maven convergence rule, archive scan, Spring startup validation, Central coordinate check | reject ambiguity or reuse |
| unsupported | compatibility matrix and runtime matrix | record unsupported; never claim passing support |
| untyped | shell/environment/XML/YAML boundary validators | reject before invoking secret-bearing or runtime action |
| stale | clean local repository, checksum comparison, source-commit check, migration matrix | fail provenance or compatibility proof |
| unauthorized | external settings preflight and Portal response adapter | stop with redacted actionable failure |
| unexpected | command wrapper and candidate state controller | stop at named stage; preserve non-secret diagnostics |

External outage and real Portal authorization behavior use reproducible,
redacted manual evidence. Controlled adapters test Orca's reaction without
requiring publication during ordinary tests.

## Release Command Separation

Implementation must expose separate commands or explicit modes for:

1. build candidate without network publication;
2. verify candidate through local staging;
3. upload for Portal validation after external-mutation authorization;
4. publish a validated deployment after a second explicit authorization; and
5. verify anonymous Maven Central retrieval.

One command must not silently cross these boundaries. In particular, ordinary
`test`, `package`, `verify`, or repository-local `deploy` execution must never
publish publicly.

## Design Decisions

### Decision: Preserve the existing coordinate family

The current backend already builds as `io.github.oneofwolvesbilly:orca`. Keeping
that family avoids an unnecessary module extraction or artifact rename. Public
API remains constrained by package contract rather than by pretending every
packaged class is supported.

### Decision: Use Maven Central

Maven Central is the default public Maven source and permits ordinary consumers
to resolve a release without repository-local installation or private
credentials. A private repository would not prove the product-neutral public
delivery outcome.

### Decision: Use CI-friendly versions with a flattened consumer POM

This preserves the normal snapshot development build while allowing an exact
release version to be injected without hand-editing source files. Maven 3
requires the flattened POM so consumers receive resolved coordinates.

### Decision: Keep publication user managed

Central bundle creation and validation are reversible review steps. Public
publication is immutable, so `autoPublish=false` preserves a distinct human
authorization boundary.

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
- Activating `autoPublish` would collapse the required human gate.
- Repackaging as a fat JAR could hide or break Maven dependency behavior.
- Testing against the ordinary developer Maven cache could accidentally consume
  a locally installed Orca artifact.
- Leaving the standalone fixture in the reactor would repeat the existing proof
  gap.
- Treating every packaged internal class as public API would silently expand
  compatibility obligations and absorb `ORCA-ARCH-01`.
- Hand-writing compatibility claims without running the matrix could advertise
  unsupported Java, Spring Boot, MariaDB, or migration combinations.
- Logging Maven settings, token values, GPG arguments, datasource credentials,
  or session cookies could leak secrets.
- Calling Portal validation or local staging “published” would report the actor
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
- No automatic Central publication.
- No namespace, token, signing-key, upload, publication, tag, commit, merge, or
  push authorization.

## DDD Closeout Evidence

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

The next layer is TDD only after explicit user authorization and DDD closeout
approval.
