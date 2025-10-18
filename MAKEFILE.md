# SSO Application Management Documentation

## Overview

This project provides comprehensive management tools for the SSO application stack with Docker Compose, featuring both an enhanced Makefile for Unix/Linux/macOS and a PowerShell companion script for Windows environments.

## 🚀 Major Enhancements Made

### 🏗️ **Enhanced Makefile (Unix/Linux/macOS)**
- **80+ professional commands** organized in logical sections
- **Cross-platform compatibility** with proper shell detection
- **Colored output** using ANSI escape sequences for better readability
- **Comprehensive help system** with categorized commands and emojis
- **Automatic dependency checking** and environment validation

### � **PowerShell Companion Script (Windows)**
- **Native Windows PowerShell script** (`make.ps1`) with clean syntax
- **Native PowerShell colors** using `Write-Host -ForegroundColor`
- **Simplified command structure** optimized for Windows environments
- **Error-free execution** with proper PowerShell syntax

## 🛠️ **Core Features**

### **Service Management**
- **Individual service control** (rebuild, logs, shell access)
- **Bulk operations** for all services
- **Health monitoring** with service status checks
- **Resource monitoring** with Docker stats
- **Configuration validation**

### **Database Management**
- **Automated backups** with timestamped files
- **Easy restore** functionality
- **Safe reset** with confirmation prompts

### **Development Tools**
- **Security scanning** with Trivy integration
- **Linting** with hadolint and yamllint
- **Code formatting** with prettier
- **Environment validation** and dependency checking

### **Advanced Logging**
- **Cross-platform logging** (Unix `tee` + Windows `Tee-Object`)
- **Timestamped log files** automatically saved to `./logs/` directory
- **Service-specific logs** for targeted debugging
- **Real-time log following** and tail functionality

## 🚀 **Quick Start**

### **Unix/Linux/macOS (Enhanced Makefile)**

```bash
# Show comprehensive help with all 80+ commands
make help

# Start all services with advanced logging
make up

# Check detailed service status and health
make status
make health

# View logs with various options
make logs              # All services
make log-keycloak      # Specific service
make logs-tail         # Recent logs only

# Individual service management
make re-keycloak       # Rebuild specific service
make shell-keycloak    # Access service container

# Database operations
make db-backup         # Automated backup
make db-restore BACKUP_FILE=backup.sql

# Security and quality
make security-scan     # Container vulnerability scanning
make lint              # Code quality checks

# Environment management
make check-deps        # Verify dependencies
make cleanup           # Comprehensive cleanup
make prune             # Docker resource cleanup
```

### **Windows PowerShell (Companion Script)**

```powershell
# Show help with available commands
.\make.ps1 help

# Service management
.\make.ps1 status      # Clean service status display
.\make.ps1 up          # Start all services in background
.\make.ps1 down        # Stop all services

# Logging (simplified for Windows)
.\make.ps1 logs        # Show all logs
.\make.ps1 logs -Service keycloak  # Service-specific logs

# Note: Advanced features available via main Makefile if using WSL
```

## 📋 **Complete Command Reference**

### **Main Targets (Both Platforms)**

| Command | Makefile | PowerShell | Description |
|---------|----------|------------|-------------|
| help | `make help` | `.\make.ps1 help` | Show available commands |
| status | `make status` | `.\make.ps1 status` | Service status overview |
| up | `make up` | `.\make.ps1 up` | Start all services |
| down | `make down` | `.\make.ps1 down` | Stop all services |
| logs | `make logs` | `.\make.ps1 logs` | Show service logs |

### **Advanced Features (Makefile Only)**

#### **Service-Specific Management**
| Pattern | Example | Description |
|---------|---------|-------------|
| `re-<service>` | `make re-keycloak` | Rebuild specific service |
| `log-<service>` | `make log-keycloak` | Show service logs |
| `shell-<service>` | `make shell-keycloak` | Access service container |

**Available services:** `mockpass`, `db`, `keycloak`, `ids`, `aceas-api`, `cpds-api`, `web`

#### **Build & Development**
| Command | Description | Example |
|---------|-------------|---------|
| `build` | Build all services | `make build` |
| `build-no-cache` | Build without cache | `make build-no-cache` |
| `rebuild` | Full rebuild and restart | `make rebuild` |
| `dev` | Start in development mode | `make dev` |
| `prod` | Start in production mode | `make prod` |

