# Keycloak Custom Build

This directory contains a custom Keycloak build with custom SPI (Service Provider Interface) extensions. The build is optimized for production use with multi-stage Docker builds and advanced caching strategies.

## 📁 Directory Structure

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

## 🚀 Features

- **Custom SPI Extensions**: Extended Keycloak functionality with custom providers
- **Optimized Docker Build**: Multi-stage build with layer caching optimization
- **Production Ready**: Health checks, metrics, and production configurations
- **Build Caching**: Gradle and Docker build cache for faster builds
- **Security**: Non-root user execution and minimal attack surface

## 📋 Prerequisites

- Docker 20.10+ with BuildKit enabled
- Docker Compose (optional, for local development)
- PowerShell 5.1+ (Windows) or Bash (Unix/Linux/macOS)

## 🔧 Building the Image

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

## 🏗️ Build Optimizations

### Layer Caching Strategy

The Dockerfile is optimized for maximum cache efficiency:

1. **Gradle Configuration**: Copied first to cache dependency downloads
2. **Dependencies**: Downloaded in separate layer, cached until build files change
3. **Source Code**: Copied last, only invalidates cache when code changes
4. **Build Artifacts**: Optimized Gradle build with parallel execution and build cache

### Multi-Stage Build Benefits

- **Smaller Final Image**: Only runtime artifacts included
- **Security**: No build tools in production image
- **Faster Builds**: Parallel builds and dependency caching
- **Clean Separation**: Build and runtime environments isolated

## 🐳 Docker Images

### Main Dockerfile
- **Base**: `gradle:8.10.2-jdk17` (builder) + `quay.io/keycloak/keycloak:25.0.6` (runtime)
- **Optimizations**: Layer caching, parallel builds, cleanup
- **Features**: Health/metrics enabled, Kubernetes cache stack

### Alternative Optimized Dockerfile
- **Stages**: 3-stage build (builder, keycloak-builder, runtime)
- **Additional Features**: Health checks, production environment variables
- **Database**: Pre-configured for PostgreSQL
- **Monitoring**: Built-in health and metrics endpoints

## 🔍 Image Details

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

## 🔧 Development

### Local Development Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd app-sso/keycloak-custom
   ```

2. **Build the SPI locally** (optional):
   ```bash
   cd spi
   ./gradlew clean build
   ```

3. **Build Docker image**:
   ```powershell
   .\build.ps1 -Tag "dev"
   ```

### Testing Changes

```bash
# Run tests
cd spi
./gradlew test

# Build and test image
docker build -t keycloak-test .
docker run --rm -p 8080:8080 keycloak-test start-dev
```

## 📊 Performance Considerations

### Build Time Optimization

- **Gradle Wrapper**: Cached in Docker layer
- **Dependencies**: Cached separately from source code
- **Parallel Builds**: Enabled for faster compilation
- **Build Cache**: Gradle build cache for incremental builds

### Runtime Optimization

- **Quarkus Build**: Optimized native compilation
- **Cache Configuration**: Kubernetes-optimized Infinispan cache
- **Health Checks**: Built-in readiness and liveness probes
- **Metrics**: Prometheus-compatible metrics endpoint

## 🔐 Security

### Container Security

- **Non-root User**: Runs as user `1000` (keycloak)
- **Minimal Base**: Based on official Keycloak image
- **No Build Tools**: Production image contains no build dependencies
- **Layer Optimization**: Minimal attack surface

### SPI Security

- **Code Scanning**: Regular dependency vulnerability checks
- **Secure Defaults**: Production-ready security configurations
- **Authentication**: Enhanced JWT and key management

## 🚨 Troubleshooting

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

## 📚 References

- [Keycloak Official Documentation](https://www.keycloak.org/documentation)
- [Keycloak SPI Development](https://www.keycloak.org/docs/latest/server_development/)
- [Docker Multi-Stage Builds](https://docs.docker.com/develop/dev-best-practices/dockerfile_best-practices/)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)

## 📝 License

This project follows the same license as the main repository.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes in the `spi/` directory
4. Test your changes locally
5. Build and test the Docker image
6. Submit a pull request

---

For questions or issues, please refer to the main project documentation or create an issue in the repository.