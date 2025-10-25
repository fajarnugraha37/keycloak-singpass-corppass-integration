# Use Alpine Linux with OpenSSL
FROM alpine:latest

# Install required packages
RUN apk add --no-cache \
    openssl \
    bash \
    make \
    curl \
    && rm -rf /var/cache/apk/*

# Create working directory
WORKDIR /ssl

# Copy configuration files
COPY eservice.conf mockpass.conf ./
COPY Makefile ./
COPY entrypoint.sh /entrypoint.sh

# Create directory structure
RUN mkdir -p certs private saml oidc

# Make entrypoint executable
RUN chmod +x /entrypoint.sh

# Set entrypoint
ENTRYPOINT ["/entrypoint.sh"]

# Default command
CMD ["generate"]

# Add labels
LABEL maintainer="SSL Certificate Generator"
LABEL description="Docker container for generating SSL certificates for SSO applications"
LABEL version="1.0"