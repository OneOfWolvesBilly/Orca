#!/usr/bin/env sh
set -u

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
BACKEND_POM="$PROJECT_ROOT/orca_backend/pom.xml"
ROOT_POM="$PROJECT_ROOT/pom.xml"
REACTOR_FIXTURE_POM="$PROJECT_ROOT/minimal_consumer_fixture/pom.xml"
RELEASE_ROOT="$PROJECT_ROOT/deploy/backend-artifact"
CONSUMER_ROOT="$RELEASE_ROOT/consumer-fixture"

failures=0

pass() {
  printf 'ok - %s\n' "$1"
}

fail() {
  printf 'not ok - %s\n' "$1"
  if [ -n "${2:-}" ]; then
    printf '%s\n' "$2" | sed 's/^/  /'
  fi
  failures=$((failures + 1))
}

assert_file() {
  name="$1"
  path="$2"
  if [ -f "$path" ]; then
    pass "$name"
  else
    fail "$name" "Missing file: ${path#$PROJECT_ROOT/}"
  fi
}

assert_contains() {
  name="$1"
  path="$2"
  expected="$3"
  if [ ! -f "$path" ]; then
    fail "$name" "Missing file: ${path#$PROJECT_ROOT/}"
    return
  fi
  if grep -Fq -- "$expected" "$path"; then
    pass "$name"
  else
    fail "$name" "Expected ${path#$PROJECT_ROOT/} to contain: $expected"
  fi
}

assert_not_contains() {
  name="$1"
  path="$2"
  forbidden="$3"
  if [ ! -f "$path" ]; then
    fail "$name" "Missing file: ${path#$PROJECT_ROOT/}"
    return
  fi
  if grep -Fq -- "$forbidden" "$path"; then
    fail "$name" "Forbidden content in ${path#$PROJECT_ROOT/}: $forbidden"
  else
    pass "$name"
  fi
}

assert_shell_syntax() {
  name="$1"
  path="$2"
  if [ ! -f "$path" ]; then
    fail "$name" "Missing file: ${path#$PROJECT_ROOT/}"
    return
  fi
  if sh -n "$path"; then
    pass "$name"
  else
    fail "$name" "Invalid shell syntax: ${path#$PROJECT_ROOT/}"
  fi
}

assert_fixture_outside_reactor() {
  name="$1"
  if grep -Fq '<module>deploy/backend-artifact/consumer-fixture</module>' "$ROOT_POM"; then
    fail "$name" "Independent consumer fixture must not be a reactor module."
  else
    pass "$name"
  fi
}

assert_public_orca_imports_only() {
  name="$1"
  if [ ! -d "$CONSUMER_ROOT/src" ]; then
    fail "$name" "Missing independent consumer source tree."
    return
  fi

  if java "$RELEASE_ROOT/bin/ConsumerSourceBoundary.java" "$CONSUMER_ROOT/src"; then
    pass "$name"
  else
    fail "$name" "Consumer depends on an unsupported Orca type."
  fi
}

assert_no_copied_orca_sources() {
  name="$1"
  copied=$(find "$CONSUMER_ROOT" -type f \
    \( -path '*/io/github/oneofwolvesbilly/orca/*' -o -path '*/db/migration/*' \) \
    -print 2>/dev/null || true)
  if [ -n "$copied" ]; then
    fail "$name" "$copied"
  else
    pass "$name"
  fi
}

BUILD_SCRIPT="$RELEASE_ROOT/bin/build-release-candidate.sh"
VERSION_VALIDATOR="$RELEASE_ROOT/bin/validate-release-version.sh"
VERIFY_SCRIPT="$RELEASE_ROOT/bin/verify-release-candidate.sh"
GITHUB_VERIFY_SCRIPT="$RELEASE_ROOT/bin/verify-github-package.sh"
FIXTURE_POM="$CONSUMER_ROOT/pom.xml"
FIXTURE_TEST="$CONSUMER_ROOT/src/test/java/io/github/oneofwolvesbilly/orcaconsumer/PublishedArtifactConsumerTest.java"

assert_contains \
  "uses a CI-friendly version in the root reactor" \
  "$ROOT_POM" \
  '<version>${revision}${changelist}</version>'
assert_contains \
  "uses a CI-friendly version in the backend artifact" \
  "$BACKEND_POM" \
  '<version>${revision}${changelist}</version>'
assert_contains \
  "keeps the repository fixture aligned with the CI-friendly version" \
  "$REACTOR_FIXTURE_POM" \
  '<version>${revision}${changelist}</version>'
assert_contains \
  "defaults development builds to a snapshot changelist" \
  "$ROOT_POM" \
  '<changelist>-SNAPSHOT</changelist>'
assert_contains \
  "defines the backend artifact release profile" \
  "$BACKEND_POM" \
  '<id>backend-artifact-release</id>'
assert_contains \
  "flattens the Maven 3 consumer POM" \
  "$BACKEND_POM" \
  '<artifactId>flatten-maven-plugin</artifactId>'
assert_contains \
  "attaches backend sources" \
  "$BACKEND_POM" \
  '<artifactId>maven-source-plugin</artifactId>'
assert_contains \
  "attaches backend Javadoc" \
  "$BACKEND_POM" \
  '<artifactId>maven-javadoc-plugin</artifactId>'
assert_contains \
  "defines the GitHub Packages publication profile" \
  "$BACKEND_POM" \
  '<id>backend-artifact-github-packages</id>'
assert_contains \
  "uses the standard Maven deploy plugin" \
  "$BACKEND_POM" \
  '<artifactId>maven-deploy-plugin</artifactId>'
