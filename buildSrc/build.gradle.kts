plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.21")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.2")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("org.liquibase:liquibase-gradle-plugin:3.1.0")
    implementation("org.liquibase:liquibase-core:5.0.1")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.4.4")
}
