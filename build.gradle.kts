// Root project — all shared build logic lives in buildSrc convention plugins:
//   spring-module    — Kotlin, Spring Boot, dependencies, test config
//   liquibase-module — Liquibase config, createMigration task

tasks.register("runLocal") {
    group = "application"
    description = "Starts collection-manager, mcp-server (HTTP), and cloudflared tunnel"
    dependsOn(":collection-manager:bootJar", ":mcp-server:installDist")

    doLast {
        val mcpPort = (project.findProperty("mcpPort") as? String)?.toIntOrNull() ?: 3000
        val cmPort = (project.findProperty("cmPort") as? String)?.toIntOrNull() ?: 8080

        fun killPortOccupant(port: Int) {
            try {
                val pids = ProcessBuilder("lsof", "-t", "-i", ":$port")
                    .redirectErrorStream(true).start()
                    .inputStream.bufferedReader().readText().trim()
                if (pids.isNotEmpty()) {
                    pids.lines().forEach { pid ->
                        logger.lifecycle("[runLocal] Killing stale process on port $port (PID $pid)")
                        ProcessBuilder("kill", pid).start().waitFor()
                    }
                    Thread.sleep(500)
                }
            } catch (_: Exception) { }
        }

        fun killStaleProcesses(pattern: String) {
            try {
                val pids = ProcessBuilder("pgrep", "-f", pattern)
                    .redirectErrorStream(true).start()
                    .inputStream.bufferedReader().readText().trim()
                if (pids.isNotEmpty()) {
                    logger.lifecycle("[runLocal] Killing stale $pattern processes …")
                    ProcessBuilder("pkill", "-f", pattern).start().waitFor()
                    Thread.sleep(500)
                }
            } catch (_: Exception) { }
        }

        killPortOccupant(cmPort)
        killPortOccupant(mcpPort)
        killStaleProcesses("cloudflared.*tunnel")
        killStaleProcesses("ngrok")

        val cmJar = file("collection-manager/build/libs")
            .listFiles()
            ?.filter { it.name.endsWith(".jar") && !it.name.contains("plain") }
            ?.maxByOrNull { it.lastModified() }
            ?: error("collection-manager boot jar not found in collection-manager/build/libs")

        val mcpBin = file("mcp-server/build/install/mcp-server/bin/mcp-server")
        require(mcpBin.exists()) { "mcp-server binary not found at ${mcpBin.absolutePath}" }

        data class NamedProcess(val name: String, val process: Process)

        val managed = mutableListOf<NamedProcess>()
        Runtime.getRuntime().addShutdownHook(Thread {
            managed.asReversed().forEach { if (it.process.isAlive) it.process.destroyForcibly() }
        })

        logger.lifecycle("[runLocal] Starting collection-manager on port $cmPort …")
        managed += NamedProcess(
            "collection-manager",
            ProcessBuilder("java", "-jar", cmJar.absolutePath, "--server.port=$cmPort")
                .directory(projectDir)
                .inheritIO()
                .start()
        )
        Thread.sleep(2_000)
        managed.find { !it.process.isAlive }?.let {
            logger.error("[runLocal] ${it.name} exited immediately with code ${it.process.exitValue()}")
            managed.filter { m -> m.process.isAlive }.forEach { m -> m.process.destroyForcibly() }
            throw GradleException("${it.name} failed to start")
        }

        logger.lifecycle("[runLocal] Starting mcp-server (HTTP) on port $mcpPort …")
        managed += NamedProcess(
            "mcp-server",
            ProcessBuilder(mcpBin.absolutePath, "--transport", "http", "--port", mcpPort.toString())
                .directory(projectDir)
                .also { it.environment()["COLLECTION_MANAGER_BASE_URL"] = "http://localhost:$cmPort" }
                .inheritIO()
                .start()
        )
        Thread.sleep(2_000)
        managed.find { !it.process.isAlive }?.let {
            logger.error("[runLocal] ${it.name} exited immediately with code ${it.process.exitValue()}")
            managed.filter { m -> m.process.isAlive }.forEach { m -> m.process.destroyForcibly() }
            throw GradleException("${it.name} failed to start")
        }

        logger.lifecycle("[runLocal] Starting cloudflared tunnel → http://localhost:$mcpPort …")
        val tunnelProcess = ProcessBuilder(
            "cloudflared", "tunnel", "--url", "http://localhost:$mcpPort", "--no-autoupdate"
        )
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        managed += NamedProcess("cloudflared", tunnelProcess)

        val tunnelReader = tunnelProcess.inputStream.bufferedReader()
        var tunnelUrl: String? = null
        val urlPattern = Regex("https://[a-z0-9-]+\\.trycloudflare\\.com")

        val readerThread = Thread {
            try {
                tunnelReader.forEachLine { line ->
                    logger.lifecycle("[cloudflared] $line")
                    if (tunnelUrl == null) {
                        urlPattern.find(line)?.let { tunnelUrl = it.value }
                    }
                }
            } catch (_: Exception) { }
        }
        readerThread.isDaemon = true
        readerThread.start()

        for (attempt in 1..20) {
            Thread.sleep(1_000)
            if (!tunnelProcess.isAlive) {
                logger.error("[runLocal] cloudflared exited with code ${tunnelProcess.exitValue()}")
                managed.filter { it.process.isAlive }.forEach { it.process.destroyForcibly() }
                throw GradleException("cloudflared failed to start")
            }
            if (tunnelUrl != null) break
        }

        logger.lifecycle("")
        logger.lifecycle("=========================================")
        logger.lifecycle("  collection-manager : http://localhost:$cmPort")
        logger.lifecycle("  mcp-server         : http://localhost:$mcpPort/mcp")
        if (tunnelUrl != null) {
            logger.lifecycle("  tunnel (public)    : $tunnelUrl/mcp")
        } else {
            logger.lifecycle("  tunnel             : waiting for URL (check cloudflared output)")
        }
        logger.lifecycle("=========================================")
        logger.lifecycle("")

        while (managed.all { it.process.isAlive }) {
            Thread.sleep(1_000)
        }

        val exited = managed.first { !it.process.isAlive }
        logger.lifecycle("[runLocal] ${exited.name} exited (code ${exited.process.exitValue()}), shutting down …")
        managed.filter { it.process.isAlive }.forEach { it.process.destroyForcibly() }
    }
}
