#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
ENV_FILE="$ROOT_DIR/deploy/local/.env.local"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing deploy/local/.env.local. Run ./deploy/local/bin/create-local-env.sh first." >&2
  exit 1
fi

if ! awk '
  BEGIN {
    keys = "ORCA_LOCAL_DB_MODE ORCA_LOCAL_DB_HOST ORCA_LOCAL_DB_PORT"
    keys = keys " ORCA_LOCAL_DB_NAME ORCA_LOCAL_DB_USERNAME"
    keys = keys " ORCA_LOCAL_DB_PASSWORD ORCA_LOCAL_DB_CONTAINER"
    keys = keys " ORCA_LOCAL_BACKEND_PORT ORCA_LOCAL_LOGIN_IDENTIFIER"
    keys = keys " ORCA_LOCAL_LOGIN_PASSWORD"
    split(keys, required)
    for (key_index in required) {
      critical[required[key_index]] = 1
    }
  }
  {
    line = $0
    sub(/^[[:space:]]*export[[:space:]]+/, "", line)
    if (match(line, /^[A-Za-z_][A-Za-z0-9_]*[[:space:]]*=/)) {
      key = substr(line, 1, RLENGTH)
      sub(/[[:space:]]*=.*/, "", key)
      if (critical[key] && ++seen[key] > 1) {
        duplicate[key] = 1
        failed = 1
      }
    }
  }
  END {
    if (failed) {
      for (key in duplicate) {
        print "Duplicate verification-critical environment key: " key > "/dev/stderr"
      }
      exit 1
    }
  }
' "$ENV_FILE"
then
  exit 1
fi

. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

: "${ORCA_LOCAL_LOGIN_IDENTIFIER:?ORCA_LOCAL_LOGIN_IDENTIFIER is required}"
: "${ORCA_LOCAL_LOGIN_PASSWORD:?ORCA_LOCAL_LOGIN_PASSWORD is required}"

require_non_blank() {
  value_name="$1"
  value="$2"
  case "$value" in
    *[![:space:]]*)
      ;;
    *)
      echo "$value_name must not be blank." >&2
      exit 1
      ;;
  esac
}

require_port() {
  value_name="$1"
  value="$2"
  case "$value" in
    ''|*[!0-9]*)
      echo "$value_name must be an integer from 1 through 65535." >&2
      exit 1
      ;;
  esac
  if ! [ "$value" -ge 1 ] 2>/dev/null || ! [ "$value" -le 65535 ] 2>/dev/null; then
    echo "$value_name must be an integer from 1 through 65535." >&2
    exit 1
  fi
}

require_json_serializable() {
  value_name="$1"
  value="$2"
  if printf '%s' "$value" | LC_ALL=C grep -q '[[:cntrl:]]'; then
    echo "$value_name contains a control character that cannot be safely serialized." >&2
    exit 1
  fi
}

json_escape() {
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

require_running_database_container() {
  container_name="$1"
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required for database mode $ORCA_LOCAL_DB_MODE." >&2
    exit 1
  fi
  if ! docker ps \
    --filter "name=^/${container_name}$" \
    --filter "status=running" \
    --format '{{.Names}}' | grep -q "^${container_name}$"
  then
    echo "Selected database container is not running for mode $ORCA_LOCAL_DB_MODE." >&2
    exit 1
  fi
}

require_non_blank ORCA_LOCAL_DB_HOST "$ORCA_LOCAL_DB_HOST"
require_port ORCA_LOCAL_DB_PORT "$ORCA_LOCAL_DB_PORT"
require_non_blank ORCA_LOCAL_DB_NAME "$ORCA_LOCAL_DB_NAME"
require_non_blank ORCA_LOCAL_DB_USERNAME "$ORCA_LOCAL_DB_USERNAME"
require_non_blank ORCA_LOCAL_DB_PASSWORD "$ORCA_LOCAL_DB_PASSWORD"
require_port ORCA_LOCAL_BACKEND_PORT "$ORCA_LOCAL_BACKEND_PORT"
require_non_blank ORCA_LOCAL_LOGIN_IDENTIFIER "$ORCA_LOCAL_LOGIN_IDENTIFIER"
require_non_blank ORCA_LOCAL_LOGIN_PASSWORD "$ORCA_LOCAL_LOGIN_PASSWORD"
require_json_serializable ORCA_LOCAL_LOGIN_IDENTIFIER "$ORCA_LOCAL_LOGIN_IDENTIFIER"
require_json_serializable ORCA_LOCAL_LOGIN_PASSWORD "$ORCA_LOCAL_LOGIN_PASSWORD"

case "$ORCA_LOCAL_DB_MODE" in
  compose)
    require_running_database_container orca-db
    ;;
  container)
    require_non_blank ORCA_LOCAL_DB_CONTAINER "$ORCA_LOCAL_DB_CONTAINER"
    require_running_database_container "$ORCA_LOCAL_DB_CONTAINER"
    ;;
  external)
    ;;
