plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "mtg-bro"

include("wizard-stat-aggregator")
include("collection-manager")
include("mcp-server")
include("draftsim-parser")
include("auth-service")
