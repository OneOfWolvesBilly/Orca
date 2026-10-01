#!/usr/bin/env sh
set -eu

SCRIPT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GITHUB_PACKAGES_URL='https://maven.pkg.github.com/OneOfWolvesBilly/Orca'

if [ "$#" -ne 1 ]; then
  printf '%s\n' 'Usage: verify-github-package.sh <version>' >&2
  exit 64
fi

maven_settings=${ORCA_RELEASE_MAVEN_SETTINGS:-}
case "$maven_settings" in
  /*) ;;
  *)
    printf '%s\n' 'ORCA_RELEASE_MAVEN_SETTINGS must name an absolute external settings.xml file.' >&2
    exit 65
    ;;
esac

if [ ! -f "$maven_settings" ]; then
  printf '%s\n' 'The external Maven settings file does not exist.' >&2
  exit 66
fi

python3 "$SCRIPT_ROOT/verify_artifact.py" check-release-manifest "${ORCA_RELEASE_EXPECTED_MANIFEST:-}" "$1"

exec env ORCA_RELEASE_MAVEN_SETTINGS="$maven_settings" \
  sh "$SCRIPT_ROOT/verify-release-candidate.sh" \
  "$1" \
  "$GITHUB_PACKAGES_URL"
