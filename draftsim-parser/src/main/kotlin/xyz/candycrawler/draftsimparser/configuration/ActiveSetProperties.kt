package xyz.candycrawler.draftsimparser.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infrastructure.active-sets")
data class ActiveSetProperties(
    val windowDays: Long = 365,
    val setTypes: List<String> = listOf("expansion", "core", "draft_innovation", "commander", "masters"),
    val includeDigital: Boolean = false,
)
