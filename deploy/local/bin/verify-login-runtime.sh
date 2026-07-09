#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

: "${ORCA_LOCAL_LOGIN_IDENTIFIER:?ORCA_LOCAL_LOGIN_IDENTIFIER is required}"
: "${ORCA_LOCAL_LOGIN_PASSWORD:?ORCA_LOCAL_LOGIN_PASSWORD is required}"

TMP_DIR="$ROOT_DIR/deploy/local/tmp"
mkdir -p "$TMP_DIR"
success_headers="$TMP_DIR/login-success.headers.tmp"
success_body="$TMP_DIR/login-success.body.tmp"
failure_headers="$TMP_DIR/login-failure.headers.tmp"
failure_body="$TMP_DIR/login-failure.body.tmp"

backend_url="http://localhost:$ORCA_LOCAL_BACKEND_PORT"
frontend_url="http://localhost:${ORCA_LOCAL_FRONTEND_PORT:-5173}"

for container_name in orca-frontend orca-backend orca-db
do
  docker ps --filter "name=^/${container_name}$" --filter "status=running" --format '{{.Names}}' | grep -q "^${container_name}$"
done

frontend_status=$(curl -sS -o "$TMP_DIR/frontend.body.tmp" -w '%{http_code}' "$frontend_url")
if [ "$frontend_status" != "200" ]; then
  echo "Expected frontend status 200, got $frontend_status." >&2
  exit 1
fi

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

success_status=$(curl -sS -o "$success_body" -D "$success_headers" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -X POST "$backend_url/api/auth/login" \
  --data "{\"loginIdentifier\":\"$ORCA_LOCAL_LOGIN_IDENTIFIER\",\"password\":\"$ORCA_LOCAL_LOGIN_PASSWORD\"}")

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
  --data "{\"loginIdentifier\":\"$ORCA_LOCAL_LOGIN_IDENTIFIER\",\"password\":\"invalid-local-password\"}")

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
echo "Frontend, backend, and database containers are running."
echo "Frontend runtime is reachable over HTTP."
echo "Backend local profile can serve login over HTTP."
echo "Flyway-created required tables are present."
echo "Successful login returned 204 with ORCA_SESSION cookie."
echo "Failed login returned LOGIN_REJECTED with loginFailureReferenceId and no session cookie."
echo "Secret values and session cookie values were not printed."
