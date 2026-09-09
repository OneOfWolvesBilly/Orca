#!/usr/bin/env sh
set -u

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
TEST_ROOT=$(mktemp -d \
  "$PROJECT_ROOT/deploy/local/tmp/verify-login-runtime-test.XXXXXX")
trap 'rm -rf "$TEST_ROOT"' EXIT HUP INT TERM

failures=0
case_number=0
case_root=
case_output=
case_status=0
case_log=

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

write_local_env() {
  env_file="$case_root/deploy/local/.env.local"
  {
    printf "ORCA_LOCAL_DB_HOST='%s'\n" "${TEST_DB_HOST:-localhost}"
    printf "ORCA_LOCAL_DB_PORT='%s'\n" "${TEST_DB_PORT:-3306}"
    printf "ORCA_LOCAL_DB_NAME='%s'\n" "${TEST_DB_NAME:-orca}"
    printf "ORCA_LOCAL_DB_USERNAME='%s'\n" "${TEST_DB_USERNAME:-orca}"
    printf "ORCA_LOCAL_DB_PASSWORD='%s'\n" "${TEST_DB_PASSWORD:-local-db-secret}"
    printf "ORCA_LOCAL_DB_MODE='%s'\n" "${TEST_DB_MODE:-compose}"
    if [ -n "${TEST_DB_CONTAINER:-}" ]; then
      printf "ORCA_LOCAL_DB_CONTAINER='%s'\n" "$TEST_DB_CONTAINER"
    fi
    printf "ORCA_LOCAL_BACKEND_PORT='%s'\n" "${TEST_BACKEND_PORT:-8080}"
    printf "ORCA_LOCAL_FRONTEND_PORT='%s'\n" "${TEST_FRONTEND_PORT:-5173}"
    printf "ORCA_LOCAL_LOGIN_IDENTIFIER='%s'\n" "${TEST_LOGIN_IDENTIFIER:-local-login}"
    printf "ORCA_LOCAL_LOGIN_PASSWORD='%s'\n" "${TEST_LOGIN_PASSWORD:-local-login-secret}"
  } > "$env_file"

  if [ -n "${TEST_EXTRA_ENV_LINE:-}" ]; then
    printf '%s\n' "$TEST_EXTRA_ENV_LINE" >> "$env_file"
  fi
}

write_fake_commands() {
  fake_bin="$case_root/fake-bin"
  mkdir -p "$fake_bin"

  cat > "$fake_bin/docker" <<'EOF'
#!/usr/bin/env sh
printf '%s\n' docker >> "$TEST_COMMAND_LOG"
if [ "${TEST_DOCKER_FAIL:-0}" = "1" ]; then
  echo "Docker is unavailable." >&2
  exit 1
fi
printf '%s\n' "${TEST_DOCKER_NAMES:-}"
EOF

  cat > "$fake_bin/curl" <<'EOF'
#!/usr/bin/env sh
printf '%s\n' curl >> "$TEST_COMMAND_LOG"
if [ "${TEST_CURL_FAIL:-0}" = "1" ]; then
  echo "Backend HTTP boundary is unavailable." >&2
  exit 1
fi

output_file=
headers_file=
request_data=
url=

while [ "$#" -gt 0 ]; do
  case "$1" in
    -o|-D|-w|-H|-X)
      option="$1"
      shift
      value="${1:-}"
      case "$option" in
        -o) output_file="$value" ;;
        -D) headers_file="$value" ;;
      esac
      ;;
    --data)
      shift
      request_data="${1:-}"
      ;;
    http://*|https://*)
      url="$1"
      ;;
  esac
  shift
done

case "$url" in
  */api/auth/login)
    ;;
  *)
    if [ -n "$output_file" ]; then
      : > "$output_file"
    fi
    printf '%s' 200
    exit 0
    ;;
esac

