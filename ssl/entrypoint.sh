#!/bin/bash
set -e

echo "🐳 SSL Certificate Generator Container"
echo "====================================="

# Function to generate certificates
generate_certs() {
    local validity_days=${1:-365}
    echo "🔐 Generating certificates with ${validity_days} days validity..."
    make generate-unix VALIDITY_DAYS=${validity_days}
    echo "✅ Certificate generation complete!"
}

# Function to show certificate info
show_certs() {
    echo "🔍 Certificate Information:"
    make show-unix
}

# Function to validate certificates
validate_certs() {
    echo "🔍 Validating certificates:"
    make validate-unix
}

# Function to clean certificates
clean_certs() {
    echo "🗑️ Cleaning certificates..."
    rm -rf certs/ private/ saml/ oidc/ encryption.key 2>/dev/null || true
    echo "✅ Cleanup complete"
}

# Function to generate environment file
generate_env() {
    echo "📋 Generating .env file..."
    make env-dotenv-unix > .env
}

# Parse command line arguments
case "${1:-generate}" in
    "generate")
        generate_certs ${2:-365}
        ;;
    "show")
        show_certs
        ;;
    "validate")
        validate_certs
        ;;
    "clean")
        clean_certs
        ;;
    "env")
        generate_env
        ;;
    "all")
        generate_certs ${2:-365}
        generate_env
        show_certs
        validate_certs
        ;;
    "bash")
        exec /bin/bash
        ;;
    "help"|*)
        echo "Usage: docker run [options] ssl-cert-generator [command] [validity_days]"
        echo ""
        echo "Commands:"
        echo "  generate [days]  Generate certificates (default: 365 days)"
        echo "  show            Show certificate information"  
        echo "  validate        Validate certificates"
        echo "  clean           Remove all certificates"
        echo "  env             Generate .env file"
        echo "  all [days]      Generate certificates, .env, and show info"
        echo "  bash            Open interactive bash shell"
        echo "  help            Show this help message"
        echo ""
        echo "Examples:"
        echo "  docker run --rm -v \$(pwd):/ssl ssl-cert-generator"
        echo "  docker run --rm -v \$(pwd):/ssl ssl-cert-generator generate 730"
        echo "  docker run --rm -v \$(pwd):/ssl ssl-cert-generator all"
        ;;
esac