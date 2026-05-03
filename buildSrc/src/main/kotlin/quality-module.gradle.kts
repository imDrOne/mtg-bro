import dev.detekt.gradle.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

detekt {
    toolVersion = "2.0.0-alpha.3"
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    parallel = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = false
    basePath.set(rootDir)
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.3.21")
        }
    }
}

tasks.withType<Detekt>().configureEach {
    exclude("**/build/**")

    reports {
        checkstyle.required.set(true)
        html.required.set(true)
        sarif.required.set(true)
        markdown.required.set(true)
    }
}

ktlint {
    version.set("1.8.0")
    outputToConsole.set(true)
    ignoreFailures.set(false)

    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }

    filter {
        exclude("**/build/**")
    }
}
