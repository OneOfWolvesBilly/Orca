#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
ENV_FILE="$ROOT_DIR/deploy/local/.env.local"

if [ -f "$ENV_FILE" ]; then
  echo "Local env already exists: deploy/local/.env.local"
  echo "No values were changed."
  exit 0
fi

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 32 | tr -d '\n'
  else
    LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48
  fi
}

db_password=$(random_secret)
db_root_password=$(random_secret)
login_password=$(random_secret)

umask 077
{
  printf '%s\n' 'ORCA_LOCAL_DB_HOST=localhost'
  printf '%s\n' 'ORCA_LOCAL_DB_PORT=3306'
  printf '%s\n' 'ORCA_LOCAL_DB_NAME=orca'
  printf '%s\n' 'ORCA_LOCAL_DB_USERNAME=orca'
  printf 'ORCA_LOCAL_DB_PASSWORD=%s\n' "$db_password"
  printf 'ORCA_LOCAL_DB_ROOT_PASSWORD=%s\n' "$db_root_password"
  printf '%s\n' 'ORCA_LOCAL_DB_MODE=compose'
  printf '%s\n' ''
  printf '%s\n' 'ORCA_LOCAL_FRONTEND_COMPONENT=orca-frontend'
  printf '%s\n' 'ORCA_LOCAL_BACKEND_COMPONENT=orca-backend'
  printf '%s\n' 'ORCA_LOCAL_DB_COMPONENT=orca-db'
  printf '%s\n' 'ORCA_LOCAL_BACKEND_PORT=8080'
  printf '%s\n' 'ORCA_LOCAL_FRONTEND_PORT=5173'
  printf '%s\n' 'ORCA_LOCAL_BACKEND_DB_HOST=orca-db'
  printf '%s\n' 'ORCA_LOCAL_BACKEND_DB_PORT=3306'
  printf '%s\n' 'ORCA_FRONTEND_API_PROXY_TARGET=http://orca-backend:8080'
  printf '%s\n' ''
  printf '%s\n' 'ORCA_LOCAL_LOGIN_IDENTIFIER=local-login'
  printf 'ORCA_LOCAL_LOGIN_PASSWORD=%s\n' "$login_password"
} > "$ENV_FILE"

echo "Created deploy/local/.env.local with local-only values."
echo "Secret values were not printed."
