plugins {
    id("spring-module")
    id("liquibase-module")
}

description = "collection-manager"

val exposedVersion: String by project

dependencies {
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
}
