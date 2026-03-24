package xyz.candycrawler.collectionmanager.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.tribal.exception.InvalidTribalQueryException
import xyz.candycrawler.collectionmanager.domain.tribal.model.TribalStats
import xyz.candycrawler.collectionmanager.infrastructure.cache.CreatureTypeCacheService

private val WUBRG = listOf("W", "U", "B", "R", "G")

@Service
class TribalAnalysisService(
    private val cardRepository: CardRepository,
    private val creatureTypeCacheService: CreatureTypeCacheService,
) {

    fun analyze(tribe: String): TribalStats {
        val normalizedTribe = tribe.trim()

        val validTypes = creatureTypeCacheService.getCreatureTypes()
        if (!validTypes.any { it.equals(normalizedTribe, ignoreCase = true) }) {
            throw InvalidTribalQueryException(normalizedTribe)
        }

        val cards = cardRepository.findByTribe(normalizedTribe)
        val tribeLower = normalizedTribe.lowercase()

        val creatures = cards.filter { card ->
            card.typeLine.lowercase().let { it.contains("creature") && it.contains(tribeLower) }
        }

        val kindredSpells = cards.filter { card ->
            card.typeLine.lowercase().let { it.contains("kindred") && it.contains(tribeLower) }
        }

        val tribalSupport = cards.filter { card ->
            !card.typeLine.lowercase().contains(tribeLower) &&
            card.oracleText?.lowercase()?.contains(tribeLower) == true
        }

        val byCmc = cards
            .groupBy { card ->
                val cmc = card.cmc.toInt()
                if (cmc >= 5) "5+" else cmc.toString()
            }
            .mapValues { it.value.size }

        val colorSpread = cards
            .map { card ->
                card.colors
                    .sortedBy { color -> WUBRG.indexOf(color.uppercase()).takeIf { it >= 0 } ?: Int.MAX_VALUE }
                    .joinToString("")
            }
            .groupBy { it }
            .mapValues { it.value.size }

        val hasCommander = cards.any { card ->
            card.typeLine.lowercase().let { it.contains("legendary") && it.contains("creature") && it.contains(tribeLower) }
        }

        val hasLord = cards.any { card ->
            card.typeLine.lowercase().let { it.contains("legendary") && it.contains(tribeLower) }
        }

        val deckViability = when {
            creatures.size >= 20 -> "strong"
            creatures.size >= 10 -> "moderate"
            else -> "weak"
        }

        return TribalStats(
            tribe = normalizedTribe,
            totalCards = cards.size,
            byCmc = byCmc,
            creatures = creatures.size,
            tribalSpells = kindredSpells.size,
            tribalSupport = tribalSupport.size,
            colorSpread = colorSpread,
            hasLord = hasLord,
            hasCommander = hasCommander,
            deckViability = deckViability,
        )
    }
}