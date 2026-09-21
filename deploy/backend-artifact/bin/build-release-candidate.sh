#!/usr/bin/env sh
set -eu

SCRIPT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_ROOT/../../.." && pwd)
VALIDATOR="$SCRIPT_ROOT/validate-release-version.sh"

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  printf '%s\n' 'Usage: build-release-candidate.sh <version> [absolute-output-directory]' >&2
  exit 64
fi

release_version=$1
sh "$VALIDATOR" "$release_version"

verified_java_runtimes=${ORCA_RELEASE_VERIFIED_JAVA_RUNTIMES:-}
verified_database_versions=${ORCA_RELEASE_VERIFIED_DATABASE_VERSIONS:-}
if [ -z "$verified_java_runtimes" ] || [ -z "$verified_database_versions" ]; then
  printf '%s\n' 'Verified Java and MariaDB version evidence is required.' >&2
  exit 65
fi
if printf '%s' "$verified_java_runtimes" | LC_ALL=C grep -q '[[:cntrl:]]' || \
   printf '%s' "$verified_database_versions" | LC_ALL=C grep -q '[[:cntrl:]]'; then
  printf '%s\n' 'Verified compatibility inputs must not contain control characters.' >&2
  exit 65
fi

candidate_directory=${2:-"$PROJECT_ROOT/target/backend-artifact-release/$release_version"}
case "$candidate_directory" in
  /*) ;;
  *)
    printf '%s\n' 'Release candidate output directory must be absolute.' >&2
    exit 65
    ;;
esac

if [ -e "$candidate_directory" ]; then
  printf '%s\n' 'Release candidate output directory already exists.' >&2
  exit 73
fi

if ! git -C "$PROJECT_ROOT" diff --quiet || ! git -C "$PROJECT_ROOT" diff --cached --quiet; then
  printf '%s\n' 'Tracked working-tree changes must be committed before building a release candidate.' >&2
  exit 69
fi

source_commit=$(git -C "$PROJECT_ROOT" rev-parse --verify HEAD)
maven_command=${ORCA_RELEASE_MAVEN_COMMAND:-"$PROJECT_ROOT/orca_backend/mvnw"}

mkdir -p "$candidate_directory"
repository_url="file://$candidate_directory"

"$maven_command" \
  -f "$PROJECT_ROOT/orca_backend/pom.xml" \
  -Pbackend-artifact-release \
  -DbackendArtifactRelease=true \
  -Drevision="$release_version" \
  -Dchangelist= \
  -Dorca.source.commit="$source_commit" \
  -Dorca.java.verified-runtimes="$verified_java_runtimes" \
  -Dorca.database.verified-versions="$verified_database_versions" \
  -DaltDeploymentRepository="orca-candidate::$repository_url" \
  clean deploy

printf 'Release candidate repository: %s\n' "$repository_url"
