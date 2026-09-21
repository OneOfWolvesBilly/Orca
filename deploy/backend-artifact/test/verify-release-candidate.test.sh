#!/usr/bin/env sh
set -u

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
VERIFIER="$PROJECT_ROOT/deploy/backend-artifact/bin/verify-release-candidate.sh"
TEST_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/orca-release-candidate-test.XXXXXX")

case "$TEST_ROOT" in
  "${TMPDIR:-/tmp}"/orca-release-candidate-test.*) ;;
  *)
    printf '%s\n' 'Refusing unsafe test directory.' >&2
    exit 1
    ;;
esac

trap 'rm -rf "$TEST_ROOT"' EXIT HUP INT TERM

FAKE_BIN="$TEST_ROOT/fake-bin"
COMMAND_LOG="$TEST_ROOT/maven-command.log"
mkdir -p "$FAKE_BIN"
: > "$COMMAND_LOG"

cat > "$FAKE_BIN/mvn" <<'EOF'
#!/usr/bin/env sh
printf '%s\n' "$*" >> "$TEST_COMMAND_LOG"
if [ -n "${TEST_MAVEN_OUTPUT:-}" ]; then
  printf '%s\n' "$TEST_MAVEN_OUTPUT"
fi
exit "${TEST_MAVEN_STATUS:-0}"
EOF
chmod +x "$FAKE_BIN/mvn"

failures=0
case_status=0
case_output=

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

reset_case() {
  : > "$COMMAND_LOG"
  TEST_MAVEN_STATUS=0
  TEST_MAVEN_OUTPUT=
}

run_verifier() {
  case_status=0
  if [ ! -f "$VERIFIER" ]; then
    case_status=126
    case_output="Missing verifier: ${VERIFIER#$PROJECT_ROOT/}"
    return
  fi
  case_output=$(
    PATH="$FAKE_BIN:$PATH" \
    ORCA_RELEASE_MAVEN_COMMAND=mvn \
    TEST_COMMAND_LOG="$COMMAND_LOG" \
    TEST_MAVEN_STATUS="$TEST_MAVEN_STATUS" \
    TEST_MAVEN_OUTPUT="$TEST_MAVEN_OUTPUT" \
      sh "$VERIFIER" "$@" 2>&1
  ) || case_status=$?
}

assert_safe_output() {
  name="$1"
  for protected_value in \
    'github-test-token' \
    'database-test-password'
  do
    case "$case_output" in
      *"$protected_value"*)
        fail "$name" "Verifier output exposed a protected value."
        return 1
        ;;
    esac
  done
  return 0
}

expect_success() {
  name="$1"
  shift
  run_verifier "$@"
  if [ "$case_status" -eq 126 ]; then
    fail "$name" "$case_output"
    return
  fi
  if [ "$case_status" -ne 0 ]; then
    fail "$name" "Expected success, got exit $case_status.
$case_output"
    return
  fi
  if ! assert_safe_output "$name"; then
    return
  fi
  pass "$name"
}

expect_failure_before_maven() {
  name="$1"
  shift
  run_verifier "$@"
  if [ "$case_status" -eq 126 ]; then
    fail "$name" "$case_output"
    return
  fi
  if [ "$case_status" -eq 0 ]; then
    fail "$name" "Expected a non-zero result."
    return
  fi
  if [ -s "$COMMAND_LOG" ]; then
    fail "$name" "Expected validation before Maven execution."
    return
  fi
  if ! assert_safe_output "$name"; then
    return
  fi
  pass "$name"
}

expect_failure_after_maven() {
  name="$1"
  failure_output="$2"
  reset_case
  TEST_MAVEN_STATUS=1
  TEST_MAVEN_OUTPUT="$failure_output"
  run_verifier "1.2.3" "file://$TEST_ROOT/staging"
  if [ "$case_status" -eq 126 ]; then
    fail "$name" "$case_output"
    return
  fi
  if [ "$case_status" -eq 0 ]; then
    fail "$name" "Expected Maven verification failure to propagate."
    return
  fi
  if [ ! -s "$COMMAND_LOG" ]; then
    fail "$name" "Expected Maven to execute."
    return
  fi
  if ! assert_safe_output "$name"; then
    return
  fi
  pass "$name"
}

reset_case
mkdir -p "$TEST_ROOT/staging"
expect_success \
  "verifies an isolated consumer from staged artifacts" \
  "1.2.3" \
  "file://$TEST_ROOT/staging"

mkdir -p "$TEST_ROOT/maven command"
cp "$FAKE_BIN/mvn" "$TEST_ROOT/maven command/mvn"
case_status=0
case_output=$(
  ORCA_RELEASE_MAVEN_COMMAND="$TEST_ROOT/maven command/mvn" \
  TEST_COMMAND_LOG="$COMMAND_LOG" \
  TEST_MAVEN_STATUS=0 \
  TEST_MAVEN_OUTPUT= \
    sh "$VERIFIER" "1.2.3" "file://$TEST_ROOT/staging" 2>&1
) || case_status=$?
if [ "$case_status" -eq 0 ]; then
  pass "supports an absolute Maven command path containing spaces"
else
  fail "supports an absolute Maven command path containing spaces" "$case_output"
fi