#### **Monitoring & Health**
| Command | Description | Example |
|---------|-------------|---------|
| `health` | Check service health status | `make health` |
| `monitor` | Monitor resource usage | `make monitor` |
| `logs-tail` | Show recent logs only | `make logs-tail` |

#### **Database Operations**
| Command | Description | Example |
|---------|-------------|---------|
| `db-backup` | Create timestamped backup | `make db-backup` |
| `db-restore` | Restore from backup file | `make db-restore BACKUP_FILE=backup.sql` |
| `db-reset` | Reset database (with confirmation) | `make db-reset` |

#### **Security & Quality**
| Command | Description | Example |
|---------|-------------|---------|
| `security-scan` | Run Trivy security scan | `make security-scan` |
| `lint` | Lint Dockerfiles and configs | `make lint` |
| `format` | Format configuration files | `make format` |

#### **Environment & Utilities**
| Command | Description | Example |
|---------|-------------|---------|
| `check-deps` | Verify required dependencies | `make check-deps` |
| `validate` | Validate Docker Compose config | `make validate` |
| `env` | Show environment variables | `make env` |
| `config` | Show Docker Compose config | `make config` |

#### **Cleanup Operations**
| Command | Description | Example |
|---------|-------------|---------|
| `clean` | Clean containers and images | `make clean` |
| `cleanup` | Comprehensive cleanup with volumes | `make cleanup` |
| `prune` | Prune unused Docker resources | `make prune` |

#### **Package Management**
| Command | Description | Example |
|---------|-------------|---------|
| `install` | Pull latest images | `make install` |
| `update` | Update and rebuild | `make update` |
| `upgrade` | Interactive upgrade check | `make upgrade` |

## 🔧 **Platform-Specific Features**

### **Unix/Linux/macOS Makefile**
- **Full feature set** with 80+ commands
- **ANSI color support** for terminals that support it
- **Advanced shell scripting** with error handling
- **Professional DevOps toolkit** for complete lifecycle management

### **Windows PowerShell Script**
- **Simplified command set** focused on core operations
- **Native PowerShell colors** with `Write-Host -ForegroundColor`
- **Windows-optimized** Docker Compose commands
- **Clean syntax** without complex string interpolation

## 📊 **Architecture & Configuration**

### **Services Managed**
- **mockpass**: Authentication simulator (port 3001)
- **db**: PostgreSQL database (port 5432)
- **keycloak**: Identity provider (port 8081)
- **ids**: Identity service (internal)
- **aceas-api**: ACEAS API service (internal)
- **cpds-api**: CPDS API service (internal)
- **web**: Nginx reverse proxy (ports 80, 443)

### **Environment Detection**
- **Automatic OS detection** in Makefile
- **Cross-platform logging** strategies
- **Service dependency management**
- **Health check integration**

## 🚨 **Troubleshooting**

### **Common Issues**

#### **ANSI Color Codes in PowerShell**
**Problem:** Seeing `\033[1m\033[0;34m` instead of colors
**Solution:** Use the PowerShell script instead: `.\make.ps1 help`

#### **Docker Command Not Found**
**Problem:** `docker: command not found`
**Solution:** Run `make check-deps` to verify Docker installation

#### **Permission Denied**
**Problem:** Docker permission errors
**Solution:** Ensure Docker daemon is running and user has proper permissions

#### **Port Conflicts**
**Problem:** Services fail to start due to port conflicts
**Solution:** Check ports 80, 443, 3001, 5432, 8081 are available

### **Debug Commands**

```bash
# Environment validation
make check-deps
make validate
make env

# Service debugging
make status
make health
make log-<service>
make shell-<service>

# Clean restart
make cleanup
make build-no-cache
make up
```

## 🔄 **Development Workflows**

### **Daily Development**
```bash
# Start development environment
make dev

# Check service health
make health

# Rebuild after code changes
make re-keycloak

# View logs for debugging
make log-keycloak

# Access container for investigation
make shell-keycloak
```

### **Production Deployment**
```bash
# Validate configuration
make validate

# Start production environment
make prod

# Monitor resources
make monitor

# Create backup
make db-backup

# Security validation
make security-scan
```

