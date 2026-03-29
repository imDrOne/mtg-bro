plugins {
    id("spring-module")
    id("liquibase-module")
    id("jib-module")
}

description = "draftsim-parser"

val exposedVersion: String by project
val springDocWebMVCVersion: String by project

dependencies {
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocWebMVCVersion")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("com.anthropic:anthropic-java:2.18.0")
}
