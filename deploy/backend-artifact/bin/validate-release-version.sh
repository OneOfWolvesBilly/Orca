#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  printf '%s\n' 'Usage: validate-release-version.sh <major.minor.patch>' >&2
  exit 64
fi

release_version=$1

if ! printf '%s\n' "$release_version" | LC_ALL=C grep -Eq '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'; then
  printf '%s\n' 'Release version must be an exact major.minor.patch value.' >&2
  exit 65
fi
