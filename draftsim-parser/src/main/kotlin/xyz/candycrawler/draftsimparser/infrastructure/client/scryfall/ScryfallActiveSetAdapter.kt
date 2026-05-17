package xyz.candycrawler.draftsimparser.infrastructure.client.scryfall

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import xyz.candycrawler.common.scryfall.client.ScryfallApiClient
import xyz.candycrawler.draftsimparser.application.port.ActiveSetSource
import xyz.candycrawler.draftsimparser.configuration.ActiveSetProperties
import xyz.candycrawler.draftsimparser.domain.set.model.ActiveSet
import java.time.LocalDate

@Component
class ScryfallActiveSetAdapter(
    private val scryfallApiClient: ScryfallApiClient,
    private val properties: ActiveSetProperties,
) : ActiveSetSource {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetchActiveSets(): List<ActiveSet> {
        val today = LocalDate.now()
        val windowStart = today.minusDays(properties.windowDays)
        val allowedTypes = properties.setTypes.toSet()

        val sets = scryfallApiClient.getSets().data
        log.debug("Fetched {} sets from Scryfall", sets.size)

        return sets
            .mapNotNull { set ->
                val released = set.releasedAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@mapNotNull null
                if (released !in windowStart..today) return@mapNotNull null
                if (set.setType !in allowedTypes) return@mapNotNull null
                if (set.cardCount <= 0) return@mapNotNull null
                if (set.digital && !properties.includeDigital) return@mapNotNull null
                ActiveSet(
                    code = set.code,
                    name = set.name,
                    releasedAt = released,
                    setType = set.setType,
                )
            }
            .sortedByDescending { it.releasedAt }
            .also { log.info("Found {} active sets in window [{}, {}]", it.size, windowStart, today) }
    }
}
