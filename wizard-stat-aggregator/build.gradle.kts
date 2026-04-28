plugins {
    id("spring-module")
    id("liquibase-module")
    id("jib-module")
}

description = "wizard-stat-aggregator"

val exposedVersion: String by project
val springDocWebMVCVersion: String by project

dependencies {
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocWebMVCVersion")
}
