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
COMPOSE_PROJECT_NAME := $(PROJECT_NAME)

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

up: check-deps ## 🚀 Start all services with logging
	@echo "$(BOLD)$(GREEN)Starting all services...$(RESET)"
ifeq ($(OS_TYPE),windows)
	@$(DOCKER_COMPOSE_CMD) up -d && $(DOCKER_COMPOSE_CMD) logs -f 2>&1 | powershell -Command "Tee-Object -FilePath '$(LOG_FILE)' -Append"
else
	@$(DOCKER_COMPOSE_CMD) up 2>&1 | tee -a "$(LOG_FILE)"; exit $${PIPESTATUS[0]}
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
re-web: re-web ## 🔧 Legacy: Rebuild web service
re-mockpass: re-mockpass ## 🔧 Legacy: Rebuild mockpass service
re-kc: re-keycloak ## 🔧 Legacy: Rebuild Keycloak service
re-db: re-db ## 🔧 Legacy: Rebuild database service

log-ids: log-ids ## 📝 Legacy: Show IDS logs
log-cpds: log-cpds-api ## 📝 Legacy: Show CPDS API logs
log-aceas: log-aceas-api ## 📝 Legacy: Show ACEAS API logs
log-web: log-web ## 📝 Legacy: Show web logs
log-mockpass: log-mockpass ## 📝 Legacy: Show mockpass logs
log-kc: log-keycloak ## 📝 Legacy: Show Keycloak logs
log-db: log-db ## 📝 Legacy: Show database logs