#!/usr/bin/env sh

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
ENV_FILE="$ROOT_DIR/deploy/local/.env.local"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing deploy/local/.env.local. Run ./deploy/local/bin/create-local-env.sh first." >&2
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

: "${ORCA_LOCAL_DB_HOST:?ORCA_LOCAL_DB_HOST is required}"
: "${ORCA_LOCAL_DB_PORT:?ORCA_LOCAL_DB_PORT is required}"
: "${ORCA_LOCAL_DB_NAME:?ORCA_LOCAL_DB_NAME is required}"
: "${ORCA_LOCAL_DB_USERNAME:?ORCA_LOCAL_DB_USERNAME is required}"
: "${ORCA_LOCAL_DB_PASSWORD:?ORCA_LOCAL_DB_PASSWORD is required}"
: "${ORCA_LOCAL_BACKEND_PORT:?ORCA_LOCAL_BACKEND_PORT is required}"

ORCA_LOCAL_DB_MODE="${ORCA_LOCAL_DB_MODE:-compose}"
case "$ORCA_LOCAL_DB_MODE" in
  compose|container|external)
    ;;
  *)
    echo "ORCA_LOCAL_DB_MODE must be compose, container, or external." >&2
    exit 1
    ;;
esac

if [ "$ORCA_LOCAL_DB_MODE" = "container" ]; then
  : "${ORCA_LOCAL_DB_CONTAINER:?ORCA_LOCAL_DB_CONTAINER is required when ORCA_LOCAL_DB_MODE=container}"
fi
