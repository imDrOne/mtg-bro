plugins {
    id("spring-module")
    id("liquibase-module")
    id("jib-module")
}

description = "collection-manager"

val exposedVersion: String by project
val springDocWebMVCVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocWebMVCVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
}
