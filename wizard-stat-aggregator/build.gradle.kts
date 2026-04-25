plugins {
    id("spring-module")
    id("liquibase-module")
    id("jib-module")
}

description = "wizard-stat-aggregator"

val springDocWebMVCVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocWebMVCVersion")
}
