package xyz.candycrawler.collectionmanager.domain.deck.model

import xyz.candycrawler.collectionmanager.domain.deck.exception.InvalidDeckException
import java.time.LocalDateTime

class Deck(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val format: DeckFormat,
    val colorIdentity: List<String>,
    val comment: String? = null,
    val entries: List<DeckEntry>,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    init {
        if (name.isBlank()) throw InvalidDeckException("Deck name must not be blank")

        val mainboardTotal = entries.filter { !it.isSideboard }.sumOf { it.quantity }
        if (mainboardTotal < format.minMainboardCards) {
            throw InvalidDeckException(
                "Deck mainboard must have at least ${format.minMainboardCards} cards for ${format.name} format, got $mainboardTotal",
            )
        }
    }
}
