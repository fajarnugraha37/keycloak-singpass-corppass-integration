# =============================================================================
# SSO Application Makefile
# =============================================================================
# Enhanced Makefile for managing the SSO application stack
# Supports both Unix/Linux/macOS and Windows environments

SHELL := /bin/bash
.SHELLFLAGS := -o pipefail -c
.ONESHELL:
.DEFAULT_GOAL := help

# =============================================================================
# Configuration Variables
# =============================================================================

# Project configuration
PROJECT_NAME := app-sso
COMPOSE_FILE := docker-compose.yml
COMPOSE_OVERRIDE_FILE := docker-compose.override.yml
COMPOSE_PROD_FILE := docker-compose.prod.yml
COMPOSE_PROJECT_NAME := $(PROJECT_NAME)
ENV_FILE := .env

# Environment detection
UNAME_S := $(shell uname -s 2>/dev/null || echo "Windows")
ifeq ($(UNAME_S),Windows_NT)
    OS_TYPE := windows
    DATE_CMD := powershell -Command "Get-Date -Format 'yyyyMMddTHHmmssZ'"
    MKDIR_CMD := powershell -Command "New-Item -ItemType Directory -Force -Path"
else
    OS_TYPE := unix
    DATE_CMD := date +%Y%m%dT%H%M%SZ
    MKDIR_CMD := mkdir -p
endif

# Logging configuration
TS := $(shell $(DATE_CMD))
LOGS_DIR := ./logs
LOG_FILE := $(LOGS_DIR)/compose.$(TS).log
LOG_BUILD_FILE := $(LOGS_DIR)/compose.build.$(TS).log
$(shell $(MKDIR_CMD) $(LOGS_DIR))

# Docker configuration
DOCKER_COMPOSE := docker compose
DOCKER_COMPOSE_CMD := $(DOCKER_COMPOSE) -p $(COMPOSE_PROJECT_NAME) -f $(COMPOSE_FILE)

# Services list
SERVICES := mockpass db keycloak ids aceas-api cpds-api web
BUILD_SERVICES := mockpass keycloak ids aceas-api cpds-api

