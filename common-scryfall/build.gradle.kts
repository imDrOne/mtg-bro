plugins {
    kotlin("jvm")
    id("quality-module")
}

group = "xyz.candy-crawler"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.2"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("tools.jackson.module:jackson-module-kotlin")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
