import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    id("org.liquibase.gradle")
}

val liquibaseVersion: String by project

val dbProperties = Properties().apply {
    val file = project.file("db.properties")
    if (file.exists()) load(file.inputStream())
}

fun dbProp(envKey: String, propKey: String): String =
    System.getenv(envKey) ?: dbProperties.getProperty(propKey) ?: ""

dependencies {
    "liquibaseRuntime"("org.liquibase:liquibase-core:$liquibaseVersion")
    "liquibaseRuntime"("org.liquibase:liquibase-groovy-dsl:4.0.1")
    "liquibaseRuntime"("info.picocli:picocli:4.6.1")
    "liquibaseRuntime"("org.postgresql:postgresql:42.7.3")
    "liquibaseRuntime"(files("src/main/resources"))

    "testImplementation"("org.springframework.boot:spring-boot-liquibase")
    "testImplementation"("org.liquibase:liquibase-core:$liquibaseVersion")
}

liquibase {
    activities.register("main") {
        arguments = mapOf(
            "logLevel" to "info",
            "changelogFile" to "db/changelog/db.changelog-master.yaml",
            "url" to dbProp("DB_MIGRATION_URL", "db.migration.url"),
            "username" to dbProp("DB_MIGRATION_USERNAME", "db.migration.username"),
            "password" to dbProp("DB_MIGRATION_PASSWORD", "db.migration.password"),
            "driver" to "org.postgresql.Driver"
        )
    }
    runList = "main"
}

tasks.register("createMigration") {
    group = "liquibase"
    description = "Creates a new Liquibase SQL migration file"

    doLast {
        val migrationName = project.findProperty("sqlName") as? String
            ?: error("Migration name is required. Use: ./gradlew createMigration -PsqlName=your_migration_name")

        val author = Runtime.getRuntime()
            .exec(arrayOf("git", "config", "user.name"))
            .inputStream.bufferedReader().readText().trim()
            .ifEmpty { "unknown" }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        val fileName = "${timestamp}_${migrationName}.sql"
        val migrationsDir = file("src/main/resources/db/changelog/migrations")
        val migrationFile = file("$migrationsDir/$fileName")

        migrationFile.writeText(
            """
            |-- liquibase formatted sql
            |
            |-- changeset $author:$timestamp
            |-- comment: $migrationName
            |
            |-- TODO: write your migration here
            |
            |-- rollback
            |-- TODO: write your rollback here
            """.trimMargin()
        )

        Runtime.getRuntime().exec(arrayOf("git", "add", "$migrationsDir"))

        println("Migration created: src/main/resources/db/changelog/migrations/$fileName")
    }
}
