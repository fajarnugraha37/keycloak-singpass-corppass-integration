# Keycloak Custom Build

This directory contains a custom Keycloak build with custom SPI (Service Provider Interface) extensions. The build is optimized for production use with multi-stage Docker builds and advanced caching strategies.

## Directory Structure

```
keycloak-custom/
├── Dockerfile              # Main optimized Dockerfile
├── Dockerfile.optimized    # Alternative 3-stage optimized build
├── .dockerignore           # Excludes unnecessary files from build context
├── build.sh               # Build script for Unix/Linux/macOS
├── build.ps1              # Build script for Windows PowerShell
├── README.md              # This file
└── spi/                   # Custom SPI implementation
    ├── build.gradle.kts   # Gradle build configuration
    ├── gradle.properties  # Gradle properties
    ├── settings.gradle.kts # Gradle settings
    ├── gradlew*           # Gradle wrapper scripts
    ├── gradle/            # Gradle wrapper JAR and version config
    └── src/               # Java source code
        ├── main/java/     # Main source code
        └── test/java/     # Test source code
```

## Features

- **Custom SPI Extensions**: Extended Keycloak functionality with custom providers
- **Docker Build**: Multi-stage build with layer cachin, Health checks, metrics, production configurations, and Non-root user execution and minimal attack surface

## Prerequisites

- Docker 20.10+ with BuildKit enabled
- Docker Compose (optional, for local development)
- PowerShell 5.1+ (Windows) or Bash (Unix/Linux/macOS)

## Building the Image

### Using PowerShell (Windows)

```powershell
# Basic build
.\build.ps1

# Build with custom tag
.\build.ps1 -Tag "v1.0.0" -Name "my-keycloak"

# Build and push to registry
.\build.ps1 -Tag "latest" -Push

# Use alternative optimized Dockerfile
.\build.ps1 -File "Dockerfile.optimized" -Tag "v1.0.0-optimized"

# Build with cache from existing image
.\build.ps1 -CacheFrom "my-keycloak:previous" -Tag "v1.0.1"
```

### Using Bash (Unix/Linux/macOS)

```bash
# Make script executable
chmod +x build.sh

# Basic build
./build.sh

# Build with custom options
./build.sh --tag "v1.0.0" --name "my-keycloak"

# Build and push
./build.sh --tag "latest" --push

# Use alternative Dockerfile
./build.sh --file "Dockerfile.optimized" --tag "v1.0.0-optimized"
```

### Manual Docker Build

```bash
# Basic build
docker build -t custom-keycloak:latest .

# Build with BuildKit optimizations
DOCKER_BUILDKIT=1 docker build -t custom-keycloak:latest .

# Build with cache
docker build --cache-from custom-keycloak:previous -t custom-keycloak:latest .
```

## Image Details

### Included SPI Extensions

The custom Keycloak build includes the following SPI providers:

- **Custom Key Providers**: ECDSA and JWK key providers
- **API Extensions**: Custom certificate and alternate API providers
- **Authentication**: Private key JWT client authenticator
- **Utilities**: Crypto, JWT, JWE, and mapping utilities

### Runtime Configuration

```bash
# Default production startup
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://localhost/keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=password \
  custom-keycloak:latest start --optimized

# Development mode
docker run -d \
  --name keycloak-dev \
  -p 8080:8080 \
  custom-keycloak:latest start-dev
```

## Considerations

### Build Time Optimization

- **Gradle Wrapper**: Cached in Docker layer
- **Dependencies**: Cached separately from source code
- **Parallel Builds**: Enabled for faster compilation
- **Build Cache**: Gradle build cache for incremental builds

## Troubleshooting

### Common Build Issues

1. **Gradle Build Fails**:
   ```bash
   # Check Gradle version compatibility
   cd spi && ./gradlew --version
   
   # Clean and rebuild
   ./gradlew clean build --refresh-dependencies
   ```

2. **Docker Build Fails**:
   ```bash
   # Enable BuildKit for better error messages
   export DOCKER_BUILDKIT=1
   docker build --no-cache -t keycloak-debug .
   ```

3. **Large Image Size**:
   ```bash
   # Check image layers
   docker history custom-keycloak:latest
   
   # Use multi-stage build optimization
   docker build -f Dockerfile.optimized -t custom-keycloak:optimized .
   ```

### Runtime Issues

1. **Container Won't Start**:
   ```bash
   # Check logs
   docker logs keycloak-container
   
   # Verify health check
   docker exec keycloak-container curl -f http://localhost:8080/health/ready
   ```

2. **Performance Issues**:
   - Ensure adequate memory allocation (minimum 1GB)
   - Check database connection performance
   - Monitor metrics endpoint: `http://localhost:8080/metrics`

## References

- [Keycloak Official Documentation](https://www.keycloak.org/documentation)
- [Keycloak SPI Development](https://www.keycloak.org/docs/latest/server_development/)
- [Docker Multi-Stage Builds](https://docs.docker.com/develop/dev-best-practices/dockerfile_best-practices/)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)

