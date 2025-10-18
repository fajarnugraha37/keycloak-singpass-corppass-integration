plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

val kcVersion = "25.0.6"
val nimbusVersion = "9.40"
val nimbusOidcVersion = "11.7.1"
val junitVersion = "5.10.3"
val httpClientVersion = "5.3.1"
val jacksonVersion = "2.20.0"
val jbossVersion = "3.5.1.Final";

dependencies {
    compileOnly("org.keycloak:keycloak-core:$kcVersion")
    compileOnly("org.keycloak:keycloak-server-spi:$kcVersion")
    compileOnly("org.keycloak:keycloak-server-spi-private:$kcVersion")
    compileOnly("org.keycloak:keycloak-services:$kcVersion")
    compileOnly("org.keycloak:keycloak-model-jpa:${kcVersion}")

    compileOnly("jakarta.interceptor:jakarta.interceptor-api:2.1.0")
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")

    implementation("com.nimbusds:oauth2-oidc-sdk:$nimbusOidcVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
    implementation("org.jboss.logging:jboss-logging:$jbossVersion")

    testImplementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    // ensures META-INF/services is included
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes(
            "Implementation-Title" to "custom-keycloak-spi",
            "Implementation-Version" to project.version
        )
    }
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath) {
        exclude("org.keycloak:*")
        exclude("jakarta.interceptor:*")
        exclude("jakarta.inject:*")
        exclude("jakarta.annotation:*")
        exclude("jakarta.enterprise:*")
    }
    into(layout.buildDirectory.dir("libs/jars"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    dependsOn("copyDependencies")
}

tasks.build {
    dependsOn("copyDependencies", "shadowJar")
}
