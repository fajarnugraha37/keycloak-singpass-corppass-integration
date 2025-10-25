# Makefile for SSL Certificate Management
# Supports both Windows (PowerShell) and Unix-like systems (Bash)

# Variables
VALIDITY_DAYS ?= 365
ORG_NAME ?= "SSL Development CA"
SHELL := /bin/bash

# Detect OS
ifeq ($(OS),Windows_NT)
    DETECTED_OS := Windows
    PWSH := pwsh.exe
    RM := Remove-Item -Force -Recurse
    MKDIR := New-Item -ItemType Directory -Force
else
    DETECTED_OS := $(shell uname -s)
    PWSH := pwsh
    RM := rm -rf
    MKDIR := mkdir -p
endif

.PHONY: help generate show validate clean clean-all clean-expired clean-backups env-powershell env-bash env-dotenv install-openssl

# Default target
help: ## Show this help message
	@echo "🔐 SSL Certificate Management"
	@echo "============================="
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Variables:"
	@echo "  VALIDITY_DAYS=$(VALIDITY_DAYS)  Certificate validity in days"
	@echo "  ORG_NAME=$(ORG_NAME)  Organization name for CA certificate"
	@echo ""
	@echo "Examples:"
	@echo "  make generate                Generate all certificates"
	@echo "  make generate VALIDITY_DAYS=730  Generate with 2-year validity"
	@echo "  make generate-ca ORG_NAME='My Company'  Generate custom CA"
	@echo "  make validate                Validate all certificates"
	@echo "  make clean                   Remove all certificates"

generate: ## Generate all SSL certificates and keys
	@echo "🔐 Generating SSL certificates ($(VALIDITY_DAYS) days validity)..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\generate-certificates.ps1" -ValidityDays $(VALIDITY_DAYS) -Force
else
	@echo "Generating certificates using OpenSSL commands..."
	@$(MAKE) generate-unix
endif

generate-unix: ## Generate certificates on Unix-like systems
	@echo "Creating directories..."
	@$(MKDIR) certs private saml oidc
	
	@echo "Generating SSL certificates..."
	@openssl req -x509 -nodes -days $(VALIDITY_DAYS) -newkey rsa:2048 \
		-keyout private/eservice.key \
		-out certs/eservice.crt \
		-config eservice.conf \
		-extensions v3_req
	
	@openssl req -x509 -nodes -days $(VALIDITY_DAYS) -newkey rsa:2048 \
		-keyout private/mockpass.key \
		-out certs/mockpass.crt \
		-config mockpass.conf \
		-extensions v3_req
	
	@echo "Generating SAML keys..."
	@openssl req -x509 -newkey rsa:2048 -keyout saml/key.pem -out saml/public.crt \
		-sha256 -days $(VALIDITY_DAYS) -nodes \
		-subj "/C=SG/ST=Singapore/L=Singapore/O=SSO/OU=SAML/CN=saml-signing"
	
	@echo "Generating OIDC/JWT keys..."
	@openssl genrsa -out oidc/private-key.pem 3072
	@openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
		-in oidc/private-key.pem -out oidc/private_key.pem
	@openssl rsa -in oidc/private_key.pem -pubout -out oidc/public_key.pem
	
	@echo "Generating base64 files..."
	@cat saml/public.crt | base64 > saml/public_base64.txt
	@cat saml/key.pem | base64 > saml/key_base64.txt
	@cat oidc/private_key.pem | base64 > oidc/private_key_base64.txt
	@cat oidc/public_key.pem | base64 > oidc/public_key_base64.txt
	
	@echo "Generating encryption key..."
	@openssl rand -base64 32 > encryption.key
	
	@echo "✅ Certificate generation complete!"

show: ## Show certificate information
	@echo "🔍 Displaying certificate information..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\show-certificates.ps1"
else
	@$(MAKE) show-unix
endif

