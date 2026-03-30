// Root project — all shared build logic lives in buildSrc convention plugins:
//   spring-module    — Kotlin, Spring Boot, dependencies, test config
//   liquibase-module — Liquibase config, createMigration task

val dockerExecutable: String by lazy {
    listOf("/usr/local/bin/docker", "/opt/homebrew/bin/docker",
        "/Applications/Docker.app/Contents/Resources/bin/docker")
        .firstOrNull { File(it).canExecute() } ?: "docker"
}

tasks.register("jibDockerBuild") {
    group = "build"
    description = "Builds Docker images for all services (collection-manager, mcp-server, wizard-stat-aggregator)"
    dependsOn(":collection-manager:jibDockerBuild", ":mcp-server:jibDockerBuild", ":wizard-stat-aggregator:jibDockerBuild", ":draftsim-parser:jibDockerBuild")
}

tasks.register<Exec>("runLocal") {
    group = "application"
    description = "Builds Docker images, starts postgres + collection-manager + mcp-server + ngrok in Docker. Blocks until Ctrl+C."
    dependsOn(":collection-manager:jibDockerBuild", ":mcp-server:jibDockerBuild", ":draftsim-parser:jibDockerBuild")

    doFirst {
        val composeFile = file("docker/docker-compose.local.yml")
        logger.lifecycle("[runLocal] Starting Docker Compose (postgres, collection-manager, mcp-server, ngrok) …")
        val composeUp = ProcessBuilder(dockerExecutable, "compose", "-f", composeFile.absolutePath, "up", "-d")
            .directory(projectDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        if (composeUp.waitFor() != 0) throw GradleException("docker compose up failed. Ensure NGROK_AUTHTOKEN is set (env or docker/.env)")
        Thread.sleep(10000)

        Runtime.getRuntime().addShutdownHook(Thread {
            ProcessBuilder(dockerExecutable, "compose", "-f", composeFile.absolutePath, "down")
                .directory(projectDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
        })

        var tunnelUrl: String? = null
        repeat(5) {
            Thread.sleep(2000)
            tunnelUrl = try {
                val curl = ProcessBuilder("curl", "-s", "http://127.0.0.1:4040/api/tunnels").start()
                Regex("\"public_url\"\\s*:\\s*\"(https://[^\"]+)\"").find(curl.inputStream.bufferedReader().readText())?.groupValues?.get(1)
            } catch (_: Exception) { null }
            if (tunnelUrl != null) return@repeat
        }

        logger.lifecycle("")
        logger.lifecycle("========================================")
        logger.lifecycle("  collection-manager : http://localhost:8080")
        logger.lifecycle("  draftsim-parser    : http://localhost:8081")
        logger.lifecycle("  mcp-server         : http://localhost:3000/mcp")
        logger.lifecycle("  tunnel (public)    : ${tunnelUrl?.plus("/mcp") ?: "http://127.0.0.1:4040"}")
        logger.lifecycle("  Press Ctrl+C to stop")
        logger.lifecycle("========================================")
        logger.lifecycle("")
    }

    commandLine("tail", "-f", "/dev/null")
    isIgnoreExitValue = true

    doLast {
        ProcessBuilder(dockerExecutable, "compose", "-f", file("docker/docker-compose.local.yml").absolutePath, "down")
            .directory(projectDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }
}
