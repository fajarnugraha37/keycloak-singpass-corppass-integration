# SSO Application Makefile Reference

## Overview

The enhanced `Makefile` in this repository wraps every Docker Compose workflow for the SSO playground: orchestration, database hygiene, Keycloak ops, SSL tooling, and troubleshooting shortcuts. Targets are organized in logical sections, emit color-coded output (where the terminal supports ANSI), and work on macOS/Linux, WSL, or native Windows shells (through the companion `make.ps1` script).

- **Project name**: `app-sso` (used for Compose project/volume prefixes)
- **Default compose file**: `docker-compose.yml` with optional overrides (`docker-compose.override.yml`, `docker-compose.prod.yml`)
- **Services** (`SERVICES` variable): `mockpass`, `db`, `keycloak`, `ids`, `aceas-api`, `cpds-api`, `web`
- **Background logs** live in `./logs/compose.<timestamp>.log`

> 💡 `make` automatically creates the `logs/` directory and timestamped log files on first run.

## Prerequisites

| Requirement | Purpose |
|-------------|---------|
| Docker & Docker Compose v2 (`docker compose` CLI) | Run the stack |
| GNU Make | Execute the Makefile targets |
| Node.js 18+ & npm | Needed for the `web-*` helper targets |
| Optional CLIs | `hadolint`, `yamllint`, `prettier`, `jq`, `openssl`, `aquasec/trivy` (pulled as a container) |

> Windows users can either run the Makefile inside WSL or rely on `make.ps1` for the smaller command set described later.

## Getting Help Quickly

Run `make help` to see a categorized list of targets:

```bash
make help
```

The help output groups commands under “Main Targets”, “Service Management”, and “Development” using emojis that match the descriptions in this document. Because the help parser is emoji-driven, only targets with `##` annotations appear there—use this markdown file to see every available helper.

## Quick Start (macOS/Linux/WSL)

```bash
make check-deps          # Confirm Docker + Compose are available
make env-setup           # Copy .env.example → .env (one-time)
make up                  # Start the entire stack in detached mode
make status && make health
make logs                # Follow all service logs
make down                # Stop everything when finished
```

### Windows PowerShell Basics

On native Windows shells run the companion script instead of GNU Make:

```powershell
.\make.ps1 help
.\make.ps1 up
.\make.ps1 status
.\make.ps1 logs -Service keycloak
```

Advanced targets (e.g., database resets, SSL generation, `re-<service>`) still require GNU Make from WSL or Git Bash.

## Service Matrix & Dynamic Targets

| Service     | Purpose |
|-------------|---------|
| `mockpass`  | SingPass/CorpPass simulator |
| `db`        | PostgreSQL backing store for Keycloak |
| `keycloak`  | Identity provider customized for the playground |
| `ids`       | Token broker / intermediary provider |
| `aceas-api` | Sample ACEAS microservice |
| `cpds-api`  | Sample CPDS microservice |
| `web`       | Portal UI + reverse proxy |

For every service in the table the Makefile generates the following command families:

| Pattern | Example | Description |
|---------|---------|-------------|
| `re-<service>` | `make re-keycloak` | Rebuild + recreate the container with `--no-deps` |
| `restart-<service>` | `make restart-web` | Restart without rebuilding |
| `stop-<service>` | `make stop-ids` | Stop a single container |
| `start-<service>` | `make start-mockpass` | Start a stopped container |
| `log-<service>` | `make log-cpds-api` | Follow live logs for that service |
| `tail-<service>` | `make tail-db` | Show the last 50 log lines |
| `shell-<service>` | `make shell-aceas-api` | Open `/bin/bash` (or `/bin/sh`) inside the running container |

> Use `SERVICES` to confirm the exact service keys. All pattern targets are case-sensitive.

## Command Catalogue

### Environment & Validation

| Command | Purpose | Notes |
|---------|---------|-------|
| `make check-deps` | Confirms `docker` and `docker compose` exist | Fails fast if prerequisites are missing |
| `make env` | Prints current Makefile configuration (`OS_TYPE`, compose files, services) | Helpful when debugging environment differences |
| `make env-setup` | Copies `.env.example` to `.env` if missing | Warns before overwriting |
| `make env-validate` | Ensures `.env` exists and echos the first non-comment entries | Stops with instructions if the file is missing |
| `make config` | Outputs the rendered Compose configuration | Equivalent to `docker compose config` |
| `make validate` | Validates the Compose file | Exits with non‑zero status on invalid configurations |
| `make dirs-create` | Creates data/log/SSL directories (`data/postgres`, `logs/nginx`, `ssl/private`, etc.) | Called automatically by `dev-up`/`prod-up` |

### Lifecycle & Compose Orchestration