# Colors for output (if supported)
ifeq ($(OS_TYPE),unix)
    RED := \033[0;31m
    GREEN := \033[0;32m
    YELLOW := \033[0;33m
    BLUE := \033[0;34m
    MAGENTA := \033[0;35m
    CYAN := \033[0;36m
    WHITE := \033[0;37m
    RESET := \033[0m
    BOLD := \033[1m
else
    RED := 
    GREEN := 
    YELLOW := 
    BLUE := 
    MAGENTA := 
    CYAN := 
    WHITE := 
    RESET := 
    BOLD := 
endif

# =============================================================================
# Phony Targets Declaration
# =============================================================================

.PHONY: help status health check-deps clean logs monitor
.PHONY: up down restart stop start
.PHONY: build build-no-cache rebuild
.PHONY: $(addprefix re-,$(SERVICES))
.PHONY: $(addprefix log-,$(SERVICES))
.PHONY: $(addprefix shell-,$(SERVICES))
.PHONY: dev prod test
.PHONY: backup restore reset
.PHONY: security-scan lint format
.PHONY: install update upgrade cleanup prune

# =============================================================================
# Help Target
# =============================================================================

help: ## 📋 Show this help message
	@echo "$(BOLD)$(BLUE)SSO Application Management$(RESET)"
	@echo "$(CYAN)=============================$(RESET)"
	@echo ""
	@echo "$(BOLD)Usage:$(RESET) make [target]"
	@echo ""
	@echo "$(BOLD)$(GREEN)Main Targets:$(RESET)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(CYAN)%-20s$(RESET) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "(🚀|📋|🏗️|🔄|🛑|📊|🧹)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Service Management:$(RESET)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(CYAN)%-20s$(RESET) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "(🔧|📝|🔍|🚪)"
	@echo ""
	@echo "$(BOLD)$(MAGENTA)Development:$(RESET)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(CYAN)%-20s$(RESET) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "(🛠️|🔒|📦|⚡)"
	@echo ""
	@echo "$(BOLD)Examples:$(RESET)"
	@echo "  make up              # Start all services"
	@echo "  make dev             # Start in development mode"
	@echo "  make logs            # Show all logs"
	@echo "  make log-keycloak    # Show Keycloak logs"
	@echo "  make re-keycloak     # Rebuild and restart Keycloak"

# =============================================================================
# Environment and Dependency Checks
# =============================================================================

check-deps: ## 📋 Check if required dependencies are installed
	@echo "$(BOLD)$(BLUE)Checking dependencies...$(RESET)"
	@command -v docker >/dev/null 2>&1 || { echo "$(RED)✗ Docker is required but not installed$(RESET)"; exit 1; }
	@docker compose version >/dev/null 2>&1 || { echo "$(RED)✗ Docker Compose is required but not installed$(RESET)"; exit 1; }
	@echo "$(GREEN)✓ All dependencies are installed$(RESET)"

status: ## 📊 Show status of all services
	@echo "$(BOLD)$(BLUE)Service Status:$(RESET)"
	@$(DOCKER_COMPOSE_CMD) ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

health: ## 📊 Check health status of services
	@echo "$(BOLD)$(BLUE)Health Check:$(RESET)"
	@for service in $(SERVICES); do \
		container_name=$$($(DOCKER_COMPOSE_CMD) ps -q $$service 2>/dev/null); \
		if [ -n "$$container_name" ]; then \
			health=$$(docker inspect --format='{{.State.Health.Status}}' $$container_name 2>/dev/null || echo "no-healthcheck"); \
			if [ "$$health" = "healthy" ]; then \
				echo "$(GREEN)✓ $$service: $$health$(RESET)"; \
			elif [ "$$health" = "no-healthcheck" ]; then \
				status=$$(docker inspect --format='{{.State.Status}}' $$container_name 2>/dev/null || echo "unknown"); \
				echo "$(YELLOW)⚪ $$service: $$status (no health check)$(RESET)"; \
			else \
				echo "$(RED)✗ $$service: $$health$(RESET)"; \
			fi; \
		else \
			echo "$(RED)✗ $$service: not running$(RESET)"; \
		fi; \
	done

# =============================================================================
# Main Service Management
# =============================================================================

up: check-deps ## 🚀 Start all services in detached mode (background)
	@echo "$(BOLD)$(GREEN)Starting all services in background...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) --progress plain up -d
	@echo "$(GREEN)✓ All services started in background$(RESET)"
	@echo "$(BLUE)Logs are being written to: $(LOG_FILE)$(RESET)"
	@echo "$(BLUE)Use 'make logs' to view logs or 'make status' to check status$(RESET)"
ifeq ($(OS_TYPE),windows)
	@powershell -Command "Start-Process -NoNewWindow -FilePath 'docker' -ArgumentList 'compose -p $(COMPOSE_PROJECT_NAME) -f $(COMPOSE_FILE) logs -f' -RedirectStandardOutput '$(LOG_FILE)' -RedirectStandardError '$(LOG_FILE)'"
else
	@nohup $(DOCKER_COMPOSE_CMD) logs -f >> "$(LOG_FILE)" 2>&1 &
endif

up-fg: check-deps ## 🚀 Start all services in foreground with logging
	@echo "$(BOLD)$(GREEN)Starting all services in foreground...$(RESET)"
ifeq ($(OS_TYPE),windows)
	@$(DOCKER_COMPOSE_CMD) --progress plain up 2>&1 | powershell -Command "Tee-Object -FilePath '$(LOG_FILE)' -Append"
else
	@$(DOCKER_COMPOSE_CMD) --progress plain up 2>&1 | tee -a "$(LOG_FILE)"; exit $${PIPESTATUS[0]}
endif

up-logs: check-deps ## 🚀 Start all services and follow logs
	@echo "$(BOLD)$(GREEN)Starting all services and following logs...$(RESET)"
ifeq ($(OS_TYPE),windows)
	@$(DOCKER_COMPOSE_CMD) --progress plain up -d && $(DOCKER_COMPOSE_CMD) logs -f 2>&1 | powershell -Command "Tee-Object -FilePath '$(LOG_FILE)' -Append"
else
	@$(DOCKER_COMPOSE_CMD) --progress plain up -d 2>&1 | tee -a "$(LOG_FILE)"; $(DOCKER_COMPOSE_CMD) logs -f
endif

down: ## 🛑 Stop all services
	@echo "$(BOLD)$(RED)Stopping all services...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) down

start: ## 🚀 Start existing services (without building)
	@echo "$(BOLD)$(GREEN)Starting existing services...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) start

stop: ## 🛑 Stop services (without removing containers)
	@echo "$(BOLD)$(YELLOW)Stopping services...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) stop

restart: ## 🔄 Restart all services
	@echo "$(BOLD)$(BLUE)Restarting all services...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) restart

# =============================================================================
# Build Targets
# =============================================================================

build: ## 🏗️ Build all services
	@echo "$(BOLD)$(BLUE)Building all services...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) build --parallel

build-no-cache: ## 🏗️ Build all services without cache
	@echo "$(BOLD)$(BLUE)Building all services (no cache)...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) build --no-cache --parallel

rebuild: down build up ## 🔄 Full rebuild and restart

# =============================================================================
# Development Environment
# =============================================================================

dev: ## 🛠️ Start services in development mode
	@echo "$(BOLD)$(GREEN)Starting development environment...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) up --build

prod: ## ⚡ Start services in production mode
	@echo "$(BOLD)$(GREEN)Starting production environment...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) up -d --build

test: ## 🧪 Run tests (placeholder for future implementation)
	@echo "$(BOLD)$(BLUE)Running tests...$(RESET)"
	@echo "$(YELLOW)Test implementation coming soon...$(RESET)"
	
# =============================================================================
# Individual Service Management
# =============================================================================

# Generate rebuild targets for each service
$(foreach service,$(SERVICES),$(eval re-$(service): ## 🔧 Rebuild and restart $(service); @echo "$(BOLD)$(BLUE)Rebuilding $(service)...$(RESET)"; $(DOCKER_COMPOSE_CMD) up -d --no-deps --build --force-recreate $(service)))

# Generate log targets for each service
$(foreach service,$(SERVICES),$(eval log-$(service): ## 📝 Show logs for $(service); @echo "$(BOLD)$(BLUE)Showing logs for $(service)...$(RESET)"; $(DOCKER_COMPOSE_CMD) logs -f $(service)))

# Generate tail log targets for each service
$(foreach service,$(SERVICES),$(eval tail-$(service): ## 📝 Show last 50 lines of logs for $(service); @echo "$(BOLD)$(BLUE)Showing last 50 lines for $(service)...$(RESET)"; $(DOCKER_COMPOSE_CMD) logs --tail=50 $(service)))

# Generate shell access targets for each service
$(foreach service,$(SERVICES),$(eval shell-$(service): ## 🚪 Open shell in $(service) container; @echo "$(BOLD)$(BLUE)Opening shell in $(service)...$(RESET)"; docker exec -it $$($(DOCKER_COMPOSE_CMD) ps -q $(service)) /bin/bash || docker exec -it $$($(DOCKER_COMPOSE_CMD) ps -q $(service)) /bin/sh))

# =============================================================================
# Logging and Monitoring
# =============================================================================

logs: ## 📝 Show logs from all services
	@echo "$(BOLD)$(BLUE)Showing logs from all services...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) logs -f

logs-tail: ## 📝 Show last 100 lines of logs from all services
	@echo "$(BOLD)$(BLUE)Showing recent logs...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) logs --tail=100

logs-file: ## 📝 Show logs from background log file
	@echo "$(BOLD)$(BLUE)Showing logs from file: $(LOG_FILE)$(RESET)"
	@if [ -f "$(LOG_FILE)" ]; then \
		tail -f "$(LOG_FILE)"; \
	else \
		echo "$(YELLOW)⚠️  Log file not found. Start services with 'make up' first.$(RESET)"; \
	fi

logs-start: ## 📝 Start background logging process
	@echo "$(BOLD)$(BLUE)Starting background logging to: $(LOG_FILE)$(RESET)"
ifeq ($(OS_TYPE),windows)
	@powershell -Command "Start-Process -NoNewWindow -FilePath 'docker' -ArgumentList 'compose -p $(COMPOSE_PROJECT_NAME) -f $(COMPOSE_FILE) logs -f' -RedirectStandardOutput '$(LOG_FILE)' -RedirectStandardError '$(LOG_FILE)'"
else
	@nohup $(DOCKER_COMPOSE_CMD) logs -f >> "$(LOG_FILE)" 2>&1 &
endif
	@echo "$(GREEN)✓ Background logging started$(RESET)"
	@echo "$(BLUE)Use 'make logs-file' to view logs or 'make logs-stop' to stop logging$(RESET)"

logs-stop: ## 📝 Stop background logging process
	@echo "$(BOLD)$(BLUE)Stopping background logging...$(RESET)"
ifeq ($(OS_TYPE),windows)
	@powershell -Command "Get-Process | Where-Object {$$_.ProcessName -eq 'docker' -and $$_.CommandLine -like '*logs -f*'} | Stop-Process -Force" || echo "$(YELLOW)No background logging process found$(RESET)"
else
	@pkill -f "docker.*logs.*-f" || echo "$(YELLOW)No background logging process found$(RESET)"
endif
	@echo "$(GREEN)✓ Background logging stopped$(RESET)"

monitor: ## 📊 Monitor resource usage of containers
	@echo "$(BOLD)$(BLUE)Monitoring container resources...$(RESET)"
	@docker stats $$($(DOCKER_COMPOSE_CMD) ps -q)

# =============================================================================
# Database Management
# =============================================================================

db-backup: ## 💾 Backup database
	@echo "$(BOLD)$(BLUE)Creating database backup...$(RESET)"
	@mkdir -p ./backups
	@docker exec kc_db_agency pg_dump -U keycloak keycloak > ./backups/keycloak_backup_$(TS).sql
	@echo "$(GREEN)✓ Database backup created: ./backups/keycloak_backup_$(TS).sql$(RESET)"

db-restore: ## 💾 Restore database (requires BACKUP_FILE variable)
	@if [ -z "$(BACKUP_FILE)" ]; then \
		echo "$(RED)Error: Please specify BACKUP_FILE=path/to/backup.sql$(RESET)"; \
		exit 1; \
	fi
	@echo "$(BOLD)$(BLUE)Restoring database from $(BACKUP_FILE)...$(RESET)"
	@docker exec -i kc_db_agency psql -U keycloak -d keycloak < $(BACKUP_FILE)
	@echo "$(GREEN)✓ Database restored$(RESET)"

db-reset: ## 💾 Reset database (WARNING: destroys all data)
	@echo "$(BOLD)$(RED)WARNING: This will destroy all database data!$(RESET)"
	@read -p "Are you sure? (y/N): " confirm && [ "$$confirm" = "y" ] || exit 1
	@$(DOCKER_COMPOSE_CMD) stop db
	@docker volume rm $(COMPOSE_PROJECT_NAME)_kc_pgdata_agency || true
	@$(DOCKER_COMPOSE_CMD) up -d db
	@echo "$(GREEN)✓ Database reset complete$(RESET)"

# =============================================================================
# Cleanup Operations
# =============================================================================

clean: ## 🧹 Clean up containers and images
	@echo "$(BOLD)$(YELLOW)Cleaning up containers and images...$(RESET)"
	-@$(DOCKER_COMPOSE_CMD) down
	-@docker container rm ids_op cpds_api aceas_api kc_agency kc_db_agency mockpass web 2>/dev/null || true
	-@docker image rm kc_ids kc_cpds_api kc_aceas_api kc_agency kc_keycloak kc_mockpass 2>/dev/null || true

cleanup: clean ## 🧹 Comprehensive cleanup including volumes
	@echo "$(BOLD)$(YELLOW)Performing comprehensive cleanup...$(RESET)"
	-@docker volume rm $(COMPOSE_PROJECT_NAME)_ids_data $(COMPOSE_PROJECT_NAME)_kc_pgdata_agency 2>/dev/null || true
	@echo "$(GREEN)✓ Cleanup complete$(RESET)"

prune: ## 🧹 Prune unused Docker resources
	@echo "$(BOLD)$(YELLOW)Pruning unused Docker resources...$(RESET)"
	@docker system prune -f
	@docker volume prune -f
	@docker network prune -f
	@echo "$(GREEN)✓ Docker prune complete$(RESET)"

# =============================================================================
# Security and Quality
# =============================================================================

security-scan: ## 🔒 Run security scan on images
	@echo "$(BOLD)$(BLUE)Running security scans...$(RESET)"
	@for image in $(BUILD_SERVICES); do \
		echo "$(CYAN)Scanning kc_$$image...$(RESET)"; \
		docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
			aquasec/trivy:latest image kc_$$image || true; \
	done

lint: ## 🔍 Lint Dockerfiles and configuration files
	@echo "$(BOLD)$(BLUE)Linting files...$(RESET)"
	@if command -v hadolint >/dev/null 2>&1; then \
		find . -name "Dockerfile*" -exec hadolint {} \; ; \
	else \
		echo "$(YELLOW)⚠️  hadolint not installed. Install with: brew install hadolint$(RESET)"; \
	fi
	@if command -v yamllint >/dev/null 2>&1; then \
		yamllint docker-compose.yml || true; \
	else \
		echo "$(YELLOW)⚠️  yamllint not installed. Install with: pip install yamllint$(RESET)"; \
	fi

format: ## 🔍 Format configuration files
	@echo "$(BOLD)$(BLUE)Formatting files...$(RESET)"
	@if command -v prettier >/dev/null 2>&1; then \
		prettier --write docker-compose.yml || true; \
	else \
		echo "$(YELLOW)⚠️  prettier not installed. Install with: npm install -g prettier$(RESET)"; \
	fi

# =============================================================================
# Nginx Management
# =============================================================================

nginx-test: ## 🔍 Test nginx configuration
	@echo "$(BOLD)$(BLUE)Testing nginx configuration...$(RESET)"
	@docker exec web nginx -t && echo "$(GREEN)✓ Nginx configuration is valid$(RESET)" || echo "$(RED)✗ Nginx configuration has errors$(RESET)"

nginx-reload: ## 🔄 Reload nginx configuration
	@echo "$(BOLD)$(BLUE)Reloading nginx configuration...$(RESET)"
	@docker exec web nginx -s reload && echo "$(GREEN)✓ Nginx configuration reloaded$(RESET)" || echo "$(RED)✗ Failed to reload nginx$(RESET)"

nginx-status: ## 📊 Show nginx status
	@echo "$(BOLD)$(BLUE)Nginx status:$(RESET)"
	@curl -s http://localhost/nginx-status 2>/dev/null || echo "$(YELLOW)⚠️  Nginx status endpoint not accessible$(RESET)"

nginx-logs: ## 📝 Show nginx access logs in JSON format
	@echo "$(BOLD)$(BLUE)Nginx access logs:$(RESET)"
	@$(DOCKER_COMPOSE_CMD) logs web | tail -20 | grep -E '\{.*\}' | jq . 2>/dev/null || $(DOCKER_COMPOSE_CMD) logs web | tail -20

nginx-errors: ## 📝 Show nginx error logs
	@echo "$(BOLD)$(BLUE)Nginx error logs:$(RESET)"
	@docker exec web cat /var/log/nginx/error.log 2>/dev/null | tail -20 || echo "$(YELLOW)⚠️  No error log found$(RESET)"

nginx-config: ## 🔍 Show nginx configuration
	@echo "$(BOLD)$(BLUE)Current nginx configuration:$(RESET)"
	@docker exec web nginx -T 2>/dev/null | head -50 || echo "$(RED)✗ Cannot access nginx configuration$(RESET)"

# =============================================================================
# Package Management
# =============================================================================

install: ## 📦 Install/update dependencies
	@echo "$(BOLD)$(BLUE)Pulling latest images...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) pull

update: install ## 📦 Update all dependencies and rebuild
	@echo "$(BOLD)$(BLUE)Updating and rebuilding...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) build --pull

upgrade: ## 📦 Upgrade to latest versions (interactive)
	@echo "$(BOLD)$(BLUE)Checking for image updates...$(RESET)"
	@for service in $(SERVICES); do \
		current_image=$$($(DOCKER_COMPOSE_CMD) config | grep -A5 "$$service:" | grep "image:" | awk '{print $$2}' | head -1); \
		if [ -n "$$current_image" ]; then \
			echo "$(CYAN)Checking $$service ($$current_image)...$(RESET)"; \
			docker pull $$current_image || true; \
		fi; \
	done

# =============================================================================
# Utility Functions
# =============================================================================

env: ## 🔍 Show environment variables
	@echo "$(BOLD)$(BLUE)Environment Configuration:$(RESET)"
	@echo "Project: $(COMPOSE_PROJECT_NAME)"
	@echo "OS Type: $(OS_TYPE)"
	@echo "Compose File: $(COMPOSE_FILE)"
	@echo "Log File: $(LOG_FILE)"
	@echo "Services: $(SERVICES)"

config: ## 🔍 Show Docker Compose configuration
	@echo "$(BOLD)$(BLUE)Docker Compose Configuration:$(RESET)"
	@$(DOCKER_COMPOSE_CMD) config

validate: ## 🔍 Validate Docker Compose file
	@echo "$(BOLD)$(BLUE)Validating Docker Compose file...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) config -q && echo "$(GREEN)✓ Configuration is valid$(RESET)" || echo "$(RED)✗ Configuration has errors$(RESET)"

# =============================================================================
# Legacy Compatibility Targets
# =============================================================================

run: up ## 🚀 Legacy: Start all services (alias for 'up')
run-recreate: rebuild ## 🔄 Legacy: Rebuild and restart (alias for 'rebuild')

# Individual legacy targets maintained for compatibility
re-ids: re-ids ## 🔧 Legacy: Rebuild IDS service
re-cpds: re-cpds-api ## 🔧 Legacy: Rebuild CPDS API service  
re-aceas: re-aceas-api ## 🔧 Legacy: Rebuild ACEAS API service
re-web: ## 🔧 Legacy: Rebuild web service
	@echo "$(BOLD)$(BLUE)Rebuilding web service...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) up -d --no-deps --build --force-recreate web
re-mockpass: re-mockpass ## 🔧 Legacy: Rebuild mockpass service
re-kc: re-keycloak ## 🔧 Legacy: Rebuild Keycloak service
re-db: re-db ## 🔧 Legacy: Rebuild database service

log-ids: log-ids ## 📝 Legacy: Show IDS logs
log-cpds: log-cpds-api ## 📝 Legacy: Show CPDS API logs
log-aceas: log-aceas-api ## 📝 Legacy: Show ACEAS API logs
log-web: ## 📝 Legacy: Show web logs
	@echo "$(BOLD)$(BLUE)Showing logs for web...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) logs -f web
log-mockpass: log-mockpass ## 📝 Legacy: Show mockpass logs
log-kc: log-keycloak ## 📝 Legacy: Show Keycloak logs
log-db: log-db ## 📝 Legacy: Show database logs

# =============================================================================
# Environment Management Commands
# =============================================================================

env-setup: ## 🔧 Setup environment file from template
	@echo "$(BOLD)$(BLUE)Setting up environment configuration...$(RESET)"
	@if [ ! -f "$(ENV_FILE)" ]; then \
		cp .env.example $(ENV_FILE); \
		echo "$(GREEN)✓ Created $(ENV_FILE) from template$(RESET)"; \
		echo "$(YELLOW)⚠ Please review and customize $(ENV_FILE) before starting services$(RESET)"; \
	else \
		echo "$(YELLOW)⚠ $(ENV_FILE) already exists$(RESET)"; \
	fi

env-validate: ## 🔍 Validate environment configuration
	@echo "$(BOLD)$(BLUE)Validating environment configuration...$(RESET)"
	@if [ -f "$(ENV_FILE)" ]; then \
		echo "$(GREEN)✓ $(ENV_FILE) exists$(RESET)"; \
		echo "$(BLUE)Environment variables:$(RESET)"; \
		grep -v '^#' $(ENV_FILE) | grep -v '^$$' | head -10; \
	else \
		echo "$(RED)✗ $(ENV_FILE) not found$(RESET)"; \
		echo "$(YELLOW)Run 'make env-setup' to create it$(RESET)"; \
		exit 1; \
	fi

# =============================================================================
# Development Environment Commands
# =============================================================================

dev-up: env-validate dirs-create ## 🚀 Start development environment with override
	@echo "$(BOLD)$(BLUE)Starting development environment...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_OVERRIDE_FILE) --progress plain up -d
	@$(MAKE) wait-healthy
	@echo "$(GREEN)✓ Development environment started$(RESET)"
	@$(MAKE) show-dev-info

dev-down: ## 🛑 Stop development environment
	@echo "$(BOLD)$(BLUE)Stopping development environment...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_OVERRIDE_FILE) down
	@echo "$(GREEN)✓ Development environment stopped$(RESET)"

dev-restart: dev-down dev-up ## 🔄 Restart development environment

dev-logs: ## 📝 Show all development logs
	@echo "$(BOLD)$(BLUE)Showing development logs...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_OVERRIDE_FILE) logs -f

dev-debug-keycloak: ## 🐛 Start Keycloak with debug enabled
	@echo "$(BOLD)$(BLUE)Starting Keycloak with debug enabled...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_OVERRIDE_FILE) up -d keycloak
	@echo "$(GREEN)✓ Keycloak started with debug on port 8787$(RESET)"

# =============================================================================
# Production Environment Commands
# =============================================================================

prod-up: env-validate dirs-create ## 🚀 Start production environment
	@echo "$(BOLD)$(BLUE)Starting production environment...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_PROD_FILE) --progress plain up -d
	@$(MAKE) wait-healthy
	@echo "$(GREEN)✓ Production environment started$(RESET)"
	@$(MAKE) show-prod-info

prod-down: ## 🛑 Stop production environment
	@echo "$(BOLD)$(BLUE)Stopping production environment...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_PROD_FILE) down
	@echo "$(GREEN)✓ Production environment stopped$(RESET)"

prod-logs: ## 📝 Show production logs
	@echo "$(BOLD)$(BLUE)Showing production logs...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) -f $(COMPOSE_FILE) -f $(COMPOSE_PROD_FILE) logs -f

prod-deploy: prod-down prod-up ## 🚀 Deploy to production (down then up)

# =============================================================================
# Directory and Volume Management
# =============================================================================

dirs-create: ## 📁 Create required directories
	@echo "$(BOLD)$(BLUE)Creating required directories...$(RESET)"
	@$(MKDIR_CMD) data/postgres data/keycloak data/ids logs/nginx logs/keycloak ssl/certs ssl/private db/init
	@echo "$(GREEN)✓ Directories created$(RESET)"

dirs-clean: ## 🧹 Clean data directories (destructive!)
	@echo "$(BOLD)$(RED)This will delete all application data!$(RESET)"
	@echo "$(YELLOW)Are you sure? This action cannot be undone.$(RESET)"
	@read -p "Type 'yes' to continue: " confirm && [ "$$confirm" = "yes" ] || exit 1
	@echo "$(BOLD)$(BLUE)Cleaning data directories...$(RESET)"
	@rm -rf data logs
	@echo "$(GREEN)✓ Data directories cleaned$(RESET)"

volumes-clean: ## 🧹 Remove Docker volumes (destructive!)
	@echo "$(BOLD)$(RED)This will delete all Docker volumes!$(RESET)"
	@echo "$(YELLOW)Are you sure? This action cannot be undone.$(RESET)"
	@read -p "Type 'yes' to continue: " confirm && [ "$$confirm" = "yes" ] || exit 1
	@echo "$(BOLD)$(BLUE)Removing Docker volumes...$(RESET)"
	@docker volume prune -f
	@echo "$(GREEN)✓ Docker volumes removed$(RESET)"

# =============================================================================
# Health and Monitoring Commands
# =============================================================================

wait-healthy: ## ⏳ Wait for all services to be healthy
	@echo "$(BOLD)$(BLUE)Waiting for services to be healthy...$(RESET)"
	@timeout=300; \
	while [ $$timeout -gt 0 ]; do \
		if $(DOCKER_COMPOSE_CMD) ps | grep -E "(unhealthy|starting)" > /dev/null; then \
			echo "$(YELLOW)⏳ Waiting for services... ($$timeout seconds remaining)$(RESET)"; \
			sleep 5; \
			timeout=$$((timeout - 5)); \
		else \
			echo "$(GREEN)✓ All services are healthy$(RESET)"; \
			exit 0; \
		fi; \
	done; \
	echo "$(RED)✗ Services failed to become healthy within timeout$(RESET)"; \
	$(MAKE) health; \
	exit 1

show-dev-info: ## ℹ️ Show development environment information
	@echo "$(BOLD)$(GREEN)=== Development Environment Info ===$(RESET)"
	@echo "$(BLUE)Web Interface:$(RESET) http://eservice.localhost"
	@echo "$(BLUE)Keycloak Admin:$(RESET) http://eservice.localhost/auth/admin (admin/admin)"
	@echo "$(BLUE)Keycloak Debug:$(RESET) Port 8787 (for IDE debugging)"
	@echo "$(BLUE)MockPass:$(RESET) http://mockpass.localhost"
	@echo "$(BLUE)PostgreSQL:$(RESET) localhost:5432 (keycloak/keycloak)"
	@echo "$(BLUE)Hot Reload:$(RESET) Enabled for all Node.js services"

show-prod-info: ## ℹ️ Show production environment information
	@echo "$(BOLD)$(GREEN)=== Production Environment Info ===$(RESET)"
	@echo "$(BLUE)Web Interface:$(RESET) https://eservice.localhost"
	@echo "$(BLUE)Keycloak Admin:$(RESET) https://eservice.localhost/auth/admin"
	@echo "$(BLUE)SSL/TLS:$(RESET) Enabled and enforced"
	@echo "$(BLUE)Security:$(RESET) Hardened configuration active"
	@echo "$(BLUE)Monitoring:$(RESET) Health checks and metrics enabled"

# =============================================================================
# Backup and Restore Commands
# =============================================================================

backup-db: ## 💾 Backup database
	@echo "$(BOLD)$(BLUE)Creating database backup...$(RESET)"
	@timestamp=$$($(DATE_CMD)); \
	$(DOCKER_COMPOSE_CMD) exec -T db pg_dump -U keycloak keycloak > "backup/db_$${timestamp}.sql"
	@echo "$(GREEN)✓ Database backup created$(RESET)"

backup-keycloak: ## 💾 Backup Keycloak configuration
	@echo "$(BOLD)$(BLUE)Creating Keycloak backup...$(RESET)"
	@timestamp=$$($(DATE_CMD)); \
	$(MKDIR_CMD) backup; \
	cp -r data/keycloak "backup/keycloak_$${timestamp}"
	@echo "$(GREEN)✓ Keycloak backup created$(RESET)"

backup-all: backup-db backup-keycloak ## 💾 Backup everything
	@echo "$(GREEN)✓ Complete backup finished$(RESET)"

restore-db: ## 📥 Restore database from backup
	@echo "$(BOLD)$(BLUE)Available database backups:$(RESET)"
	@ls -la backup/db_*.sql 2>/dev/null || echo "No database backups found"
	@read -p "Enter backup filename: " backup_file; \
	if [ -f "backup/$$backup_file" ]; then \
		echo "$(BOLD)$(BLUE)Restoring database from $$backup_file...$(RESET)"; \
		$(DOCKER_COMPOSE_CMD) exec -T db psql -U keycloak -d keycloak < "backup/$$backup_file"; \
		echo "$(GREEN)✓ Database restored$(RESET)"; \
	else \
		echo "$(RED)✗ Backup file not found$(RESET)"; \
		exit 1; \
	fi

# =============================================================================
# Security and SSL Commands
# =============================================================================

ssl-generate: ## 🔐 Generate self-signed SSL certificates for both domains
	@echo "$(BOLD)$(BLUE)Generating self-signed SSL certificates...$(RESET)"
	@$(MKDIR_CMD) ssl/certs ssl/private
	@echo "$(CYAN)Generating certificate for eservice.localhost...$(RESET)"
	@openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
		-keyout ssl/private/eservice.key \
		-out ssl/certs/eservice.crt \
		-config ssl/eservice.conf \
		-extensions v3_req
	@echo "$(CYAN)Generating certificate for mockpass.localhost...$(RESET)"
	@openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
		-keyout ssl/private/mockpass.key \
		-out ssl/certs/mockpass.crt \
		-config ssl/mockpass.conf \
		-extensions v3_req
	@echo "$(GREEN)✓ SSL certificates generated for both domains$(RESET)"
	@echo "$(YELLOW)⚠ These are self-signed certificates for development only$(RESET)"
	@echo "$(BLUE)Files created:$(RESET)"
	@echo "  ssl/private/eservice.key + ssl/certs/eservice.crt"
	@echo "  ssl/private/mockpass.key + ssl/certs/mockpass.crt"

ssl-info: ## 🔍 Show SSL certificate information for both domains
	@echo "$(BOLD)$(BLUE)SSL Certificate Information:$(RESET)"
	@echo "$(CYAN)=== eservice.localhost Certificate ===$(RESET)"
	@if [ -f "ssl/certs/eservice.crt" ]; then \
		openssl x509 -in ssl/certs/eservice.crt -text -noout | grep -E "(Subject:|DNS:|Not Before|Not After)"; \
	else \
		echo "$(RED)✗ eservice.localhost certificate not found$(RESET)"; \
	fi
	@echo ""
	@echo "$(CYAN)=== mockpass.localhost Certificate ===$(RESET)"
	@if [ -f "ssl/certs/mockpass.crt" ]; then \
		openssl x509 -in ssl/certs/mockpass.crt -text -noout | grep -E "(Subject:|DNS:|Not Before|Not After)"; \
	else \
		echo "$(RED)✗ mockpass.localhost certificate not found$(RESET)"; \
	fi
	@if [ ! -f "ssl/certs/eservice.crt" ] && [ ! -f "ssl/certs/mockpass.crt" ]; then \
		echo "$(YELLOW)Run 'make ssl-generate' to create certificates$(RESET)"; \
	fi

# =============================================================================
# Performance and Optimization Commands
# =============================================================================

optimize-db: ## ⚡ Optimize database performance
	@echo "$(BOLD)$(BLUE)Optimizing database performance...$(RESET)"
	@$(DOCKER_COMPOSE_CMD) exec db psql -U keycloak -d keycloak -c "VACUUM ANALYZE;"
	@$(DOCKER_COMPOSE_CMD) exec db psql -U keycloak -d keycloak -c "REINDEX DATABASE keycloak;"
	@echo "$(GREEN)✓ Database optimized$(RESET)"

analyze-performance: ## 📊 Analyze system performance
	@echo "$(BOLD)$(BLUE)System Performance Analysis:$(RESET)"
	@echo "$(BLUE)=== Container Resource Usage ===$(RESET)"
	@docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" $$($(DOCKER_COMPOSE_CMD) ps -q)
	@echo ""
	@echo "$(BLUE)=== Database Statistics ===$(RESET)"
	@$(DOCKER_COMPOSE_CMD) exec db psql -U keycloak -d keycloak -c "SELECT schemaname,tablename,n_tup_ins,n_tup_upd,n_tup_del FROM pg_stat_user_tables ORDER BY n_tup_ins DESC LIMIT 5;"
	@echo ""
	@echo "$(BLUE)=== Nginx Access Summary ===$(RESET)"
	@if [ -f "logs/nginx/access.log" ]; then \
		tail -100 logs/nginx/access.log | awk '{print $$7}' | sort | uniq -c | sort -nr | head -10; \
	else \
		echo "No nginx access logs found"; \
	fi