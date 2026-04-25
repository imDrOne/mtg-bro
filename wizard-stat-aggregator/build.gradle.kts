plugins {
    id("spring-module")
    id("liquibase-module")
    id("jib-module")
}

description = "wizard-stat-aggregator"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}
