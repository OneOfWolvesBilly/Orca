#!/usr/bin/env sh
set -u

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
VALIDATOR="$PROJECT_ROOT/deploy/backend-artifact/bin/validate-release-version.sh"

failures=0
case_output=
case_status=0

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

run_validator() {
  case_status=0
  if [ ! -f "$VALIDATOR" ]; then
    case_status=126
    case_output="Missing validator: ${VALIDATOR#$PROJECT_ROOT/}"
    return
  fi
  case_output=$(sh "$VALIDATOR" "$@" 2>&1) || case_status=$?
}

expect_success() {
  name="$1"
  shift
  run_validator "$@"
  if [ "$case_status" -eq 0 ]; then
    pass "$name"
  else
    fail "$name" "Expected success, got exit $case_status.
$case_output"
  fi
}

expect_failure() {
  name="$1"
  shift
  run_validator "$@"
  if [ "$case_status" -eq 126 ]; then
    fail "$name" "$case_output"
  elif [ "$case_status" -ne 0 ]; then
    pass "$name"
  else
    fail "$name" "Expected failure for release version input."
  fi
}

expect_success "accepts an exact semantic release version" "1.2.3"
expect_success "accepts a major-zero semantic release version" "0.1.0"
expect_failure "rejects an absent release version"
expect_failure "rejects a blank release version" "   "
expect_failure "rejects a literal null release version" "null"
expect_failure "rejects a malformed release version" "version-one"
expect_failure "rejects an incomplete semantic version" "1.2"
expect_failure "rejects a snapshot release version" "1.2.3-SNAPSHOT"
expect_failure "rejects a Maven version range" "[1.0.0,2.0.0)"
expect_failure "rejects the LATEST alias" "LATEST"
expect_failure "rejects the RELEASE alias" "RELEASE"
expect_failure "rejects duplicate release-version inputs" "1.2.3" "1.2.3"
expect_failure "rejects an untyped control character" "$(printf '1.2.3\t')"

if [ "$failures" -ne 0 ]; then
  printf '%s\n' "$failures test(s) failed."
  exit 1
fi

printf '%s\n' 'All release version validation tests passed.'
