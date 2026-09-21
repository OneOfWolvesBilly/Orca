#!/usr/bin/env sh
set -eu

SCRIPT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELEASE_ROOT=$(CDPATH= cd -- "$SCRIPT_ROOT/.." && pwd)
VALIDATOR="$SCRIPT_ROOT/validate-release-version.sh"
CONSUMER_FIXTURE="$RELEASE_ROOT/consumer-fixture"

if [ "$#" -ne 2 ]; then
  printf '%s\n' 'Usage: verify-release-candidate.sh <version> <repository-url>' >&2
  exit 64
fi

release_version=$1
repository_url=$2
sh "$VALIDATOR" "$release_version"

if printf '%s' "$repository_url" | LC_ALL=C grep -q '[[:space:]]'; then
  printf '%s\n' 'Repository URL must not contain whitespace.' >&2
  exit 65
fi

case "$repository_url" in
  file:///*)
    repository_path=${repository_url#file://}
    if [ ! -d "$repository_path" ]; then
      printf '%s\n' 'The local release repository does not exist.' >&2
      exit 66
    fi
    ;;
  https://*@*)
    printf '%s\n' 'Repository URL must not contain credentials.' >&2
    exit 65
    ;;
  https://*)
    ;;
  *)
    printf '%s\n' 'Repository URL must be an absolute file or HTTPS URL.' >&2
    exit 65
    ;;
esac

maven_command=${ORCA_RELEASE_MAVEN_COMMAND:-"$RELEASE_ROOT/../../orca_backend/mvnw"}
maven_settings=${ORCA_RELEASE_MAVEN_SETTINGS:-}
if [ -n "$maven_settings" ]; then
  case "$maven_settings" in
    /*) ;;
    *)
      printf '%s\n' 'ORCA_RELEASE_MAVEN_SETTINGS must name an absolute settings.xml file.' >&2
      exit 65
      ;;
  esac
  if [ ! -f "$maven_settings" ]; then
    printf '%s\n' 'The Maven settings file does not exist.' >&2
    exit 66
  fi
fi

verification_root=$(mktemp -d "${TMPDIR:-/tmp}/orca-release-verification.XXXXXX")
case "$verification_root" in
  "${TMPDIR:-/tmp}"/orca-release-verification.*) ;;
  *)
    printf '%s\n' 'Refusing unsafe verification directory.' >&2
    exit 70
    ;;
esac
trap 'rm -rf "$verification_root"' EXIT HUP INT TERM

cp -R "$CONSUMER_FIXTURE" "$verification_root/consumer"
mkdir -p "$verification_root/maven-repository"

set -- \
  -q \
  -f "$verification_root/consumer/pom.xml" \
  -Dmaven.repo.local="$verification_root/maven-repository" \
  -Dorca.version="$release_version" \
  -Dorca.repository.url="$repository_url" \
  test

if [ -n "$maven_settings" ]; then
  set -- -s "$maven_settings" "$@"
fi

if ! "$maven_command" "$@" >"$verification_root/maven-output.log" 2>&1
then
  printf '%s\n' 'Release candidate verification failed; protected Maven output was withheld.' >&2
  exit 1
fi

printf '%s\n' 'Release candidate passed isolated consumer verification.'