### **Maintenance**
```bash
# Update dependencies
make update

# Clean environment
make cleanup

# Prune unused resources
make prune

# Check for upgrades
make upgrade
```

## 📁 **File Structure**

```
app-sso/
├── Makefile                    # Enhanced Unix/Linux/macOS management
├── make.ps1                    # Windows PowerShell companion
├── MAKEFILE.md                 # This comprehensive documentation
├── docker-compose.yml          # Service definitions
├── logs/                       # Automated log storage
│   └── compose.TIMESTAMP.log   # Timestamped log files
└── services/                   # Individual service directories
    ├── keycloak-custom/
    ├── mockpass/
    └── ...
```

## 🎯 **Legacy Compatibility**

All original Makefile targets are maintained for backward compatibility:
- `run` → `up`
- `run-recreate` → `rebuild`
- `re-kc` → `re-keycloak`
- `log-kc` → `log-keycloak`

## 🤝 **Contributing**

When adding new features:

1. **Makefile**: Add appropriate `.PHONY` declaration and help comment with emoji
2. **PowerShell**: Keep syntax simple and Windows-optimized
3. **Documentation**: Update this file with new commands
4. **Testing**: Test on both Unix and Windows platforms

## 📝 **Dependencies**

### **Required**
- Docker 20.10+
- Docker Compose V2+

### **Optional (Enhanced Features)**
- **hadolint**: Dockerfile linting (`brew install hadolint`)
- **yamllint**: YAML validation (`pip install yamllint`)
- **prettier**: Code formatting (`npm install -g prettier`)

## 🎉 **Summary**

This enhanced management system transforms your basic Docker Compose setup into a **professional-grade DevOps toolkit** with:

- **Complete lifecycle management** for development and production
- **Cross-platform compatibility** for diverse team environments
- **Advanced monitoring and debugging** capabilities
- **Security and quality** integration
- **Automated operations** for database management and cleanup

Whether you're on Unix/Linux/macOS using the full Makefile or Windows using the PowerShell companion, you have access to powerful tools for managing your SSO application stack efficiently and professionally! 🚀

```powershell
# Use PowerShell companion script
.\make.ps1 up
.\make.ps1 status
.\make.ps1 logs -Service keycloak -Follow
```

## Available Targets

### Main Targets

| Command | Description | Example |
|---------|-------------|---------|
| `help` | Show help with all available commands | `make help` |
| `up` | Start all services with logging | `make up` |
| `down` | Stop all services | `make down` |
| `start` | Start existing services (no build) | `make start` |
| `stop` | Stop services (keep containers) | `make stop` |
| `restart` | Restart all services | `make restart` |
| `status` | Show service status | `make status` |
| `health` | Check health status of services | `make health` |

### Build Targets

| Command | Description | Example |
|---------|-------------|---------|
| `build` | Build all services | `make build` |
| `build-no-cache` | Build without cache | `make build-no-cache` |
| `rebuild` | Full rebuild and restart | `make rebuild` |

### Service-Specific Targets

| Pattern | Description | Example |
|---------|-------------|---------|
| `re-<service>` | Rebuild specific service | `make re-keycloak` |
| `log-<service>` | Show logs for service | `make log-keycloak` |
| `shell-<service>` | Open shell in service | `make shell-keycloak` |

Available services: `mockpass`, `db`, `keycloak`, `ids`, `aceas-api`, `cpds-api`, `web`

### Development Environment

| Command | Description | Example |
|---------|-------------|---------|
| `dev` | Start in development mode | `make dev` |
| `prod` | Start in production mode | `make prod` |
| `test` | Run tests (placeholder) | `make test` |

### Logging and Monitoring

| Command | Description | Example |
|---------|-------------|---------|
| `logs` | Show logs from all services | `make logs` |
| `logs-tail` | Show last 100 lines | `make logs-tail` |
| `monitor` | Monitor resource usage | `make monitor` |

### Database Management

| Command | Description | Example |
|---------|-------------|---------|
| `db-backup` | Backup database | `make db-backup` |
| `db-restore` | Restore database | `make db-restore BACKUP_FILE=backup.sql` |
| `db-reset` | Reset database (WARNING: destroys data) | `make db-reset` |

### Cleanup Operations