if [ -s "$COMMAND_LOG" ] && \
  grep -Fq -- '-Dorca.version=1.2.3' "$COMMAND_LOG" && \
  grep -Fq -- "-Dorca.repository.url=file://$TEST_ROOT/staging" "$COMMAND_LOG" && \
  grep -Fq -- '-Dmaven.repo.local=' "$COMMAND_LOG"
then
  pass "uses exact coordinates and a clean Maven local repository"
else
  fail "uses exact coordinates and a clean Maven local repository" \
    "Expected isolated Maven arguments were not observed."
fi

reset_case
SETTINGS_FILE="$TEST_ROOT/settings.xml"
: > "$SETTINGS_FILE"
case_status=0
case_output=$(
  PATH="$FAKE_BIN:$PATH" \
  ORCA_RELEASE_MAVEN_COMMAND=mvn \
  ORCA_RELEASE_MAVEN_SETTINGS="$SETTINGS_FILE" \
  TEST_COMMAND_LOG="$COMMAND_LOG" \
  TEST_MAVEN_STATUS=0 \
  TEST_MAVEN_OUTPUT= \
    sh "$VERIFIER" "1.2.3" "https://maven.pkg.github.com/OneOfWolvesBilly/Orca" 2>&1
) || case_status=$?
if [ "$case_status" -eq 0 ] && grep -Fq -- "-s $SETTINGS_FILE" "$COMMAND_LOG"; then
  pass "uses external Maven settings for an authenticated repository"
else
  fail "uses external Maven settings for an authenticated repository" "$case_output"
fi

reset_case
case_status=0
case_output=$(
  PATH="$FAKE_BIN:$PATH" \
  ORCA_RELEASE_MAVEN_COMMAND=mvn \
  ORCA_RELEASE_MAVEN_SETTINGS=relative-settings.xml \
  TEST_COMMAND_LOG="$COMMAND_LOG" \
  TEST_MAVEN_STATUS=0 \
  TEST_MAVEN_OUTPUT= \
    sh "$VERIFIER" "1.2.3" "https://maven.pkg.github.com/OneOfWolvesBilly/Orca" 2>&1
) || case_status=$?
if [ "$case_status" -ne 0 ] && [ ! -s "$COMMAND_LOG" ]; then
  pass "rejects a relative Maven settings path before resolution"
else
  fail "rejects a relative Maven settings path before resolution" "$case_output"
fi

reset_case
case_status=0
case_output=$(
  PATH="$FAKE_BIN:$PATH" \
  ORCA_RELEASE_MAVEN_COMMAND=mvn \
  ORCA_RELEASE_MAVEN_SETTINGS="$TEST_ROOT/missing-settings.xml" \
  TEST_COMMAND_LOG="$COMMAND_LOG" \
  TEST_MAVEN_STATUS=0 \
  TEST_MAVEN_OUTPUT= \
    sh "$VERIFIER" "1.2.3" "https://maven.pkg.github.com/OneOfWolvesBilly/Orca" 2>&1
) || case_status=$?
if [ "$case_status" -ne 0 ] && [ ! -s "$COMMAND_LOG" ]; then
  pass "rejects a missing Maven settings file before resolution"
else
  fail "rejects a missing Maven settings file before resolution" "$case_output"
fi

reset_case
expect_failure_before_maven "rejects an absent repository boundary" "1.2.3"
reset_case
expect_failure_before_maven "rejects a blank repository boundary" "1.2.3" "   "
reset_case
expect_failure_before_maven "rejects a malformed repository boundary" "1.2.3" "not-a-url"
reset_case
expect_failure_before_maven \
  "rejects credentials inside the repository URL" \
  "1.2.3" \
  "https://user:secret@example.invalid/repository"
reset_case
expect_failure_before_maven \
  "rejects duplicate repository inputs" \
  "1.2.3" \
  "file://$TEST_ROOT/staging" \
  "file://$TEST_ROOT/other"

expect_failure_after_maven \
  "fails when the requested artifact is missing" \
  "Could not find artifact io.github.oneofwolvesbilly:orca:jar:1.2.3"
expect_failure_after_maven \
  "fails when the consumer runtime is incompatible" \
  "Unsupported class file major version"
expect_failure_after_maven \
  "fails when dependency versions conflict" \
  "Dependency convergence error"
expect_failure_after_maven \
  "fails safely when repository access is unauthorized" \
  "401 Unauthorized github-test-token"
expect_failure_after_maven \
  "fails when a stale artifact checksum is observed" \
  "Checksum validation failed"
expect_failure_after_maven \
  "fails when Maven exits unexpectedly" \
  "Unexpected transport failure"

reset_case
TEST_MAVEN_STATUS=1
TEST_MAVEN_OUTPUT='github-test-token database-test-password'
run_verifier "1.2.3" "file://$TEST_ROOT/staging"
if [ "$case_status" -eq 126 ]; then
  fail "redacts release and runtime secrets from every result" "$case_output"
elif assert_safe_output "redacts release and runtime secrets from every result"; then
  pass "redacts release and runtime secrets from every result"
fi

if [ "$failures" -ne 0 ]; then
  printf '%s\n' "$failures test(s) failed."
  exit 1
fi

printf '%s\n' 'All staged release candidate verification tests passed.'