assert_contains \
  "uses the external GitHub server identity" \
  "$BACKEND_POM" \
  '<id>github</id>'
assert_contains \
  "targets the Orca GitHub Packages registry" \
  "$BACKEND_POM" \
  '<url>https://maven.pkg.github.com/OneOfWolvesBilly/Orca</url>'
assert_contains \
  "requires the release gate before GitHub publication" \
  "$BACKEND_POM" \
  '<property>backendArtifactRelease</property>'
assert_contains \
  "rejects snapshot publication to GitHub Packages" \
  "$BACKEND_POM" \
  '<requireReleaseVersion/>'
assert_contains \
  "enforces dependency convergence" \
  "$BACKEND_POM" \
  '<dependencyConvergence/>'
assert_not_contains \
  "does not retain the Central Publisher extension" \
  "$BACKEND_POM" \
  'central-publishing-maven-plugin'
assert_not_contains \
  "does not require GPG for GitHub Packages" \
  "$BACKEND_POM" \
  'maven-gpg-plugin'

assert_contains "publishes project URL metadata" "$BACKEND_POM" '<url>'
assert_contains "publishes license metadata" "$BACKEND_POM" '<licenses>'
assert_contains "publishes developer metadata" "$BACKEND_POM" '<developers>'
assert_contains "publishes source control metadata" "$BACKEND_POM" '<scm>'

assert_file "provides the release candidate build command" "$BUILD_SCRIPT"
assert_file "provides the release version validator" "$VERSION_VALIDATOR"
assert_file "provides the staged candidate verification command" "$VERIFY_SCRIPT"
assert_file "provides the GitHub Package verification command" "$GITHUB_VERIFY_SCRIPT"
assert_shell_syntax "parses the release candidate build command" "$BUILD_SCRIPT"
assert_shell_syntax "parses the release version validator" "$VERSION_VALIDATOR"
assert_shell_syntax "parses the staged candidate verification command" "$VERIFY_SCRIPT"
assert_shell_syntax "parses the GitHub Package verification command" "$GITHUB_VERIFY_SCRIPT"
assert_not_contains "candidate build never pushes Git state" "$BUILD_SCRIPT" 'git push'
assert_not_contains "candidate build never creates a Git tag" "$BUILD_SCRIPT" 'git tag'
assert_not_contains "candidate build never activates GitHub publication" "$BUILD_SCRIPT" 'backendArtifactGitHubPackages=true'
assert_not_contains "candidate build never uses the GitHub Packages profile" "$BUILD_SCRIPT" 'backend-artifact-github-packages'
assert_contains \
  "candidate build injects verified Java evidence" \
  "$BUILD_SCRIPT" \
  '-Dorca.java.verified-runtimes='
assert_contains \
  "candidate build injects verified database evidence" \
  "$BUILD_SCRIPT" \
  '-Dorca.database.verified-versions='
assert_contains \
  "GitHub verification uses the canonical package repository" \
  "$GITHUB_VERIFY_SCRIPT" \
  'https://maven.pkg.github.com/OneOfWolvesBilly/Orca'
assert_contains \
  "GitHub verification requires external Maven settings" \
  "$GITHUB_VERIFY_SCRIPT" \
  'ORCA_RELEASE_MAVEN_SETTINGS'
assert_not_contains \
  "GitHub verification never embeds a token" \
  "$GITHUB_VERIFY_SCRIPT" \
  'read:packages'
if ORCA_RELEASE_VERIFIED_JAVA_RUNTIMES= \
   ORCA_RELEASE_VERIFIED_DATABASE_VERSIONS= \
   sh "$BUILD_SCRIPT" 1.2.3 >/dev/null 2>&1
then
  fail "candidate build rejects absent compatibility evidence" \
    "Expected the release candidate build to stop before Maven."
else
  pass "candidate build rejects absent compatibility evidence"
fi

assert_file "provides an independent consumer POM" "$FIXTURE_POM"
assert_not_contains "independent consumer has no Orca parent" "$FIXTURE_POM" '<artifactId>orca-reactor</artifactId>'
assert_contains "independent consumer pins the Orca group" "$FIXTURE_POM" '<groupId>io.github.oneofwolvesbilly</groupId>'
assert_contains "independent consumer pins the Orca artifact" "$FIXTURE_POM" '<artifactId>orca</artifactId>'
assert_contains "independent consumer receives an exact Orca version" "$FIXTURE_POM" '<version>${orca.version}</version>'
assert_contains "independent consumer receives its repository boundary" "$FIXTURE_POM" '<url>${orca.repository.url}</url>'
assert_fixture_outside_reactor "keeps the independent consumer outside the Orca reactor"
assert_public_orca_imports_only "allows only the ten approved Orca public types"
assert_no_copied_orca_sources "copies neither Orca source nor Flyway migrations"

assert_file "provides the published artifact consumer integration test" "$FIXTURE_TEST"
assert_contains \
  "tests login actor resolution and logout rejection" \
  "$FIXTURE_TEST" \
  'login_resolves_public_actor_and_logout_rejects_reuse'
assert_contains \
  "tests missing session rejection before consumer behavior" \
  "$FIXTURE_TEST" \
  'missing_session_rejects_before_consumer_command'
assert_contains \
  "tests packaged migrations from an empty schema" \
  "$FIXTURE_TEST" \
  'packaged_migrations_start_an_empty_schema'
assert_contains \
  "tests startup against an already-current schema" \
  "$FIXTURE_TEST" \
  'startup_against_current_schema_does_not_add_migration'

if [ "$failures" -ne 0 ]; then
  printf '%s\n' "$failures test(s) failed."
  exit 1
fi

printf '%s\n' 'All backend artifact release contract tests passed.'
