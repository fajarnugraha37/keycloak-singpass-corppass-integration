# 🚀 SSO Application – Quick Reference

## Fast Commands

### Windows (PowerShell)
```powershell
.\make.ps1 help              # List available commands
.\make.ps1 up                # Start the stack (detached)
.\make.ps1 status            # Docker Compose status
.\make.ps1 logs -Service web # Follow a single service
.\make.ps1 restart           # Restart everything
```
> Use GNU Make (WSL/Git Bash) for advanced targets such as database resets, SSL utilities, or per-service rebuilds.

### Unix / Linux / macOS (GNU Make)
```bash
make check-deps              # Verify Docker + Compose
make env-setup               # Create .env from template
make up                      # Start services (writes logs/compose.<ts>.log)
make up-logs                 # Start + follow logs immediately
make down                    # Stop and remove the stack
make dev-up / make prod-up   # Bring up override / prod configs
make logs-tail               # Show the last 100 log lines
```

## Common Targets (GNU Make)

### Lifecycle
- `make env-setup`, `make env-validate` – manage `.env`
- `make up`, `make up-fg`, `make up-logs`, `make down`, `make restart`
- `make dev-up`, `make dev-down`, `make dev-debug-keycloak`
- `make prod-up`, `make prod-deploy`

### Environment & Observability
- `make check-deps`, `make status`, `make health`, `make wait-healthy`
- `make logs`, `make logs-tail`, `make logs-start`, `make logs-file`
- `make monitor`, `make analyze-performance`

### Service-Specific Shortcuts
All services (`mockpass`, `db`, `keycloak`, `ids`, `aceas-api`, `cpds-api`, `web`) support:
- `make re-<service>` – rebuild + restart
- `make restart-<service>` / `make stop-<service>` / `make start-<service>`
- `make log-<service>` / `make tail-<service>` / `make shell-<service>`

### Database & Backups
- `make db-backup`, `make db-restore BACKUP_FILE=...`, `make db-reset`
- `make optimize-db` – run `VACUUM ANALYZE` + `REINDEX`
- `make backup-db`, `make backup-keycloak`, `make backup-all`, `make restore-db`

### Security, Quality & Networking
- `make security-scan`, `make lint`, `make format`
- `make nginx-test`, `make nginx-reload`, `make nginx-status`, `make nginx-logs`, `make nginx-errors`
- `make ssl-generate`, `make ssl-info`

### Cleanup & Recovery
- `make cleanup` – removes containers, images, networks, logs, keycloak-custom data
- `make prune` – Docker system/volume/network prune
- `make dirs-clean`, `make volumes-clean` – destructive data resets
- `make dirs-create` – recreate required host directories

## Service & Port Map
- `mockpass` – SingPass/CorpPass simulator (`mockpass.localhost`, port 3001)
- `db` – PostgreSQL (`localhost:5432`)
- `keycloak` – Keycloak SPI build (`http://eservice.localhost:8081`, admin UI via Nginx proxy)
- `ids` – Token broker
- `aceas-api` – ACEAS sample API
- `cpds-api` – CPDS sample API
- `web` – Nginx/frontend gateway (`http://eservice.localhost`, `https://eservice.localhost`)

## Troubleshooting Cheatsheet
- **Missing dependencies** → `make check-deps`
- **Services stuck in `starting`** → `make wait-healthy` then `make log-<service>`
- **Need persistent logs** → `make logs-start` followed by `make logs-file`
- **Port conflicts** → ensure ports 80/443/3001/5432/8081 are free
- **Health check failing** → `make health`, `make monitor`, `make tail-<service>`
- **Stale volumes/data** → `make db-reset` or `make dirs-clean` (⚠ destructive)

## File & Directory Locations
- Runtime logs: `logs/compose.<timestamp>.log` (created by `make up`/`make logs-start`)
- Legacy dumps: `backups/keycloak_backup_<timestamp>.sql`
- Structured backups: `backup/db_<timestamp>.sql`, `backup/keycloak_<timestamp>/`
- SSL assets: `ssl/certs/*.crt`, `ssl/private/*.key`
- Environment config: `.env` (template: `.env.example`)

Keep this sheet nearby for day-to-day commands; consult `MAKEFILE.md` for the full catalogue and detailed behavior.
