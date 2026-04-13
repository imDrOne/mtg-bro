plugins {
    id("spring-module")
    id("liquibase-module")
    id("jib-module")
}

description = "auth-service"

configurations.all {
    exclude(group = "org.jetbrains.exposed")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-authorization-server")
    runtimeOnly("commons-logging:commons-logging:1.3.5")
    implementation("com.bucket4j:bucket4j_jdk17-core:8.18.0")
    implementation("com.bucket4j:bucket4j_jdk17-caffeine:8.18.0")
    implementation("com.github.ben-manes.caffeine:caffeine")
    testImplementation("org.springframework.security:spring-security-test")
}