show-unix: ## Show certificate info on Unix-like systems
	@echo "SSL Certificates:"
	@if [ -f certs/eservice.crt ]; then \
		echo "eService Certificate:"; \
		openssl x509 -in certs/eservice.crt -text -noout | grep -E "(Subject:|Not Before|Not After|DNS:)"; \
		echo ""; \
	fi
	@if [ -f certs/mockpass.crt ]; then \
		echo "MockPass Certificate:"; \
		openssl x509 -in certs/mockpass.crt -text -noout | grep -E "(Subject:|Not Before|Not After|DNS:)"; \
		echo ""; \
	fi
	@if [ -f saml/public.crt ]; then \
		echo "SAML Certificate:"; \
		openssl x509 -in saml/public.crt -text -noout | grep -E "(Subject:|Not Before|Not After)"; \
	fi

validate: ## Validate all certificates and keys
	@echo "🔍 Validating certificates..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\validate-certificates.ps1"
else
	@$(MAKE) validate-unix
endif

validate-unix: ## Validate certificates on Unix-like systems
	@echo "Checking certificate files..."
	@for cert in certs/eservice.crt certs/mockpass.crt saml/public.crt; do \
		if [ -f $$cert ]; then \
			echo "✅ $$cert exists"; \
			if openssl x509 -in $$cert -noout -checkend 2592000 > /dev/null 2>&1; then \
				echo "✅ $$cert is valid for at least 30 days"; \
			else \
				echo "⚠️  $$cert expires within 30 days or is expired"; \
			fi \
		else \
			echo "❌ $$cert missing"; \
		fi \
	done

clean: ## Remove all certificates and keys
	@echo "🗑️  Cleaning up all certificates and keys..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\cleanup-certificates.ps1" -All -Force
else
	@$(RM) certs/ private/ saml/ oidc/ encryption.key 2>/dev/null || true
	@echo "✅ Cleanup complete"
endif

clean-expired: ## Remove only expired certificates
	@echo "🗑️  Cleaning up expired certificates..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\cleanup-certificates.ps1" -Expired -Force
else
	@echo "Expired certificate cleanup requires PowerShell on Unix systems"
endif

clean-backups: ## Remove backup files
	@echo "🗑️  Cleaning up backup files..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\cleanup-certificates.ps1" -Backups -Force
else
	@find . -name "*.backup.*" -delete 2>/dev/null || true
	@echo "✅ Backup cleanup complete"
endif

clean-all: clean clean-backups ## Remove everything including backups

generate-ca: ## Generate custom Certificate Authority
	@echo "🏛️ Generating custom Certificate Authority..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\generate-ca.ps1" -OrganizationName "$(ORG_NAME)" -ValidityDays $(VALIDITY_DAYS)
else
	@echo "CA generation requires PowerShell on Unix systems"
	@echo "Please install PowerShell: https://docs.microsoft.com/en-us/powershell/scripting/install/installing-powershell"
endif

update-ca-bundle: ## Update CA bundle with Mozilla's trusted root certificates
	@echo "📦 Updating CA bundle..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\update-ca-bundle.ps1" -Source mozilla
else
	@echo "CA bundle update requires PowerShell on Unix systems"
endif

install-ca: ## Install custom CA in system trust store (requires admin/sudo)
	@echo "🔧 Installing custom CA in system trust store..."
ifeq ($(DETECTED_OS),Windows)
	@echo "Run as Administrator:"
	@echo "certlm.msc -> Import ca/certs/ca.crt to Trusted Root Certification Authorities"
else
	@if [ -f ca/certs/ca.crt ]; then \
		if [ "$(DETECTED_OS)" = "Darwin" ]; then \
			sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ca/certs/ca.crt; \
		else \
			sudo cp ca/certs/ca.crt /usr/local/share/ca-certificates/custom-ca.crt; \
			sudo update-ca-certificates; \
		fi \
	else \
		echo "❌ CA certificate not found. Run 'make generate-ca' first."; \
	fi
endif

