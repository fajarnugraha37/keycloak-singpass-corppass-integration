# SSO Application Stack

A learning playground for single sign-on (SSO) patterns built with Docker, featuring Keycloak, MockPass OIDC provider, and multiple microservices. This project is designed for experimentation and understanding authentication flows in a local development environment.

[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://www.docker.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-25.0.6-red?logo=keycloak)](https://www.keycloak.org/)
[![Node.js](https://img.shields.io/badge/Node.js-18+-green?logo=node.js)](https://nodejs.org/)
[![Nginx](https://img.shields.io/badge/Nginx-Latest-brightgreen?logo=nginx)](https://nginx.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql)](https://www.postgresql.org/)

## 📸 Screenshots

### 🏠 Main Portal Dashboard
The Singapore government-styled eServices portal provides access to both ACEAS and CPDS applications with professional branding and modern UI.

![Main Portal](./docs/images/main-portal.png)

### 🔐 ACEAS Application 
Direct Keycloak authentication with Authorization Code + PKCE flow, featuring real-time authentication status and token management.

![ACEAS Application](./docs/images/aceas-app.png)

### 📊 CPDS Application
Federated authentication through IDS provider that brokers tokens with Keycloak backend, demonstrating token isolation patterns.

![CPDS Application](./docs/images/cpds-app.png)

### 🔑 Keycloak Admin Console
Comprehensive identity management with agency realm configuration, user management, and authentication flow monitoring.

![Keycloak Admin](./docs/images/keycloak-admin.png)

### 🎭 MockPass Authentication
Singapore SingPass simulation providing realistic government authentication experience for testing and development.

![MockPass Authentication](./docs/images/mockpass-auth.png)

### 🚨 Error Pages
Professional error handling with government-styled 404 and 50x pages featuring animations and auto-refresh functionality.

![Error 404](./docs/images/error-404.png)
![Error 5XX](./docs/images/error-50x.png)

## 📖 Table of Contents

- [🏗️ Architecture](#️-architecture)
- [🚀 Quick Start](#-quick-start)
- [🛠️ Development Commands](#️-development-commands)
- [📁 Project Structure](#-project-structure)
- [🔄 Authentication Flow](#-authentication-flow)
- [🐳 Docker Configuration](#-docker-configuration)
- [🔧 Configuration](#-configuration)
- [🔍 API Documentation](#-api-documentation)
- [🚨 Troubleshooting](#-troubleshooting)
- [⚡ Performance Tuning](#-performance-tuning)
- [🧹 Maintenance](#-maintenance)
- [🔒 Security Features](#-security-features)
- [🚀 Deployment](#-deployment)
- [📚 Additional Resources](#-additional-resources)

## 🏗️ Architecture

The stack consists of containerized services orchestrated with Docker Compose:

### Core Services
- **🔐 Keycloak** – Identity provider with custom SPIs and agency realm configuration
- **🎭 MockPass** – Singapore government authentication simulator (SingPass/CorpPass)
- **🔍 IDS** – Node.js OpenID Connect provider for token brokering
- **🌐 Nginx** – High-performance reverse proxy with SSL/TLS support
- **🗃️ PostgreSQL** – Keycloak database with optimized performance settings

### Application Services  
- **📱 ACEAS API** – Sample microservice with Keycloak integration
- **📊 CPDS API** – Sample microservice with IDS token validation
- **🖥️ Web Frontend** – Single-page applications served by Nginx

### Infrastructure Features
- 🔒 **SSL/TLS Support** – Self-signed certificates for development
- 📊 **Health Checks** – Comprehensive service monitoring
- 🎯 **Resource Limits** – Memory and CPU constraints for stability
- 🔄 **Hot Reload** – Development-friendly file watching
- 📝 **Centralized Logging** – Background log collection and viewing

## 🚀 Quick Start

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) and Docker Compose V2
- [Make](https://www.gnu.org/software/make/) (for convenience commands)
- **Host file configuration** (see setup below)

### Host File Configuration

For the application to work correctly, you need to add entries to your system's hosts file to point the required domains to localhost.

#### 🪟 Windows
1. **Open Command Prompt as Administrator**
   - Press `Win + R`, type `cmd`, then press `Ctrl + Shift + Enter`

2. **Edit the hosts file**
   ```cmd
   notepad C:\Windows\System32\drivers\etc\hosts
   ```

3. **Add these lines at the end of the file**
   ```
   127.0.0.1 eservice.localhost
   127.0.0.1 mockpass.localhost
   ```

4. **Save and close the file**

#### 🍎 macOS
1. **Open Terminal**

2. **Edit the hosts file with your preferred editor**
   ```bash
   sudo nano /etc/hosts
   ```
   Or with vim:
   ```bash
   sudo vim /etc/hosts
   ```

3. **Add these lines at the end of the file**
   ```
   127.0.0.1 eservice.localhost
   127.0.0.1 mockpass.localhost
   ```

4. **Save and exit**
   - For nano: `Ctrl + X`, then `Y`, then `Enter`
   - For vim: `:wq` then `Enter`

5. **Flush DNS cache**
   ```bash
   sudo dscacheutil -flushcache
   sudo killall -HUP mDNSResponder
   ```

#### 🐧 Linux
1. **Open Terminal**

2. **Edit the hosts file**
   ```bash
   sudo nano /etc/hosts
   ```
   Or with your preferred editor:
   ```bash
   sudo vim /etc/hosts
   ```

3. **Add these lines at the end of the file**
   ```
   127.0.0.1 eservice.localhost
   127.0.0.1 mockpass.localhost
   ```

4. **Save and exit**
   - For nano: `Ctrl + X`, then `Y`, then `Enter`
   - For vim: `:wq` then `Enter`

5. **Flush DNS cache** (varies by distribution)
   ```bash
   # Ubuntu/Debian
   sudo systemctl restart systemd-resolved
   
   # CentOS/RHEL/Fedora
   sudo systemctl restart NetworkManager
   
   # Or manually flush
   sudo systemctl flush-dns
   ```

#### ✅ Verify Configuration
After updating your hosts file, verify the configuration works:

```bash
# Test eservice.localhost
ping eservice.localhost

# Test mockpass.localhost  
ping mockpass.localhost
```

Both should resolve to `127.0.0.1` (localhost).

**Alternative: Using /etc/hosts managers**
- **Windows**: [HostsMan](http://www.abelhadigital.com/hostsman) or [Hosts File Editor](https://hostsfileeditor.com/)
- **macOS**: [Gas Mask](https://github.com/2ndalpha/gasmask) or [SwitchHosts](https://switchhosts.vercel.app/)
- **Linux**: [SwitchHosts](https://switchhosts.vercel.app/) (cross-platform)

### Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/fajarnugraha37/app-sso.git
   cd app-sso
   ```

2. **Start the stack**
   ```bash
   make up
   ```
   Services start in background with automatic logging to `logs/compose.*.log`

3. **Wait for services to be ready** (optional)
   ```bash
   make wait-healthy
   ```

4. **Check service health**
   ```bash
   make health
   ```

5. **Access applications**
   - 🌐 **Main Portal**: http://eservice.localhost
   - 🔐 **Keycloak Admin**: http://eservice.localhost/auth/admin (admin/admin)  
   - 🎭 **MockPass**: http://mockpass.localhost
   - 📱 **ACEAS App**: http://eservice.localhost/aceas/
   - 📊 **CPDS App**: http://eservice.localhost/cpds/

6. **View logs**
   ```bash
   make logs              # Live logs from all services
   make logs-file         # Background log file
   make tail-keycloak     # Last 50 lines from Keycloak
   ```

7. **Stop the stack**
   ```bash
   make down
   ```

### 🎯 First-Time Setup Checklist

- [ ] Docker and Docker Compose installed
- [ ] Make command available
- [ ] Host file entries added
- [ ] Hosts resolving correctly (ping test)
- [ ] Port 80, 443, 5432, 8080 available
- [ ] At least 4GB RAM available for containers

## 🛠️ Development Commands

### Service Management
```bash
# Start/Stop
make up                 # Start all services (background)
make up-fg              # Start all services (foreground)
make up-logs            # Start services + follow logs
make down               # Stop all services
make restart            # Restart all services

# Individual Services
make re-keycloak        # Rebuild and restart Keycloak
make re-ids             # Rebuild and restart IDS
make shell-keycloak     # Open shell in Keycloak container
```

### Logging & Monitoring
```bash
# Live Logs
make logs               # All services
make log-keycloak       # Individual service logs
make log-mockpass       # MockPass logs
make log-ids            # IDS logs

# Tail Logs (last 50 lines)
make tail-keycloak      # Keycloak recent logs
make tail-db            # Database logs
make tail-web           # Nginx logs

# Background Logging
make logs-start         # Start background logging
make logs-stop          # Stop background logging
make logs-file          # View background log file
```

### Health & Diagnostics
```bash
make health             # Service health status
make status             # Container status
make monitor            # Resource usage
make nginx-test         # Test Nginx configuration
make nginx-reload       # Reload Nginx config
```

### SSL/TLS Management
```bash
make ssl-generate       # Generate self-signed certificates
make ssl-info           # View certificate information
```

### Database Operations
```bash
make db-backup          # Backup Keycloak database
make db-restore         # Restore from backup
make optimize-db        # Optimize database performance
```

### Development Environment
```bash
make dev-up             # Start with development overrides
make dev-debug-keycloak # Start Keycloak with debug (port 8787)
make dirs-create        # Create required directories
```

## 📁 Project Structure

```
app-sso/
├── 🔧 docker-compose.yml           # Main service definitions
├── 🔧 docker-compose.override.yml  # Development overrides
├── 🔧 Makefile                     # Automation commands
├── 📋 README.md                    # This file
│
├── 🔐 keycloak-custom/              # Custom Keycloak build
│   ├── 📄 Dockerfile               # Optimized Keycloak image
│   └── 🔌 spi/                     # Custom SPI extensions
│
├── 🔍 keycloak-import/              # Realm configurations
│   ├── 🌐 realm--agency-realm--export.json
│   └── 👑 realm--master--export.json
│
├── 🎭 mockpass/                     # Singapore auth simulator
│   ├── 📄 Dockerfile
│   ├── 🔧 package.json
│   └── 📚 lib/                     # MockPass libraries
│
├── 🔍 services/                     # Microservices
│   ├── 🔗 ids/                     # OIDC provider
│   ├── 📱 aceas-api/               # ACEAS backend
│   └── 📊 cpds-api/                # CPDS backend
│
├── 🌐 nginx/                        # Reverse proxy
│   ├── 📄 nginx.conf
│   ├── 🔧 common.conf
│   └── 📁 conf.d/                  # Virtual hosts
│
├── 🖥️ web/ & webroot/               # Frontend applications
├── 🔒 ssl/                          # SSL certificates
├── 📝 logs/                         # Application logs
└── 🗃️ data/                         # Persistent data
```

## 🔄 Authentication Flow

### Cross-Application SSO
```mermaid
sequenceDiagram
    participant Browser
    participant ACEAS as ACEAS SPA
    participant CPDS as CPDS SPA  
    participant IDS as IDS Provider
    participant KC as Keycloak
    participant MockPass

    Browser->>ACEAS: 1. Access ACEAS app
    ACEAS->>KC: 2. Redirect to Keycloak
    KC->>MockPass: 3. Federated login
    MockPass->>KC: 4. Authentication response
    KC->>ACEAS: 5. Auth code + session
    ACEAS->>Browser: 6. Logged in to ACEAS

    Browser->>CPDS: 7. Access CPDS app  
    CPDS->>IDS: 8. Redirect to IDS
    IDS->>KC: 9. Check existing session
    KC->>IDS: 10. Reuse session (no login!)
    IDS->>CPDS: 11. CPDS-specific token
    CPDS->>Browser: 12. Logged in to CPDS
```

### Key Benefits
- 🔄 **Single Sign-On**: Login once, access all applications
- 🎭 **Singapore Integration**: MockPass for SingPass/CorpPass simulation  
- 🔒 **Token Isolation**: Application-specific tokens via IDS
- 🧪 **Learning Environment**: Keycloak with custom SPIs and realm management

## � API Documentation

### Core Endpoints

#### 🔐 Keycloak API
```
Base URL: http://eservice.localhost/auth

# Admin API
GET    /auth/admin/                    # Admin console
GET    /auth/realms/agency-realm       # Realm info
POST   /auth/realms/agency-realm/protocol/openid-connect/token
GET    /auth/realms/agency-realm/.well-known/openid-configuration
```

#### 🎭 MockPass API  
```
Base URL: http://mockpass.localhost

# SingPass endpoints
GET    /singpass/v2/.well-known/openid-configuration
POST   /singpass/v2/authorize
POST   /singpass/v2/token
GET    /singpass/v2/userinfo

# CorpPass endpoints
GET    /corppass/v2/.well-known/openid-configuration
POST   /corppass/v2/authorize
POST   /corppass/v2/token
GET    /corppass/v2/userinfo
```

#### 🔗 IDS Provider API
```
Base URL: http://eservice.localhost/ids

# OIDC endpoints
GET    /health                         # Health check
GET    /.well-known/openid-configuration
POST   /oauth/authorize
POST   /oauth/token
GET    /oauth/userinfo
POST   /oauth/logout
```

#### 📱 ACEAS API
```
Base URL: http://eservice.localhost/api/aceas

GET    /health                         # Health check
GET    /protected                      # Protected endpoint (requires Keycloak token)
GET    /user                          # User info
```

#### 📊 CPDS API
```
Base URL: http://eservice.localhost/api/cpds

GET    /health                         # Health check
GET    /protected                      # Protected endpoint (requires IDS token)
GET    /user                          # User info
```

### Testing APIs

#### Quick Health Check
```bash
# Test all health endpoints
curl http://eservice.localhost/api/aceas/health
curl http://eservice.localhost/api/cpds/health
curl http://eservice.localhost/ids/health
curl http://mockpass.localhost/singpass/v2/.well-known/openid-configuration
```

#### Authentication Flow Testing
```bash
# 1. Get Keycloak configuration
curl http://eservice.localhost/auth/realms/agency-realm/.well-known/openid-configuration

# 2. Test MockPass configuration  
curl http://mockpass.localhost/singpass/v2/.well-known/openid-configuration

# 3. Test IDS configuration
curl http://eservice.localhost/ids/.well-known/openid-configuration
```

### Service Architecture
- **Networks**: Isolated networks for security (app_network, db_network)
- **Health Checks**: TCP-based monitoring for all services
- **Resource Limits**: Memory and CPU constraints
- **Volume Management**: Hybrid approach (bind mounts + Docker volumes)
- **Security**: No-new-privileges, tmpfs mounts, readonly filesystems

### Environment Support
- **Development**: Hot reload, debug ports, volume mounts
- **Cross-Platform**: Windows and Unix/Linux support in Makefile

## 🔧 Configuration

### Default Credentials
- **Keycloak Admin**: admin/admin
- **Demo User**: demo/demo123  
- **MockPass User**: Use any valid NRIC/FIN format

### Key Endpoints
- **Main Application**: http://eservice.localhost
- **Keycloak**: http://eservice.localhost/auth/
- **MockPass**: http://mockpass.localhost
- **Health Checks**: Individual service health endpoints
- **Debug Port**: 8787 (Keycloak debug in development)

### SSL Configuration
Self-signed certificates are generated for:
- `eservice.localhost` (main application)
- `mockpass.localhost` (MockPass service)

## 🚨 Troubleshooting

### Common Issues
```bash
# Services unhealthy
make health                    # Check service status
make logs                      # View live logs
make tail-<service>           # Check recent logs

# Keycloak SPI issues  
make re-keycloak              # Rebuild with latest SPI
docker exec kc_agency ls /opt/keycloak/providers/

# Network connectivity
make nginx-test               # Test Nginx config
make nginx-reload             # Reload configuration

# Database issues
make optimize-db              # Optimize performance
make db-backup                # Backup before changes
```

## ⚡ Performance Tuning

### System Requirements

#### Minimum Requirements
- **CPU**: 2 cores
- **RAM**: 4GB
- **Storage**: 10GB free space
- **Network**: 1Gbps localhost

#### Recommended for Development
- **CPU**: 4+ cores  
- **RAM**: 8GB+
- **Storage**: 20GB+ SSD
- **Network**: 10Gbps localhost

### Performance Optimization

#### Container Resource Tuning
```bash
# Monitor resource usage
make monitor

# Check current limits
docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"

# Analyze performance bottlenecks
make analyze-performance
```

#### Database Optimization
```bash
# Optimize PostgreSQL performance
make optimize-db

# Monitor database performance
make tail-db

# Check database statistics
docker exec kc_db_agency psql -U keycloak -d keycloak -c "
SELECT schemaname, tablename, n_tup_ins, n_tup_upd, n_tup_del, n_tup_hot_upd
FROM pg_stat_user_tables 
ORDER BY n_tup_ins DESC LIMIT 10;"
```

#### Keycloak Performance Tuning

**JVM Optimization** (in docker-compose.yml):
```yaml
environment:
  KC_CACHE: ispn
  KC_CACHE_STACK: tcp
  JAVA_OPTS: "-Xms1024m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
```

**Connection Pool Tuning**:
```yaml
environment:
  KC_DB_POOL_INITIAL_SIZE: 5
  KC_DB_POOL_MIN_SIZE: 5  
  KC_DB_POOL_MAX_SIZE: 20
```

#### Nginx Performance Tuning

**Worker Configuration**:
```nginx
worker_processes auto;
worker_connections 1024;
worker_rlimit_nofile 2048;
```

**Caching and Compression**:
```nginx
# Enable gzip compression
gzip on;
gzip_vary on;
gzip_min_length 1024;
gzip_types text/plain text/css application/json application/javascript;

# Browser caching for static assets
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### Performance Monitoring

#### Real-time Monitoring
```bash
# Container resource monitoring
make monitor

# Service health monitoring  
watch -n 5 'make health'

# Log monitoring with filtering
make logs | grep -E "(ERROR|WARN|Exception)"
```

#### Performance Benchmarking
```bash
# Simple load testing with curl
for i in {1..100}; do
  curl -s http://eservice.localhost/api/aceas/health > /dev/null &
done
wait

# Using Apache Bench (if available)
ab -n 1000 -c 10 http://eservice.localhost/

# Using wrk (if available)  
wrk -t12 -c400 -d30s http://eservice.localhost/
```

## 🧹 Maintenance

### Cleanup Commands
```bash
make clean                    # Clean containers and images
make cleanup                  # Comprehensive cleanup + volumes
make prune                    # Remove unused Docker resources
make dirs-clean               # Clean data directories (destructive!)
```

### Backup & Restore
```bash
make backup-all               # Backup database + Keycloak config
make restore-db BACKUP_FILE=backup/db_20231018.sql
```

## 🔒 Security Features

- 🛡️ **Hardened Containers**: Security-focused Docker configuration
- 🔐 **SSL/TLS Ready**: Self-signed certificates for development
- 🎯 **Resource Limits**: Prevent resource exhaustion
- 🔍 **Health Monitoring**: Continuous service health checks  
- 📝 **Audit Logging**: Comprehensive logging for troubleshooting
- 🚫 **Network Isolation**: Segmented networks for database access

## � Deployment

### Development Deployment
```bash
# Standard development setup
make dev-up

# With debug enabled for Keycloak
make dev-debug-keycloak

# With hot reload and file watching
make up-logs
```

### Deployment

#### Prerequisites
- SSL certificates (replace self-signed ones)
- Environment variables configured
- Secrets management setup  
- Database backup strategy
- Monitoring and alerting

#### SSL Setup for Local Development
```bash
# Generate SSL certificates for local testing
make ssl-generate

# Start with SSL enabled
make up

# Verify SSL certificates are working
make health
make ssl-info
```

#### Environment Configuration
```bash
# Create environment file for customization
cp .env.example .env

# Edit development settings
# - Change default passwords
# - Configure database settings
# - Set proper SSL certificates
# - Configure test parameters
```

#### Development Checklist
- [ ] SSL certificates generated for local domains
- [ ] Default passwords changed from demo values
- [ ] Database configured with development settings
- [ ] Host file entries added for local domains
- [ ] Health checks passing for all services
- [ ] Authentication flows tested
- [ ] Local development environment verified

### Container Image Building
```bash
# Build custom images locally
docker build -t sso-keycloak:latest ./keycloak-custom
docker build -t sso-mockpass:latest ./mockpass
docker build -t sso-ids:latest ./services/ids

# Push to registry
docker push your-registry/sso-keycloak:latest
docker push your-registry/sso-mockpass:latest
docker push your-registry/sso-ids:latest
```

### Kubernetes Deployment

#### Basic Kubernetes Manifests
```yaml
# Example: keycloak-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  replicas: 2
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
      - name: keycloak
        image: your-registry/sso-keycloak:latest
        ports:
        - containerPort: 8080
        env:
        - name: KC_DB
          value: postgres
        - name: KC_DB_URL
          value: jdbc:postgresql://postgres:5432/keycloak
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi" 
            cpu: "1000m"
```

### Docker Swarm Deployment
```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.yml sso-stack

# Check service status
docker service ls
```

### Health Monitoring

#### Health Check Endpoints
```bash
# Automated health monitoring
#!/bin/bash
ENDPOINTS=(
  "http://eservice.localhost/api/aceas/health"
  "http://eservice.localhost/api/cpds/health" 
  "http://eservice.localhost/ids/health"
  "http://mockpass.localhost/singpass/v2/.well-known/openid-configuration"
)

for endpoint in "${ENDPOINTS[@]}"; do
  if curl -f -s "$endpoint" > /dev/null; then
    echo "✅ $endpoint - OK"
  else
    echo "❌ $endpoint - FAILED"
  fi
done
```

#### Log Aggregation
```bash
# Ship logs to external system
make logs > /var/log/sso-stack.log

# Or use log drivers
docker-compose --log-driver=syslog up
```

## 📚 Additional Resources

### Documentation Links
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [MockPass Documentation](https://github.com/opengovsg/mockpass)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Nginx Configuration Guide](https://nginx.org/en/docs/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Tutorials and Guides
- [OpenID Connect Specification](https://openid.net/connect/)
- [OAuth 2.0 Security Best Practices](https://tools.ietf.org/html/draft-ietf-oauth-security-topics)
- [Container Security Guide](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)
- [Keycloak Admin Guide](https://www.keycloak.org/docs/latest/server_admin/)

### Community and Support
- [Keycloak Community](https://www.keycloak.org/community)
- [Docker Community](https://www.docker.com/community)
- [Stack Overflow - Keycloak](https://stackoverflow.com/questions/tagged/keycloak)
- [GitHub Issues](https://github.com/fajarnugraha37/app-sso/issues)

## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Follow the existing code style and patterns
4. Test changes thoroughly: `make rebuild && make health`
5. Update documentation for new features
6. Submit a pull request

### Code Style Guidelines
- **Docker**: Use multi-stage builds, minimize layer count
- **JavaScript**: Follow ESLint configuration
- **Shell Scripts**: Use ShellCheck for validation
- **Documentation**: Use clear, concise language with examples

### Testing Your Changes
```bash
# Full rebuild and test
make clean && make rebuild

# Run health checks
make health

# Test all endpoints
make help | grep -E "log-|tail-|re-"

# Performance test
make analyze-performance
```

### Reporting Issues
When reporting issues, please include:
- Operating system and version
- Docker and Docker Compose versions
- Steps to reproduce the issue
- Relevant log output (`make logs`)
- Service status (`make health`)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Keycloak Team](https://www.keycloak.org/community) for the excellent identity management platform
- [Open Government Products](https://github.com/opengovsg) for MockPass
- [Docker Community](https://www.docker.com/community) for containerization excellence
- [Nginx Team](https://nginx.org/) for the high-performance web server

---

> **📝 Note**: This is a learning and development environment designed for experimentation with SSO patterns. Use the self-signed certificates and default configurations only for local testing and educational purposes.

> **🔒 Security**: Remember to change default passwords and review security configurations when adapting this setup. This playground environment uses simplified settings for ease of learning.

---

<div align="center">

**⭐ If this project helped you, please consider giving it a star! ⭐**

[![GitHub stars](https://img.shields.io/github/stars/fajarnugraha37/app-sso?style=social)](https://github.com/fajarnugraha37/app-sso/stargazers)

</div>
