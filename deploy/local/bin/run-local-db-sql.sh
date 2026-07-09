#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

if [ "$#" -gt 0 ]; then
  echo "run-local-db-sql.sh reads SQL from stdin and accepts no arguments." >&2
  exit 1
fi

if [ "$ORCA_LOCAL_DB_MODE" = "compose" ]; then
  exec docker exec -i orca-db \
    mariadb -u"$ORCA_LOCAL_DB_USERNAME" -p"$ORCA_LOCAL_DB_PASSWORD" "$ORCA_LOCAL_DB_NAME"
fi

if [ "$ORCA_LOCAL_DB_MODE" = "container" ]; then
  exec docker exec -i "$ORCA_LOCAL_DB_CONTAINER" \
    mariadb -u"$ORCA_LOCAL_DB_USERNAME" -p"$ORCA_LOCAL_DB_PASSWORD" "$ORCA_LOCAL_DB_NAME"
fi

if command -v mariadb >/dev/null 2>&1; then
  exec mariadb \
    --host="$ORCA_LOCAL_DB_HOST" \
    --port="$ORCA_LOCAL_DB_PORT" \
    --user="$ORCA_LOCAL_DB_USERNAME" \
    --password="$ORCA_LOCAL_DB_PASSWORD" \
    "$ORCA_LOCAL_DB_NAME"
fi

if command -v mysql >/dev/null 2>&1; then
  exec mysql \
    --host="$ORCA_LOCAL_DB_HOST" \
    --port="$ORCA_LOCAL_DB_PORT" \
    --user="$ORCA_LOCAL_DB_USERNAME" \
    --password="$ORCA_LOCAL_DB_PASSWORD" \
    "$ORCA_LOCAL_DB_NAME"
fi

echo "ORCA_LOCAL_DB_MODE=external requires mariadb or mysql client tooling on the host." >&2
exit 1
