# 🚀 SSO Application - Quick Reference

## Common Commands

### **Windows (PowerShell)**
```powershell
.\make.ps1 help      # Show available commands
.\make.ps1 status    # Check service status
.\make.ps1 up        # Start all services
.\make.ps1 down      # Stop all services
.\make.ps1 logs      # Show logs
```

### **Unix/Linux/macOS (Makefile)**
```bash
make help            # Comprehensive help (80+ commands)
make status          # Service status with health checks
make up              # Start with advanced logging
make down            # Stop all services
make logs            # Show all logs with file output

# Development workflow
make dev             # Start development environment
make re-keycloak     # Rebuild Keycloak service
make log-keycloak    # Show Keycloak logs
make shell-keycloak  # Access Keycloak container

# Database operations
make db-backup       # Create timestamped backup
make db-restore BACKUP_FILE=backup.sql

# Monitoring & health
make health          # Check service health
make monitor         # Resource usage monitoring

# Security & quality
make security-scan   # Container vulnerability scan
make lint            # Code quality checks

# Cleanup
make cleanup         # Comprehensive cleanup
make prune          # Docker resource cleanup
```

## Service List
- **mockpass** (port 3001) - Authentication simulator
- **db** (port 5432) - PostgreSQL database  
- **keycloak** (port 8081) - Identity provider
- **ids** - Identity service
- **aceas-api** - ACEAS API service
- **cpds-api** - CPDS API service
- **web** (ports 80, 443) - Nginx reverse proxy

## Troubleshooting
- **ANSI colors not showing?** → Use `.\make.ps1` on Windows
- **Docker not found?** → Run `make check-deps`
- **Port conflicts?** → Check ports 80, 443, 3001, 5432, 8081
- **Service not healthy?** → Run `make health` then `make log-<service>`

## File Locations
- **Logs**: `./logs/compose.TIMESTAMP.log`
- **Backups**: `./backups/keycloak_backup_TIMESTAMP.sql`
- **Config**: `docker-compose.yml`