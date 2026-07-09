#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

cat <<TEXT
Use the backend URL:
  http://localhost:$ORCA_LOCAL_BACKEND_PORT

Successful login template:
  curl -i -X POST http://localhost:$ORCA_LOCAL_BACKEND_PORT/api/auth/login \\
    -H 'Content-Type: application/json' \\
    --data '{"loginIdentifier":"<value from ORCA_LOCAL_LOGIN_IDENTIFIER>","password":"<value from ORCA_LOCAL_LOGIN_PASSWORD>"}'

Failed login template:
  curl -i -X POST http://localhost:$ORCA_LOCAL_BACKEND_PORT/api/auth/login \\
    -H 'Content-Type: application/json' \\
    --data '{"loginIdentifier":"<value from ORCA_LOCAL_LOGIN_IDENTIFIER>","password":"invalid-local-password"}'

Do not commit or paste local secret values.
TEXT