ca-info: ## Show CA certificate information
	@echo "📋 CA Certificate Information:"
	@if [ -f ca/certs/ca.crt ]; then \
		openssl x509 -in ca/certs/ca.crt -text -noout | grep -E "(Subject:|Issuer:|Not Before|Not After|Serial Number:)"; \
	else \
		echo "❌ CA certificate not found. Run 'make generate-ca' first."; \
	fi

clean-ca: ## Remove CA certificates and configuration
	@echo "🗑️ Cleaning CA certificates..."
	@rm -rf ca/ 2>/dev/null || true
	@echo "✅ CA cleanup complete"

env-powershell: ## Generate PowerShell environment variables
	@echo "📋 Generating PowerShell environment variables..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\load-environment.ps1" -Format powershell
else
	@echo "PowerShell environment generation requires PowerShell"
endif

env-bash: ## Generate Bash environment variables
	@echo "📋 Generating Bash environment variables..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\load-environment.ps1" -Format bash
else
	@$(MAKE) env-bash-unix
endif

env-bash-unix: ## Generate Bash environment on Unix-like systems
	@echo "#!/bin/bash"
	@echo "# Bash Environment Variables"
	@echo "# Generated on $$(date)"
	@echo ""
	@echo "export SSL_ESERVICE_CERT_PATH=./ssl/certs/eservice.crt"
	@echo "export SSL_ESERVICE_KEY_PATH=./ssl/private/eservice.key"
	@echo "export SSL_MOCKPASS_CERT_PATH=./ssl/certs/mockpass.crt"
	@echo "export SSL_MOCKPASS_KEY_PATH=./ssl/private/mockpass.key"
	@echo "export SAML_SIGNING_CERT_PATH=./ssl/saml/public.crt"
	@echo "export SAML_SIGNING_KEY_PATH=./ssl/saml/key.pem"
	@echo "export JWT_PRIVATE_KEY_PATH=./ssl/oidc/private_key.pem"
	@echo "export JWT_PUBLIC_KEY_PATH=./ssl/oidc/public_key.pem"
	@if [ -f saml/public_base64.txt ]; then \
		echo "export SAML_PUBLIC_CERT_BASE64=$$(cat saml/public_base64.txt)"; \
	fi
	@if [ -f saml/key_base64.txt ]; then \
		echo "export SAML_PRIVATE_KEY_BASE64=$$(cat saml/key_base64.txt)"; \
	fi
	@if [ -f oidc/private_key_base64.txt ]; then \
		echo "export JWT_PRIVATE_KEY_BASE64=$$(cat oidc/private_key_base64.txt)"; \
	fi
	@if [ -f oidc/public_key_base64.txt ]; then \
		echo "export JWT_PUBLIC_KEY_BASE64=$$(cat oidc/public_key_base64.txt)"; \
	fi
	@if [ -f encryption.key ]; then \
		echo "export ENCRYPTION_KEY=$$(cat encryption.key)"; \
	fi
	@echo ""
	@echo "echo 'SSL certificate environment variables loaded'"

env-dotenv: ## Generate .env file
	@echo "📋 Generating .env file..."
ifeq ($(DETECTED_OS),Windows)
	$(PWSH) -ExecutionPolicy Bypass -File ".\load-environment.ps1" -Format dotenv -Output .env
else
	@$(MAKE) env-dotenv-unix
endif

