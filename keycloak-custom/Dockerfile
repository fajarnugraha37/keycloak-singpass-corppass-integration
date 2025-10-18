# ---- stage 1: build the SPI jar
FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /workspace

# Copy gradle files first for better caching
COPY spi/gradle/ /workspace/spi/gradle/
COPY spi/gradlew* /workspace/spi/
COPY spi/settings.gradle.kts /workspace/spi/
COPY spi/gradle.properties /workspace/spi/
COPY spi/build.gradle.kts /workspace/spi/

WORKDIR /workspace/spi

# Download dependencies first (better layer caching)
RUN gradle --no-daemon dependencies

# Copy source code
COPY spi/src/ /workspace/spi/src/

# Build with optimizations
RUN gradle --no-daemon clean build -x test --parallel --build-cache

# ---- stage 2: assemble custom keycloak
# pick an exact 25.x tag you use in prod
FROM quay.io/keycloak/keycloak:25.0.6

# Switch to root temporarily for installation
USER root

# Copy JARs in a single layer and clean up build artifacts
COPY --from=builder /workspace/spi/build/libs/jars/*.jar /opt/keycloak/providers/
COPY --from=builder /workspace/spi/build/libs/*.jar /opt/keycloak/providers/

# Build the Quarkus image with optimizations
RUN /opt/keycloak/bin/kc.sh build \
    --db=postgres \
    --http-relative-path=/auth \
    --health-enabled=true \
    --metrics-enabled=true \
    --verbose 

# Switch back to the container user
USER 1000

# Set default entrypoint arguments for production
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
