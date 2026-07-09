#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

ORCA_LOCAL_LOGIN_USER_ID="${ORCA_LOCAL_LOGIN_USER_ID:-local-login-user}"
: "${ORCA_LOCAL_LOGIN_IDENTIFIER:?ORCA_LOCAL_LOGIN_IDENTIFIER is required}"
: "${ORCA_LOCAL_LOGIN_PASSWORD:?ORCA_LOCAL_LOGIN_PASSWORD is required}"

case "$ORCA_LOCAL_LOGIN_USER_ID$ORCA_LOCAL_LOGIN_IDENTIFIER$ORCA_LOCAL_LOGIN_PASSWORD" in
  *"'"*|*"
"*)
    echo "Local login values must not contain single quotes or newlines." >&2
    exit 1
    ;;
esac

if ! command -v shasum >/dev/null 2>&1; then
  echo "Missing shasum; cannot create local password hash." >&2
  exit 1
fi

password_hash=$(printf '%s' "$ORCA_LOCAL_LOGIN_PASSWORD" | shasum -a 256 | awk '{print $1}')

"$ROOT_DIR/deploy/local/bin/run-local-db-sql.sh" <<SQL
INSERT INTO auth_registered_users (user_id)
VALUES ('$ORCA_LOCAL_LOGIN_USER_ID')
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id)
VALUES ('$ORCA_LOCAL_LOGIN_IDENTIFIER', '$password_hash', '$ORCA_LOCAL_LOGIN_USER_ID')
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  user_id = VALUES(user_id);
SQL

echo "Local-only login test data is ready."
echo "Secret values and password hashes were not printed."
