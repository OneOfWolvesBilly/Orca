# Orca Local MariaDB Runtime

This folder contains local-only runtime assets for manually testing Orca login
success and login failure against MariaDB.

## Defaults

Runtime component names:

- frontend component: `orca-frontend`
- backend component: `orca-backend`
- database component: `orca-db`

Local compose files are split by enterprise-style component ownership:

- `compose.yml` aggregates the three local runtime components
- `orca-frontend/compose.yml` owns the frontend runtime component
- `orca-backend/compose.yml` owns the backend runtime component
- `orca-db/compose.yml` owns the database runtime component

Default candidate ports:

- MariaDB host port: `3306`
- backend HTTP port: `8080`
- frontend HTTP port: `5173`

If you choose a different MariaDB port, update `ORCA_LOCAL_DB_PORT` in
`.env.local` before starting MariaDB and before starting the backend. If you
choose a different backend port, update `ORCA_LOCAL_BACKEND_PORT` and use that
port in manual API calls.

## Runtime Modes

The default mode is `compose`. It starts an Orca-owned MariaDB container and
volume so local setup does not touch an existing database.

```sh
ORCA_LOCAL_DB_MODE=compose
```

Advanced users may use an existing MariaDB with:

```sh
ORCA_LOCAL_DB_MODE=container
```

or:

```sh
ORCA_LOCAL_DB_MODE=external
```

Use `container` when the MariaDB is already running as a Docker container. Set
`ORCA_LOCAL_DB_CONTAINER` to the existing container name. The container must
include the `mariadb` CLI, which the official MariaDB image does.

Use `external` when the MariaDB is reachable from the host through host/port.
Host `mariadb` or `mysql` client tooling is required for bootstrap and
verification scripts in external mode.

For both existing-database modes, set `ORCA_LOCAL_DB_HOST`,
`ORCA_LOCAL_DB_PORT`, `ORCA_LOCAL_DB_NAME`, `ORCA_LOCAL_DB_USERNAME`, and
`ORCA_LOCAL_DB_PASSWORD` in `.env.local`. The database should be dedicated to
Orca local runtime because Flyway will create and manage Orca tables in that
database.

## Local Environment Values

Create local runtime values with:

```sh
./deploy/local/bin/create-local-env.sh
```

The script creates `deploy/local/.env.local`. That file is ignored by Git and
must not be committed. It contains real local passwords, selected ports,
runtime mode, and local-only login test values.

Commit `deploy/local/.env.example` only. It documents the expected keys with
placeholder secret values.

### Database Values

| Key | Commit? | Sensitive? | Default / Example | When to change |
| --- | --- | --- | --- | --- |
| `ORCA_LOCAL_DB_HOST` | no, in `.env.local` | no | `localhost` | Change when `ORCA_LOCAL_DB_MODE=external` and MariaDB is on another host. |
| `ORCA_LOCAL_DB_PORT` | no, in `.env.local` | no | `3306` | Change when the host MariaDB port is not `3306`. |
| `ORCA_LOCAL_DB_NAME` | no, in `.env.local` | no | `orca` | Change when using another dedicated local database name. |
| `ORCA_LOCAL_DB_USERNAME` | no, in `.env.local` | maybe | `orca` | Change when using an existing database user. |
| `ORCA_LOCAL_DB_PASSWORD` | no | yes | placeholder in `.env.example` | Required real local DB password. Never commit. |
| `ORCA_LOCAL_DB_ROOT_PASSWORD` | no | yes | placeholder in `.env.example` | Required only for Orca-owned MariaDB initialization. Never commit. |

### Database Mode Values

| Key | Commit? | Sensitive? | Default / Example | When to change |
| --- | --- | --- | --- | --- |
| `ORCA_LOCAL_DB_MODE` | no, in `.env.local` | no | `compose` | Use `compose`, `container`, or `external`. |
| `ORCA_LOCAL_DB_CONTAINER` | no, in `.env.local` | no | existing container name | Required only when `ORCA_LOCAL_DB_MODE=container`. |

Mode meanings:

- `compose`: use the Orca-owned `orca-db` container from local compose.
- `container`: use an already-running MariaDB Docker container.
- `external`: use MariaDB reachable through host and port.

### Runtime Component Values