case "$request_data" in
  *invalid-local-password*)
    if [ -n "${TEST_EXPECTED_FAILURE_DATA:-}" ] && [ "$request_data" != "$TEST_EXPECTED_FAILURE_DATA" ]; then
      echo "Rejected login request was not safely serialized." >&2
      exit 1
    fi
    if [ -n "$output_file" ]; then
      printf '%s' "${TEST_FAILURE_BODY:-{\"status\":401,\"code\":\"LOGIN_REJECTED\",\"message\":\"Login rejected\",\"loginFailureReferenceId\":\"failure-ref\"}}" > "$output_file"
    fi
    if [ -n "$headers_file" ]; then
      printf '%s\n' 'HTTP/1.1 401 Unauthorized' > "$headers_file"
      if [ "${TEST_FAILURE_COOKIE:-0}" = "1" ]; then
        printf '%s\n' 'Set-Cookie: ORCA_SESSION=opaque-test-value; HttpOnly' >> "$headers_file"
      fi
    fi
    printf '%s' "${TEST_FAILURE_STATUS:-401}"
    ;;
  *)
    if [ -n "${TEST_EXPECTED_SUCCESS_DATA:-}" ] && [ "$request_data" != "$TEST_EXPECTED_SUCCESS_DATA" ]; then
      echo "Login request was not safely serialized." >&2
      exit 1
    fi
    if [ -n "$output_file" ]; then
      : > "$output_file"
    fi
    if [ -n "$headers_file" ]; then
      printf '%s\n' 'HTTP/1.1 204 No Content' > "$headers_file"
      if [ "${TEST_SUCCESS_COOKIE:-1}" = "1" ]; then
        printf '%s\n' 'Set-Cookie: ORCA_SESSION=opaque-test-value; HttpOnly' >> "$headers_file"
      fi
    fi
    printf '%s' "${TEST_SUCCESS_STATUS:-204}"
    ;;
esac
EOF

  chmod +x "$fake_bin/docker" "$fake_bin/curl"
}

write_fake_database_adapter() {
  adapter="$case_root/deploy/local/bin/run-local-db-sql.sh"
  cat > "$adapter" <<'EOF'
#!/usr/bin/env sh
printf '%s\n' database >> "$TEST_COMMAND_LOG"
input=$(cat)
if [ "${TEST_DATABASE_FAIL:-0}" = "1" ]; then
  echo "Selected database boundary is unavailable." >&2
  exit 1
fi
if [ -n "${TEST_DATABASE_MISSING_TABLE:-}" ] &&
  printf '%s' "$input" | grep -Fq "table_name = '$TEST_DATABASE_MISSING_TABLE'"
then
  printf '%s\n' 0
  exit 0
fi
case "$input" in
  *information_schema.tables*) printf '%s\n' 1 ;;
  *) printf '%s\n' 0 ;;
esac
EOF
  chmod +x "$adapter"
}

