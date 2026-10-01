# Backend artifact release candidate

This support scope builds and verifies the versioned Orca backend artifact. It
does not publish to GitHub Packages unless a separately authorized Maven
`deploy` command explicitly activates the GitHub Packages profile. It does not
change Git state.

## Prerequisites

- JDK 21 or a separately verified newer JDK (the source guard uses the JDK parser)
- Python 3 for component/provenance verification
- exact Java and MariaDB verification evidence for the compatibility record
- an external Maven `settings.xml` with GitHub credentials only when a
  separately authorized publication or package verification is performed

## Build a local candidate

Commit tracked implementation changes, then run:

```sh
ORCA_RELEASE_VERIFIED_JAVA_RUNTIMES='21.0.x' \
ORCA_RELEASE_VERIFIED_DATABASE_VERSIONS='MariaDB 11.x.y' \
  ./deploy/backend-artifact/bin/build-release-candidate.sh 1.2.3
```

The command rejects mutable versions and existing output directories. It writes
the candidate to `target/backend-artifact-release/<version>` by default. Replace
the example compatibility values with the exact versions exercised by the
release verification matrix; absent evidence stops the build. A successful
build also writes `orca-release-evidence.json` with the exact coordinate, source
commit, compatibility record and SHA-256 digests of all four components. Keep
that file as the reviewed expectation; do not regenerate it from downloaded
bytes to make a checksum mismatch pass.

## Verify a staged candidate

```sh
ORCA_RELEASE_EXPECTED_MANIFEST=/absolute/path/to/orca-release-evidence.json \
ORCA_RELEASE_EVIDENCE_OUTPUT=/absolute/path/to/new-consumer-result.json \
  ./deploy/backend-artifact/bin/verify-release-candidate.sh \
  1.2.3 \
  file:///absolute/path/to/repository
```

Verification copies only the fixture POM and source/resources into a temporary
directory, excludes generated classes/JARs/target output, parses Java imports
and qualified references against the exact three auth plus seven audit types,
and starts with a fresh Maven repository. Maven strict checksums, the retained
manifest, complete JAR/POM/sources/Javadoc inventory, resolution origin, and
successful audit/compilation/auth/migration test reports must all pass. Maven
success without the expected reports is insufficient.

`ORCA_RELEASE_EVIDENCE_OUTPUT` is optional and must name a new file. Its report
contains coordinates, source commit, hashes and passing suite counts, never
Maven settings or raw test output. This result does not establish the release's
Java/MariaDB matrix. H2 fixture success is not MariaDB verification.

The audit fixture explicitly supplies its own recorder and typed mapper. Its
six behavior/metadata tests and three compilation/signature tests do not use
auth enablement, credential seeding or a database; the four existing auth and
migration tests remain independent regressions. Only the seven audit types
named in reference-core-03 are supported; sibling public classes are not APIs.

## Publish to GitHub Packages

Publication is an external mutation and is not part of candidate construction.
After separate authorization, configure server id `github` in an external
Maven `settings.xml`, then invoke the release and GitHub Packages profiles
explicitly. Upload the already verified component files rather than rebuilding
them during publication, so the retained SHA-256 evidence still applies:

```sh
./orca_backend/mvnw \
  -s /absolute/path/to/settings.xml \
  -f orca_backend/pom.xml \
  -Pbackend-artifact-release,backend-artifact-github-packages \
  -DbackendArtifactRelease=true \
  -DbackendArtifactGitHubPackages=true \
  -Drevision=1.2.3 \
  -Dchangelist= \
  -Dorca.source.commit=<40-character-commit> \
  -Dorca.java.verified-runtimes='<verified-runtimes>' \
  -Dorca.database.verified-versions='<verified-database-versions>' \
  -Dfile=/absolute/path/to/candidate/io/github/oneofwolvesbilly/orca/1.2.3/orca-1.2.3.jar \
  -DpomFile=/absolute/path/to/candidate/io/github/oneofwolvesbilly/orca/1.2.3/orca-1.2.3.pom \
  -Dsources=/absolute/path/to/candidate/io/github/oneofwolvesbilly/orca/1.2.3/orca-1.2.3-sources.jar \
  -Djavadoc=/absolute/path/to/candidate/io/github/oneofwolvesbilly/orca/1.2.3/orca-1.2.3-javadoc.jar \
  -DrepositoryId=github \
  -Durl=https://maven.pkg.github.com/OneOfWolvesBilly/Orca \
  validate org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file
```

The profile publishes only the backend artifact to
`https://maven.pkg.github.com/OneOfWolvesBilly/Orca`. Credentials must stay in
the external settings file or GitHub Actions secrets and must never be committed.

## Verify a GitHub Package

After the separately authorized publication completes:

```sh
ORCA_RELEASE_EXPECTED_MANIFEST=/absolute/path/to/reviewed-staged-evidence.json \
ORCA_RELEASE_MAVEN_SETTINGS=/absolute/path/to/settings.xml \
  ./deploy/backend-artifact/bin/verify-github-package.sh 1.2.3
```

The command uses a clean local Maven repository and resolves the exact package
from GitHub Packages. GitHub currently requires authentication to install Maven
packages even when the package and source repository are public.

Published retrieval must match the retained staged component hashes; a rebuilt
or substituted component set requires a new review and unused version rather
than replacing expected hashes after retrieval. The GitHub wrapper rejects
`repository-local` development manifests. No command here grants publication,
Git integration, or commit authorization.

## Development verification

Run the release contract suites and Python inventory/source-boundary tests:

```sh
sh deploy/backend-artifact/test/validate-release-version.test.sh
sh deploy/backend-artifact/test/verify-release-contract.test.sh
sh deploy/backend-artifact/test/verify-release-candidate.test.sh
python3 -m unittest discover -s deploy/backend-artifact/test -p 'test_*.py'
```

Python inventory fixtures contain synthetic archive entries and controlled
Maven doubles. They prove failure handling, not Java execution or retrieval.
The real standalone fixture must also pass against the exact built artifact.
Development assembly from an uncommitted tree may exercise the verifier with a
`repository-local` manifest, but it is not the committed staged proof required
for release. Commit the implementation first, use the guarded candidate builder,
and verify that candidate again before claiming staged acceptance.
