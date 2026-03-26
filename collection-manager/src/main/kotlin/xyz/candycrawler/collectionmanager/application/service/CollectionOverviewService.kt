package xyz.candycrawler.collectionmanager.application.service

import org.springframework.stereotype.Service
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CollectionOverviewResponse
import xyz.candycrawler.collectionmanager.application.rest.dto.response.CollectionOverviewResponse.TopTribeResponse
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.model.MtgColor
import xyz.candycrawler.collectionmanager.domain.card.repository.QueryCardRepository

@Service
class CollectionOverviewService(private val queryCardRepository: QueryCardRepository) {

    fun getOverview(): CollectionOverviewResponse {
        val cards = queryCardRepository.findAllInCollection()

        return CollectionOverviewResponse(
            totalCards = cards.size,
            byColor = computeByColor(cards),
            byType = computeByType(cards),
            topTribes = computeTopTribes(cards),
            byRarity = computeByRarity(cards),
        )
    }

    private fun computeByColor(cards: List<Card>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        cards.forEach { card ->
            val colors = card.colors.ifEmpty { listOf("C") }
            colors.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        }
        return counts.entries
            .sortedBy { (color, _) -> if (color == "C") MtgColor.entries.size else MtgColor.sortIndex(color) }
            .associate { it.toPair() }
    }

    private fun computeByType(cards: List<Card>): Map<String, Int> =
        cards.groupBy { primaryType(it.typeLine) }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .associate { it.toPair() }

    private fun primaryType(typeLine: String): String {
        val before = typeLine.substringBefore("—").trim()
        return when {
            before.contains("creature", ignoreCase = true) -> "creature"
            before.contains("instant", ignoreCase = true) -> "instant"
            before.contains("sorcery", ignoreCase = true) -> "sorcery"
            before.contains("enchantment", ignoreCase = true) -> "enchantment"
            before.contains("artifact", ignoreCase = true) -> "artifact"
            before.contains("land", ignoreCase = true) -> "land"
            before.contains("planeswalker", ignoreCase = true) -> "planeswalker"
            before.contains("battle", ignoreCase = true) -> "battle"
            else -> "other"
        }
    }

    private fun computeTopTribes(cards: List<Card>): List<TopTribeResponse> {
        val tribeCards = mutableMapOf<String, MutableList<Card>>()
        cards.filter { it.typeLine.contains("creature", ignoreCase = true) }
            .forEach { card ->
                extractSubtypes(card.typeLine).forEach { subtype ->
                    tribeCards.getOrPut(subtype) { mutableListOf() }.add(card)
                }
            }
        return tribeCards.entries
            .sortedByDescending { it.value.size }
            .take(10)
            .map { (tribe, tribeCardList) ->
                TopTribeResponse(
                    name = tribe,
                    count = tribeCardList.size,
                    colors = tribeColors(tribeCardList),
                )
            }
    }

    private fun extractSubtypes(typeLine: String): List<String> {
        val afterDash = typeLine.substringAfter("—", missingDelimiterValue = "").trim()
        if (afterDash.isBlank()) return emptyList()
        return afterDash.split(" ").map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun tribeColors(tribeCards: List<Card>): String =
        tribeCards.flatMap { it.colors }.toSet()
            .sortedBy { MtgColor.sortIndex(it) }
            .joinToString("")

    private fun computeByRarity(cards: List<Card>): Map<String, Int> =
        cards.groupBy { it.rarity }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .associate { it.toPair() }
}
