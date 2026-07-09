#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
. "$ROOT_DIR/deploy/local/bin/load-local-env.sh"

cd "$ROOT_DIR/orca_backend"
exec ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local
