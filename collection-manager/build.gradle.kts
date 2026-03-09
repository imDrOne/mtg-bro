plugins {
    id("spring-module")
    id("liquibase-module")
}

description = "collection-manager"

val exposedVersion: String by project

dependencies {
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}
