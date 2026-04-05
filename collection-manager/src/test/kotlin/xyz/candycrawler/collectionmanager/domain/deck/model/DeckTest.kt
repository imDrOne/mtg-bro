package xyz.candycrawler.collectionmanager.domain.deck.model

import org.junit.jupiter.api.Test
import xyz.candycrawler.collectionmanager.domain.deck.exception.InvalidDeckException
import kotlin.test.assertFailsWith

class DeckTest {

    private fun entry(cardId: Long = 1L, quantity: Int = 1, sideboard: Boolean = false) =
        DeckEntry(cardId = cardId, quantity = quantity, isSideboard = sideboard)

    private fun standardDeck(entries: List<DeckEntry>, name: String = "Test Deck") = Deck(
        name = name,
        format = DeckFormat.STANDARD,
        colorIdentity = listOf("G"),
        comment = null,
        entries = entries,
    )

    @Test
    fun `valid standard deck with exactly 60 mainboard cards passes`() {
        val entries = (1..60).map { entry(cardId = it.toLong()) }
        standardDeck(entries)
    }

    @Test
    fun `standard deck with 59 mainboard cards throws InvalidDeckException`() {
        val entries = (1..59).map { entry(cardId = it.toLong()) }
        assertFailsWith<InvalidDeckException> { standardDeck(entries) }
    }

    @Test
    fun `sealed deck with exactly 40 mainboard cards passes`() {
        val entries = (1..40).map { entry(cardId = it.toLong()) }
        Deck(
            name = "Sealed Deck",
            format = DeckFormat.SEALED,
            colorIdentity = emptyList(),
            comment = null,
            entries = entries,
        )
    }

    @Test
    fun `sealed deck with 39 mainboard cards throws InvalidDeckException`() {
        val entries = (1..39).map { entry(cardId = it.toLong()) }
        assertFailsWith<InvalidDeckException> {
            Deck(
                name = "Sealed Deck",
                format = DeckFormat.SEALED,
                colorIdentity = emptyList(),
                comment = null,
                entries = entries,
            )
        }
    }

    @Test
    fun `sideboard cards do not count towards mainboard minimum`() {
        val mainboard = (1..60).map { entry(cardId = it.toLong(), sideboard = false) }
        val sideboard = (61..75).map { entry(cardId = it.toLong(), sideboard = true) }
        standardDeck(mainboard + sideboard)
    }

    @Test
    fun `blank name throws InvalidDeckException`() {
        val entries = (1..60).map { entry(cardId = it.toLong()) }
        assertFailsWith<InvalidDeckException> {
            Deck(
                name = "  ",
                format = DeckFormat.STANDARD,
                colorIdentity = emptyList(),
                comment = null,
                entries = entries,
            )
        }
    }

    @Test
    fun `deck with 4 copies of same card in quantity field passes validation`() {
        val entries = listOf(
            entry(cardId = 1L, quantity = 4),
        ) + (2..57).map { entry(cardId = it.toLong()) }
        standardDeck(entries)
    }
}
