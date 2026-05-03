plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.21"
    application
    id("jib-module")
    id("quality-module")
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
val ktorVersion = "3.2.4"

dependencies {
    testImplementation(kotlin("test"))

    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.charleskorn.kaml:kaml:0.104.0")
}

application {
    mainClass.set("xyz.candycrawler.mcpserver.MainKt")
}

jib {
    container {
        mainClass = "xyz.candycrawler.mcpserver.MainKt"
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
