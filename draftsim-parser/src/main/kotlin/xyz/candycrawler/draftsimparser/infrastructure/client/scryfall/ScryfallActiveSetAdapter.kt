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
            .filter { set ->
                val released = set.releasedAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@filter false
                released in windowStart..today &&
                    set.setType in allowedTypes &&
                    set.cardCount > 0 &&
                    (!set.digital || properties.includeDigital)
            }
            .map { set ->
                ActiveSet(
                    code = set.code,
                    name = set.name,
                    releasedAt = LocalDate.parse(set.releasedAt!!),
                    setType = set.setType,
                )
            }
            .sortedByDescending { it.releasedAt }
            .also { log.info("Found {} active sets in window [{}, {}]", it.size, windowStart, today) }
    }
}