| Command | Purpose | Notes |
|---------|---------|-------|
| `make up` | Starts all services detached and spawns a background log follower | Background logs stream to `logs/compose.<timestamp>.log` |
| `make up-fg` | Starts in the foreground while teeing output to the log file | Ideal for CI or first run to watch build output |
| `make up-logs` | Runs `up -d` then follows logs immediately | Stops when you interrupt the log stream |
| `make down` | Stops and removes the Compose project | Uses the default compose file |
| `make start` / `make stop` | Start/stop already-created containers without rebuilding | Leaves containers in place |
| `make restart` | Restarts the entire stack | |
| `make dev` | `docker compose up --build` for quick local hacking | Uses the default compose file only |
| `make prod` | Detached build with the default compose file (without `docker-compose.prod.yml`) | For simple prod-like runs |
| `make test` | Placeholder target emitting a TODO message | Hook your automated tests here later |
| `make run` / `make run-recreate` | Legacy aliases for `up` and `rebuild` | Printed in `make help` |

### Overlay Environments (override/prod files)

| Command | Purpose | Notes |
|---------|---------|-------|
| `make dev-up` | Uses `docker-compose.yml + docker-compose.override.yml` with health checks and warm-up info | Calls `env-validate`, `dirs-create`, waits until all containers report healthy, then prints friendly URLs |
| `make dev-down` / `make dev-restart` | Stop or restart the override stack | |
| `make dev-logs` | Follows logs for the override stack | |
| `make dev-debug-keycloak` | Starts only Keycloak from the override stack with remote debug port 8787 exposed | Attach your IDE for SPI debugging |
| `make prod-up` | Uses `docker-compose.yml + docker-compose.prod.yml` detached | Waits on health and prints HTTPS endpoints |
| `make prod-down`, `make prod-logs`, `make prod-deploy` | Production-oriented stop/log/redeploy helpers | `prod-deploy` = down + up |
| `make wait-healthy` | Shared helper that waits up to 5 minutes for every container to leave `starting/unhealthy` status | Called automatically by `dev-up`/`prod-up`, but you can run it manually |
| `make show-dev-info` / `make show-prod-info` | Echoes URLs, credentials, and debug hints for each environment | Runs after the corresponding `*-up` target |

### Build & Image Management

| Command | Purpose | Notes |
|---------|---------|-------|
| `make build` | Parallel build of all services | Uses Docker Compose build cache |
| `make build-no-cache` | Same as `build` but passes `--no-cache` | Useful after base image upgrades |
| `make rebuild` | Runs `down`, `build`, then `up` | Full recycle |
| `make install` | `docker compose pull` for every service | Ensures you have the latest remote images |
| `make update` | Runs `make install` + `docker compose build --pull` | Refreshes base layers before building |
| `make upgrade` | Iterates through services defined in the compose config and `docker pull`s each tagged image | Great for auditing upstream tags |

### Web Asset Helpers

| Command | Purpose | Notes |
|---------|---------|-------|
| `make web-deps` | `npm install` inside `./web` | Run whenever the frontend package.json changes |
| `make web-build` / `make web-watch` | Build or watch the default frontend bundle | Maps to `npm run build` / `npm run build:watch` |
| `make web-dev` | `npm run dev` for the frontend | Runs the SPA development server |
| `make webkc-build`, `make webkc-watch`, `make webkc-dev` | Same as above but for the Keycloak theme bundle | Uses the `build:kc`/`build:kc:watch` scripts |

> These helpers are not tagged with `##` comments, so they do **not** appear under `make help`, but they are available directly.

### Logging & Monitoring

| Command | Purpose | Notes |
|---------|---------|-------|
| `make logs` | Follow logs for every service | Uses the default compose file |
| `make logs-tail` | Show the last 100 lines across all services | Non-interactive |
| `make logs-file` | `tail -f` the background log file produced by `make up` or `make logs-start` | Encourages running `make up` first |
| `make logs-start` / `make logs-stop` | Start/stop the detached log follower | Uses `nohup` on Unix and `Start-Process` on Windows |
| `make monitor` | Live resource usage via `docker stats` scoped to the project containers | |
| `make analyze-performance` | Prints container CPU/memory summaries, Postgres table activity, and an Nginx access histogram | Requires `logs/nginx/access.log` to exist for the last section |

### Database & Persistence

| Command | Purpose | Notes |
|---------|---------|-------|
| `make db-backup` | Dumps the Keycloak database via `kc_db_agency` container into `./backups/keycloak_backup_<timestamp>.sql` | Uses direct `docker exec`; ensure that container name exists |
| `make db-restore BACKUP_FILE=path.sql` | Restores a dump file by streaming it into the running database container | Requires `BACKUP_FILE` to be set when invoking the target |
| `make db-reset` | Stops the `db` service, removes the `kc_pgdata_agency` volume, and restarts Postgres | **Destructive**—prompts for confirmation |
| `make optimize-db` | Runs `VACUUM ANALYZE` and `REINDEX DATABASE keycloak` inside Postgres | Use after heavy load testing |

### Backups & Disaster Recovery

| Command | Purpose | Notes |
|---------|---------|-------|
| `make backup-db` | Executes `pg_dump` from the Compose-managed `db` service and writes to `backup/db_<timestamp>.sql` | Uses `docker compose exec -T db` |
| `make backup-keycloak` | Copies `data/keycloak` into `backup/keycloak_<timestamp>/` | Requires host access to the data directory |
| `make backup-all` | Runs both `backup-db` and `backup-keycloak` | |
| `make restore-db` | Lists available `backup/db_*.sql` files and interactively restores one | Prompts for a filename |

