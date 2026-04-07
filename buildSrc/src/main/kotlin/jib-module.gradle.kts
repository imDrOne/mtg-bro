plugins {
    id("com.google.cloud.tools.jib")
}

val dockerExecutable: String by lazy {
    val candidates = listOf(
        "/usr/local/bin/docker",
        "/opt/homebrew/bin/docker",
        "/Applications/Docker.app/Contents/Resources/bin/docker",
    )
    val fromEnv = (System.getenv("PATH") ?: "").split(":").map { "$it/docker" }
    (candidates + fromEnv).firstOrNull { java.io.File(it).canExecute() }
        ?: "docker"
}

jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "mtg-bro/${project.name}:latest"
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
    dockerClient {
        executable = dockerExecutable
    }
}