esac

TMP_DIR="$ROOT_DIR/deploy/local/tmp"
mkdir -p "$TMP_DIR"
success_headers="$TMP_DIR/login-success.headers.tmp"
success_body="$TMP_DIR/login-success.body.tmp"
failure_headers="$TMP_DIR/login-failure.headers.tmp"
failure_body="$TMP_DIR/login-failure.body.tmp"

backend_url="http://localhost:$ORCA_LOCAL_BACKEND_PORT"

"$ROOT_DIR/deploy/local/bin/run-local-db-sql.sh" <<SQL >/dev/null
SELECT COUNT(*) FROM auth_login_credentials;
SQL

for table_name in \
  organization_groups \
  group_members \
  group_invitations \
  invitation_index \
  auth_registered_users \
  auth_system_role_assignments \
  auth_provisioning_verification_requests \
  auth_login_credentials \
  auth_authenticated_sessions \
  auth_login_failure_audits \
  reference_core_client_diagnostics
do
  "$ROOT_DIR/deploy/local/bin/run-local-db-sql.sh" <<SQL | grep -q '^1'
SELECT 1 FROM information_schema.tables WHERE table_schema = '$ORCA_LOCAL_DB_NAME' AND table_name = '$table_name';
SQL
done

escaped_login_identifier=$(json_escape "$ORCA_LOCAL_LOGIN_IDENTIFIER")
escaped_login_password=$(json_escape "$ORCA_LOCAL_LOGIN_PASSWORD")
success_request=$(printf \
  '{"loginIdentifier":"%s","password":"%s"}' \
  "$escaped_login_identifier" \
  "$escaped_login_password")
failure_request=$(printf \
  '{"loginIdentifier":"%s","password":"invalid-local-password"}' \
  "$escaped_login_identifier")

success_status=$(curl -sS -o "$success_body" -D "$success_headers" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -X POST "$backend_url/api/auth/login" \
  --data "$success_request")

if [ "$success_status" != "204" ]; then
  echo "Expected successful login status 204, got $success_status." >&2
  exit 1
fi

if ! grep -qi '^Set-Cookie: ORCA_SESSION=' "$success_headers"; then
  echo "Expected successful login to include ORCA_SESSION cookie." >&2
  exit 1
fi

failure_status=$(curl -sS -o "$failure_body" -D "$failure_headers" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -X POST "$backend_url/api/auth/login" \
  --data "$failure_request")

if [ "$failure_status" != "401" ]; then
  echo "Expected failed login status 401, got $failure_status." >&2
  exit 1
fi

if grep -qi '^Set-Cookie: ORCA_SESSION=' "$failure_headers"; then
  echo "Failed login unexpectedly included ORCA_SESSION cookie." >&2
  exit 1
fi

if ! grep -q '"code":"LOGIN_REJECTED"' "$failure_body"; then
  echo "Expected failed login body to include LOGIN_REJECTED." >&2
  exit 1
fi

if ! grep -q '"loginFailureReferenceId":"' "$failure_body"; then
  echo "Expected failed login body to include loginFailureReferenceId." >&2
  exit 1
fi

echo "MariaDB runtime is reachable."
echo "Database mode $ORCA_LOCAL_DB_MODE is available."
echo "Backend local profile can serve login over HTTP."
echo "Flyway-created required tables are present."
echo "Successful login returned 204 with ORCA_SESSION cookie."
echo "Failed login returned LOGIN_REJECTED with loginFailureReferenceId and no session cookie."
echo "Secret values and session cookie values were not printed."