env-dotenv-unix: ## Generate .env file on Unix-like systems
	@echo "# Environment Variables" > .env
	@echo "# Generated on $$(date)" >> .env
	@echo "" >> .env
	@echo "SSL_ESERVICE_CERT_PATH=./ssl/certs/eservice.crt" >> .env
	@echo "SSL_ESERVICE_KEY_PATH=./ssl/private/eservice.key" >> .env
	@echo "SSL_MOCKPASS_CERT_PATH=./ssl/certs/mockpass.crt" >> .env
	@echo "SSL_MOCKPASS_KEY_PATH=./ssl/private/mockpass.key" >> .env
	@echo "SAML_SIGNING_CERT_PATH=./ssl/saml/public.crt" >> .env
	@echo "SAML_SIGNING_KEY_PATH=./ssl/saml/key.pem" >> .env
	@echo "JWT_PRIVATE_KEY_PATH=./ssl/oidc/private_key.pem" >> .env
	@echo "JWT_PUBLIC_KEY_PATH=./ssl/oidc/public_key.pem" >> .env
	@if [ -f saml/public_base64.txt ]; then \
		echo "SAML_PUBLIC_CERT_BASE64=$$(cat saml/public_base64.txt)" >> .env; \
	fi
	@if [ -f saml/key_base64.txt ]; then \
		echo "SAML_PRIVATE_KEY_BASE64=$$(cat saml/key_base64.txt)" >> .env; \
	fi
	@if [ -f oidc/private_key_base64.txt ]; then \
		echo "JWT_PRIVATE_KEY_BASE64=$$(cat oidc/private_key_base64.txt)" >> .env; \
	fi
	@if [ -f oidc/public_key_base64.txt ]; then \
		echo "JWT_PUBLIC_KEY_BASE64=$$(cat oidc/public_key_base64.txt)" >> .env; \
	fi
	@if [ -f encryption.key ]; then \
		echo "ENCRYPTION_KEY=$$(cat encryption.key)" >> .env; \
	fi
	@echo "" >> .env
	@echo "ESERVICE_URL=https://eservice.localhost:3000" >> .env
	@echo "MOCKPASS_URL=https://mockpass.localhost:5000" >> .env
	@echo "NODE_ENV=development" >> .env

install-openssl: ## Install OpenSSL (Windows only)
ifeq ($(DETECTED_OS),Windows)
	@echo "Installing OpenSSL via Chocolatey..."
	@choco install openssl -y
	@echo "✅ OpenSSL installation complete"
else
	@echo "Please install OpenSSL using your system package manager:"
	@echo "  Ubuntu/Debian: sudo apt-get install openssl"
	@echo "  CentOS/RHEL:   sudo yum install openssl"
	@echo "  macOS:         brew install openssl"
endif

# Development targets
dev-setup: generate-ca generate update-ca-bundle env-dotenv ## Set up complete development environment
	@echo "🚀 Development environment setup complete!"
	@echo "✅ Custom CA generated"
	@echo "✅ Certificates generated"
	@echo "✅ CA bundle updated"
	@echo "✅ .env file created"
	@echo ""
	@echo "Next steps:"
	@echo "1. Install the CA certificate: make install-ca"
	@echo "2. Review the generated .env file"
	@echo "3. Update your application configuration"
	@echo "4. Start your development servers"

status: ## Show current status
	@echo "🔐 SSL Certificate Status"
	@echo "========================="
	@echo "Detected OS: $(DETECTED_OS)"
	@if command -v openssl >/dev/null 2>&1; then \
		echo "✅ OpenSSL: $$(openssl version)"; \
	else \
		echo "❌ OpenSSL: Not found"; \
	fi
ifeq ($(DETECTED_OS),Windows)
	@if command -v pwsh.exe >/dev/null 2>&1; then \
		echo "✅ PowerShell: Available"; \
	else \
		echo "❌ PowerShell: Not found"; \
	fi
endif
	@echo ""
	@echo "Certificate files:"
	@for file in certs/eservice.crt certs/mockpass.crt private/eservice.key private/mockpass.key saml/public.crt saml/key.pem oidc/private_key.pem oidc/public_key.pem encryption.key; do \
		if [ -f $$file ]; then \
			echo "✅ $$file"; \
		else \
			echo "❌ $$file"; \
		fi \
	done
	@echo ""
	@echo "CA files:"
	@for file in ca/certs/ca.crt ca/certs/ca.pem ca/certs/ca-bundle.crt ca/private/ca.key; do \
		if [ -f $$file ]; then \
			echo "✅ $$file"; \
		else \
			echo "❌ $$file"; \
		fi \
	done