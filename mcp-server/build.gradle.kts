plugins {
    kotlin("jvm")
    application
}

group = "xyz.candy-crawler"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val mcpVersion = "0.9.0"
val ktorVersion = "3.2.3"

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

application {
    mainClass.set("xyz.candycrawler.mcpserver.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
