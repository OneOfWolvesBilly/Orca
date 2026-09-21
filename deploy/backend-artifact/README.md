# Backend artifact release candidate

This support scope builds and verifies the versioned Orca backend artifact. It
does not publish to GitHub Packages unless a separately authorized Maven
`deploy` command explicitly activates the GitHub Packages profile. It does not
change Git state.

## Prerequisites

- Java 21
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
release verification matrix; absent evidence stops the build.

## Verify a staged candidate

```sh
./deploy/backend-artifact/bin/verify-release-candidate.sh \
  1.2.3 \
  file:///absolute/path/to/repository
```

Verification copies the independent consumer fixture to a temporary directory,
uses a fresh Maven local repository, resolves only the requested version, and
runs its public API and migration tests.

## Publish to GitHub Packages

Publication is an external mutation and is not part of candidate construction.
After separate authorization, configure server id `github` in an external
Maven `settings.xml`, then invoke the release and GitHub Packages profiles
explicitly:

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
  clean deploy
```

The profile publishes only the backend artifact to
`https://maven.pkg.github.com/OneOfWolvesBilly/Orca`. Credentials must stay in
the external settings file or GitHub Actions secrets and must never be committed.

## Verify a GitHub Package

After the separately authorized publication completes:

```sh
ORCA_RELEASE_MAVEN_SETTINGS=/absolute/path/to/settings.xml \
  ./deploy/backend-artifact/bin/verify-github-package.sh 1.2.3
```

The command uses a clean local Maven repository and resolves the exact package
from GitHub Packages. GitHub currently requires authentication to install Maven
packages even when the package and source repository are public.
