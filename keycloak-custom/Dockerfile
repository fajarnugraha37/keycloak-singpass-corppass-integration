# ---- stage 1: build the SPI jar
FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /workspace
COPY spi /workspace/spi
WORKDIR /workspace/spi
# produce a thin/fat jar; "shadowJar" optional (see gradle file)
RUN gradle --no-daemon clean build -x test

# ---- stage 2: assemble custom keycloak
# pick an exact 24.x tag you use in prod
FROM quay.io/keycloak/keycloak:25.0.6

# put your provider(s) here
USER root
COPY --from=builder /workspace/spi/build/libs/*.jar /opt/keycloak/providers/

# (optional) if you have extra libs your SPI needs:
# COPY libs/*.jar /opt/keycloak/providers/

# build the quarkus image (indexes providers for prod)
RUN /opt/keycloak/bin/kc.sh build --verbose

# switch back to the container user
USER 1000

# example runtime (you’ll pass env/args at `docker run`)
# CMD [ "-v" ]  # dev
# or leave default and supply args at runtime