| Command | Description | Example |
|---------|-------------|---------|
| `clean` | Clean containers and images | `make clean` |
| `cleanup` | Comprehensive cleanup with volumes | `make cleanup` |
| `prune` | Prune unused Docker resources | `make prune` |

### Security and Quality

| Command | Description | Example |
|---------|-------------|---------|
| `security-scan` | Run security scan on images | `make security-scan` |
| `lint` | Lint Dockerfiles and configs | `make lint` |
| `format` | Format configuration files | `make format` |

### Utility Commands

| Command | Description | Example |
|---------|-------------|---------|
| `check-deps` | Check required dependencies | `make check-deps` |
| `env` | Show environment variables | `make env` |
| `config` | Show Docker Compose config | `make config` |
| `validate` | Validate Docker Compose file | `make validate` |
| `install` | Install/update dependencies | `make install` |
| `update` | Update and rebuild | `make update` |
| `upgrade` | Upgrade to latest versions | `make upgrade` |

## Environment Variables

The Makefile automatically detects and configures:

- **OS_TYPE**: Detected operating system (unix/windows)
- **PROJECT_NAME**: Set to `app-sso`
- **COMPOSE_PROJECT_NAME**: Docker Compose project name
- **LOGS_DIR**: Directory for log files (`./logs`)
- **TS**: Timestamp for log file naming

## Configuration

### Services Configuration

The Makefile defines these services:
- **SERVICES**: `mockpass db keycloak ids aceas-api cpds-api web`
- **BUILD_SERVICES**: `mockpass keycloak ids aceas-api cpds-api`

### Logging

- Logs are automatically saved to `./logs/compose.TIMESTAMP.log`
- Cross-platform logging with `tee` (Unix) or `Tee-Object` (Windows)
- Timestamps included in log file names

## Dependencies

### Required
- Docker 20.10+
- Docker Compose V2+

### Optional (for enhanced features)
- **hadolint**: Dockerfile linting (`brew install hadolint`)
- **yamllint**: YAML linting (`pip install yamllint`)
- **prettier**: Configuration formatting (`npm install -g prettier`)
- **trivy**: Security scanning (included in security-scan target)

## Examples

### Development Workflow

```bash
# Initial setup
make check-deps
make build
make up

# Development cycle
make re-keycloak          # Rebuild Keycloak after changes
make log-keycloak         # Check logs
make shell-keycloak       # Debug if needed

# Testing
make health               # Check service health
make monitor              # Monitor resources
```

### Production Deployment

```bash
# Production startup
make prod

# Health monitoring
make status
make health
make logs-tail

# Maintenance
make db-backup
make security-scan
```

### Troubleshooting

```bash
# Check configuration
make validate
make config

# Debug services
make status
make health
make log-<service>
make shell-<service>

# Clean restart
make cleanup
make build-no-cache
make up
```

## Windows PowerShell Companion

The `make.ps1` script provides Windows-specific functionality:

```powershell
# Basic usage
.\make.ps1 up
.\make.ps1 status

# Advanced logging
.\make.ps1 logs -Service keycloak -Follow
.\make.ps1 logs -Tail -Lines 50

# Service management
.\make.ps1 restart
```

## Legacy Compatibility

All original Makefile targets are maintained for backward compatibility:

- `run` → `up`
- `run-recreate` → `rebuild`
- Legacy service targets (`re-kc`, `log-kc`, etc.) are mapped to new names

## Troubleshooting

### Common Issues

1. **Command not found**: Ensure Docker and Docker Compose are installed
2. **Permission denied**: Check Docker daemon is running and user has permissions
3. **Port conflicts**: Check if ports 80, 443, 3001, 5432, 8081 are available
4. **Build failures**: Try `make build-no-cache` or check Dockerfile syntax with `make lint`

### Debug Commands

```bash
# Check environment
make env
make check-deps

# Validate configuration
make validate
make config

# Check service status
make status
make health

# View detailed logs
make logs-tail
make log-<service>
```

## Contributing

When adding new targets:

1. Add appropriate `.PHONY` declaration
2. Include help comment with emoji (`## 🚀 Description`)
3. Use proper color coding for output
4. Test on both Unix and Windows platforms
5. Update this documentation

## Security Considerations

- Database backups contain sensitive data - store securely
- `db-reset` permanently destroys data - use with caution
- Security scanning helps identify vulnerabilities
- Always use `make clean` before production deployments