### Storage & Cleanup

| Command | Purpose | Notes |
|---------|---------|-------|
| `make cleanup` | Stops the stack, removes known containers/images/networks, clears logs, and deletes `keycloak-custom/data` | Hard reset of the local environment |
| `make prune` | Runs `docker system/volume/network prune -f` | Removes dangling Docker resources beyond this project |
| `make dirs-clean` | Deletes the entire `data/` and `logs/` directories after a double-confirm prompt | **Destructive** |
| `make volumes-clean` | Calls `docker volume prune -f` after a confirmation prompt | Removes **all** unused Docker volumes |

### Logging Shortcuts Per Service

Use the dynamic patterns described earlier (`log-<service>`, `tail-<service>`, `shell-<service>`, etc.) to drill into a single container when troubleshooting. Combine them with `logs-start` to capture long‑running sessions while freeing the terminal.

### Nginx Helpers

| Command | Purpose | Notes |
|---------|---------|-------|
| `make nginx-test` | Runs `nginx -t` inside the `web` container | Fails if configuration syntax is invalid |
| `make nginx-reload` | Signals the running Nginx process to reload configs | Equivalent to `nginx -s reload` |
| `make nginx-status` | Hits `http://localhost/nginx-status` | Requires the status endpoint to be exposed |
| `make nginx-logs` | Shows the latest JSON access logs, formatting with `jq` when available | Falls back to plain text if `jq` is missing |
| `make nginx-errors` | Tails `/var/log/nginx/error.log` from the container | Displays a friendly warning if the file is missing |
| `make nginx-config` | Dumps the current Nginx configuration (`nginx -T`) | Useful for verifying the final merged config |

### Security, Linting & Formatting

| Command | Purpose | Notes |
|---------|---------|-------|
| `make security-scan` | Pulls `aquasec/trivy` and scans every built image (`kc_<service>`) | Requires Docker socket access |
| `make lint` | Runs `hadolint` across all Dockerfiles and `yamllint` on `docker-compose.yml` (if the tools are installed) | Prints installation hints when tools are missing |
| `make format` | Formats `docker-compose.yml` with Prettier | Installs instructions shown when Prettier is missing |

### SSL Utilities

| Command | Purpose | Notes |
|---------|---------|-------|
| `make ssl-generate` | Generates self-signed cert/key pairs for `eservice.localhost` and `mockpass.localhost` using the configs in `ssl/*.conf` | Places files under `ssl/certs` and `ssl/private` |
| `make ssl-info` | Displays subject, SAN, and validity information for the generated certificates | Reminds you to run `ssl-generate` if the files are missing |

> Combine `logs-tail`, `tail-<service>`, and `analyze-performance` to capture the exact window around an incident.

### Legacy Compatibility

Older project scripts expect the following legacy targets. They remain wired up, but prefer the modern names above when writing new docs or scripts:

- `run` → `up`
- `run-recreate` → `rebuild`
- `re-kc`, `re-ids`, `re-cpds`, `re-aceas`, `re-web`, `re-mockpass`, `re-db` → service-specific rebuilders
- `log-kc`, `log-ids`, `log-cpds`, `log-aceas`, `log-web`, `log-mockpass`, `log-db` → service log shortcuts

## Windows PowerShell Companion (`make.ps1`)

The root directory contains `make.ps1`, a native PowerShell script mirroring the most common lifecycle actions (`help`, `up`, `down`, `status`, `logs`, `restart`, etc.). Use it when GNU Make is unavailable:

```powershell
.\make.ps1 up            # Start everything detached
.\make.ps1 logs          # Stream logs (or add -Service <name> / -Tail)
.\make.ps1 restart       # Equivalent to make restart
```

For advanced scenarios (per-service rebuilds, SSL generation, DB resets) switch to a shell that supports GNU Make (WSL, Git Bash, or MSYS).

## Troubleshooting Tips

- **Dependencies**: Run `make check-deps` anytime Docker upgrades or the CLI path changes.
- **Configuration drift**: Pair `make config` with `make env` to confirm which compose files or environment settings are being picked up.
- **Health issues**: `make wait-healthy` paired with `make health` pinpoints services stuck in `starting` or `unhealthy` states.
- **Log noise**: Use `make logs-start` to capture the entire session, then inspect with `make logs-file` while restarting services as needed.
- **Database recovery**: Prefer the newer `backup-db` / `restore-db` workflow for consistent dump locations; reserve `db-backup` for quick one‑offs when the legacy container names exist.
- **Stale volumes**: `make db-reset`, `make dirs-clean`, and `make volumes-clean` are destructive—double-check that you have backups before running them.
- **Security scans**: `make security-scan` depends on `aquasec/trivy`. If the scan fails early, delete stale `trivy` images and rerun the command.

Use the tables above as your reference when creating onboarding docs, CI jobs, or shell aliases that wrap this Makefile.
