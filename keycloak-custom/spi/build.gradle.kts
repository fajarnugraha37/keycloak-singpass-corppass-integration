plugins {
    `java-library`
    // uncomment if you want a fat jar:
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
var jacksonVersion = "2.20.0"

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

    // jboss logging
    implementation("org.jboss.logging:jboss-logging:3.5.1.Final")
    testImplementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
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

tasks.shadowJar {
    archiveClassifier.set("") // overwrite default jar
    relocate("com.nimbusds", "com.example.shaded.nimbus")
    relocate("com.apache.httpcomponents.client5", "com.example.shaded.apache.httpcomponents.client5")
}
tasks.build {
    dependsOn("shadowJar")
}