prepare_case() {
  case_number=$((case_number + 1))
  case_root="$TEST_ROOT/case-$case_number"
  mkdir -p "$case_root/deploy/local/bin"
  cp "$PROJECT_ROOT"/deploy/local/bin/*.sh \
    "$case_root/deploy/local/bin/"
  case_log="$case_root/commands.log"
  : > "$case_log"
  write_local_env
  write_fake_commands
  write_fake_database_adapter
}

run_verifier() {
  case_output=
  case_status=0
  case_output=$(
    PATH="$case_root/fake-bin:$PATH" \
    TEST_COMMAND_LOG="$case_log" \
    TEST_DOCKER_NAMES="${TEST_DOCKER_NAMES:-}" \
    TEST_DOCKER_FAIL="${TEST_DOCKER_FAIL:-0}" \
    TEST_CURL_FAIL="${TEST_CURL_FAIL:-0}" \
    TEST_DATABASE_FAIL="${TEST_DATABASE_FAIL:-0}" \
    TEST_DATABASE_MISSING_TABLE="${TEST_DATABASE_MISSING_TABLE:-}" \
    TEST_EXPECTED_SUCCESS_DATA="${TEST_EXPECTED_SUCCESS_DATA:-}" \
    TEST_EXPECTED_FAILURE_DATA="${TEST_EXPECTED_FAILURE_DATA:-}" \
    TEST_SUCCESS_STATUS="${TEST_SUCCESS_STATUS:-204}" \
    TEST_SUCCESS_COOKIE="${TEST_SUCCESS_COOKIE:-1}" \
    TEST_FAILURE_STATUS="${TEST_FAILURE_STATUS:-401}" \
    TEST_FAILURE_COOKIE="${TEST_FAILURE_COOKIE:-0}" \
    TEST_FAILURE_BODY="${TEST_FAILURE_BODY:-}" \
      sh "$case_root/deploy/local/bin/verify-login-runtime.sh" 2>&1
  ) || case_status=$?
}

assert_safe_output() {
  name="$1"
  for secret in \
    "${TEST_DB_PASSWORD:-local-db-secret}" \
    "${TEST_LOGIN_PASSWORD:-local-login-secret}" \
    opaque-test-value
  do
    case "$case_output" in
      *"$secret"*)
        fail "$name" "Verification output exposed a protected runtime value."
        return 1
        ;;
    esac
  done
  return 0
}

expect_success() {
  name="$1"
  run_verifier
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

expect_failure_before_commands() {
  name="$1"
  run_verifier
  if [ "$case_status" -eq 0 ]; then
    fail "$name" "Expected a non-zero result."
    return
  fi
  if [ -s "$case_log" ]; then
    fail "$name" "Expected validation before external commands, observed:
$(sed 's/^/command: /' "$case_log")"
    return
  fi
  if ! assert_safe_output "$name"; then
    return
  fi
  pass "$name"
}

expect_failure_after_command() {
  name="$1"
  required_command="$2"
  run_verifier
  if [ "$case_status" -eq 0 ]; then
    fail "$name" "Expected a non-zero result."
    return
  fi
  if ! grep -q "^${required_command}$" "$case_log"; then
    fail "$name" "Expected the $required_command capability to be checked."
    return
  fi
  if ! assert_safe_output "$name"; then
    return
  fi
  pass "$name"
}

TEST_DB_MODE=compose
TEST_DB_CONTAINER=
TEST_DOCKER_NAMES=orca-db
TEST_DOCKER_FAIL=0
TEST_CURL_FAIL=0
TEST_DATABASE_FAIL=0
TEST_DATABASE_MISSING_TABLE=
TEST_EXTRA_ENV_LINE=
TEST_EXPECTED_SUCCESS_DATA=
TEST_EXPECTED_FAILURE_DATA=
TEST_SUCCESS_STATUS=204
TEST_SUCCESS_COOKIE=1
TEST_FAILURE_STATUS=401
TEST_FAILURE_COOKIE=0
TEST_FAILURE_BODY='{"status":401,"code":"LOGIN_REJECTED","message":"Login rejected","loginFailureReferenceId":"failure-ref"}'
prepare_case
expect_success "verifies compose login runtime by required capabilities"

TEST_DB_MODE=container
TEST_DB_CONTAINER=existing-mariadb
TEST_DOCKER_NAMES=existing-mariadb
prepare_case
expect_success "verifies selected existing database container mode"

TEST_DB_MODE=external
TEST_DB_CONTAINER=
TEST_DOCKER_NAMES=
TEST_DOCKER_FAIL=1
prepare_case
expect_success "verifies external database mode without Docker"

TEST_DB_MODE=compose
TEST_DOCKER_NAMES=orca-db
TEST_DOCKER_FAIL=0
prepare_case
expect_success "verifies backend availability through HTTP"

TEST_DB_MODE=external
TEST_DOCKER_FAIL=1
TEST_LOGIN_IDENTIFIER='local"user'
TEST_LOGIN_PASSWORD='local\password"value'
TEST_EXPECTED_SUCCESS_DATA='{"loginIdentifier":"local\"user","password":"local\\password\"value"}'
TEST_EXPECTED_FAILURE_DATA='{"loginIdentifier":"local\"user","password":"invalid-local-password"}'
prepare_case
expect_success "safely serializes local login values"

TEST_LOGIN_IDENTIFIER=local-login
TEST_LOGIN_PASSWORD=local-login-secret
TEST_EXPECTED_SUCCESS_DATA=
TEST_EXPECTED_FAILURE_DATA=
TEST_DB_MODE=compose
TEST_DOCKER_FAIL=0
TEST_DOCKER_NAMES=orca-db
TEST_EXTRA_ENV_LINE="ORCA_LOCAL_DB_MODE='external'"
prepare_case
expect_failure_before_commands "rejects duplicate verification-critical environment keys before sourcing"

TEST_EXTRA_ENV_LINE=
TEST_DB_MODE=compose
TEST_DB_PORT=not-a-port
prepare_case
expect_failure_before_commands "rejects untyped database port before command execution"

TEST_DB_PORT=3306
TEST_DB_HOST='   '
prepare_case
expect_failure_before_commands "rejects blank required runtime values before command execution"

TEST_DB_HOST=localhost
TEST_DB_MODE=unsupported
prepare_case
expect_failure_before_commands "rejects unsupported database mode before command execution"

TEST_DB_MODE=null
prepare_case
expect_failure_before_commands "rejects null database mode before command execution"

TEST_DB_MODE=compose
prepare_case
mv "$case_root/deploy/local/.env.local" \
  "$case_root/deploy/local/.env.local.missing"
expect_failure_before_commands "rejects absent local environment before command execution"

TEST_DB_MODE=compose
TEST_DOCKER_FAIL=1
prepare_case
expect_failure_after_command "fails safely when Docker access is unauthorized" docker

TEST_DB_MODE=external
TEST_DOCKER_FAIL=1
TEST_DATABASE_FAIL=1
prepare_case
expect_failure_after_command "fails when the selected external database is stale or unreachable" database

TEST_DATABASE_FAIL=0
TEST_CURL_FAIL=1
prepare_case
expect_failure_after_command "fails when the backend HTTP boundary is unexpectedly unavailable" curl

TEST_CURL_FAIL=0
TEST_DB_MODE=compose
TEST_DOCKER_FAIL=0
TEST_DOCKER_NAMES=orca-db-copy
prepare_case
expect_failure_after_command "rejects a similar database container name" docker

TEST_DB_MODE=external
TEST_DOCKER_FAIL=1
TEST_LOGIN_IDENTIFIER=$(printf 'local\tuser')
prepare_case
expect_failure_before_commands "rejects login values with control characters before command execution"

TEST_LOGIN_IDENTIFIER=local-login
TEST_SUCCESS_STATUS=not-a-status
prepare_case
expect_failure_after_command "rejects an unparseable backend HTTP status" curl

TEST_SUCCESS_STATUS=204
TEST_DATABASE_MISSING_TABLE=auth_login_credentials
prepare_case
expect_failure_after_command "fails when a required Flyway table is missing" database

TEST_DATABASE_MISSING_TABLE=
TEST_SUCCESS_STATUS=401
prepare_case
expect_failure_after_command "fails when the valid local credential is rejected" curl

TEST_SUCCESS_STATUS=204
TEST_SUCCESS_COOKIE=0
prepare_case
expect_failure_after_command "fails when successful login omits the session cookie" curl

TEST_SUCCESS_COOKIE=1
TEST_FAILURE_STATUS=500
prepare_case
expect_failure_after_command "fails when rejected login returns an unexpected status" curl

TEST_FAILURE_STATUS=401
TEST_FAILURE_BODY='{"status":401,"message":"Login rejected","loginFailureReferenceId":"failure-ref"}'
prepare_case
expect_failure_after_command "fails when rejected login omits LOGIN_REJECTED" curl

TEST_FAILURE_BODY='{"status":401,"code":"LOGIN_REJECTED","message":"Login rejected"}'
prepare_case
expect_failure_after_command "fails when rejected login omits the failure reference" curl

TEST_FAILURE_BODY='{"status":401,"code":"LOGIN_REJECTED","message":"Login rejected","loginFailureReferenceId":"failure-ref"}'
TEST_FAILURE_COOKIE=1
prepare_case
expect_failure_after_command "fails when rejected login includes a session cookie" curl

if [ "$failures" -ne 0 ]; then
  printf '%s\n' "$failures test(s) failed."
  exit 1
fi

printf '%s\n' 'All verify-login-runtime contract tests passed.'