| Key | Commit? | Sensitive? | Default / Example | When to change |
| --- | --- | --- | --- | --- |
| `ORCA_LOCAL_FRONTEND_COMPONENT` | no, in `.env.local` | no | `orca-frontend` | Change only if component naming changes. |
| `ORCA_LOCAL_BACKEND_COMPONENT` | no, in `.env.local` | no | `orca-backend` | Change only if component naming changes. |
| `ORCA_LOCAL_DB_COMPONENT` | no, in `.env.local` | no | `orca-db` | Change only if component naming changes. |
| `ORCA_LOCAL_BACKEND_PORT` | no, in `.env.local` | no | `8080` | Change when backend host port is occupied. |
| `ORCA_LOCAL_FRONTEND_PORT` | no, in `.env.local` | no | `5173` | Change when frontend host port is occupied. |
| `ORCA_LOCAL_BACKEND_DB_HOST` | no, in `.env.local` | no | `orca-db` | Change when backend container should reach another DB host. |
| `ORCA_LOCAL_BACKEND_DB_PORT` | no, in `.env.local` | no | `3306` | Change when backend container should reach another DB port. |
| `ORCA_FRONTEND_API_PROXY_TARGET` | no, in `.env.local` | no | `http://orca-backend:8080` | Change when frontend should proxy API calls to another backend target. |

### Local-only Login Test Values

| Key | Commit? | Sensitive? | Default / Example | When to change |
| --- | --- | --- | --- | --- |
| `ORCA_LOCAL_LOGIN_IDENTIFIER` | no, in `.env.local` | no | `local-login` | Change only when this unique local login identifier conflicts with another local test credential. |
| `ORCA_LOCAL_LOGIN_PASSWORD` | no | yes | generated local value | Required for manual success login. Never commit. |

`ORCA_LOCAL_LOGIN_IDENTIFIER` is the value typed into the login form. It maps
to `auth_login_credentials.login_identifier`, which is the table primary key.
The bootstrap script updates the existing local credential when the same
identifier is reused; it does not create duplicate login identifiers.

The internal local test user id defaults to `local-login-user` inside the
bootstrap script. It is not needed for normal manual login testing and is not
written by the local environment generator.

The bootstrap script derives the stored password hash at runtime and does not
print or commit the password hash.

### Commit Boundary

Commit these files:

- `deploy/local/.env.example`
- `deploy/local/.gitignore`
- `deploy/local/README.md`
- `deploy/local/compose.yml`
- `deploy/local/orca-frontend/compose.yml`
- `deploy/local/orca-backend/compose.yml`
- `deploy/local/orca-db/compose.yml`
- `deploy/local/bin/*.sh`

Do not commit these files:

- `deploy/local/.env.local`
- `deploy/local/tmp/*`

Check the ignore boundary with:

```sh
git check-ignore -v deploy/local/.env.local deploy/local/tmp/frontend.body.tmp
```

## Start The Full Local Runtime

```sh
./deploy/local/bin/create-local-env.sh
docker compose --env-file deploy/local/.env.local -f deploy/local/compose.yml up -d
```

This starts `orca-frontend`, `orca-backend`, and `orca-db`.

## Start Component-by-component

```sh
docker compose --env-file deploy/local/.env.local -f deploy/local/orca-db/compose.yml up -d
docker compose --env-file deploy/local/.env.local -f deploy/local/orca-backend/compose.yml up -d
docker compose --env-file deploy/local/.env.local -f deploy/local/orca-frontend/compose.yml up -d
```

## Start With Existing MariaDB

```sh
./deploy/local/bin/create-local-env.sh
```

Edit `deploy/local/.env.local`, set `ORCA_LOCAL_DB_MODE=container` or
`ORCA_LOCAL_DB_MODE=external`, and point the database values to your existing
MariaDB. Do not commit that file.

Check the database connection, then start only the backend:

```sh
./deploy/local/bin/check-local-db.sh
docker compose --env-file deploy/local/.env.local -f deploy/local/orca-backend/compose.yml up -d
```

In another terminal, after the backend has started:

```sh
./deploy/local/bin/bootstrap-login-test-data.sh
./deploy/local/bin/verify-login-runtime.sh
```

The scripts do not print passwords, password hashes, database root passwords,
or session cookie values.

## Manual API Checks

Use `./deploy/local/bin/show-local-login-command.sh` to print curl commands
with placeholders. Replace the placeholders with values from your local
environment without committing or pasting the secret values into Git.
