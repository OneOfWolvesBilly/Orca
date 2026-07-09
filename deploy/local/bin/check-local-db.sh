#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

if [ "$ORCA_LOCAL_DB_MODE" = "compose" ]; then
  docker ps --filter "name=^/orca-db$" --format '{{.Names}} {{.Image}} {{.Status}} {{.Ports}}'
fi

if [ "$ORCA_LOCAL_DB_MODE" = "container" ]; then
  docker ps --filter "name=^/${ORCA_LOCAL_DB_CONTAINER}$" --format '{{.Names}} {{.Image}} {{.Status}} {{.Ports}}'
fi

"$ROOT_DIR/deploy/local/bin/run-local-db-sql.sh" <<SQL >/dev/null
SELECT 1;
SQL

echo "Local MariaDB connection is available."
echo "Database mode: $ORCA_LOCAL_DB_MODE"
echo "Secret values were not printed."
