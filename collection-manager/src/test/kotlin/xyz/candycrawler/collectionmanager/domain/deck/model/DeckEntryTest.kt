package xyz.candycrawler.collectionmanager.domain.deck.model

import org.junit.jupiter.api.Test
import xyz.candycrawler.collectionmanager.domain.deck.exception.InvalidDeckException
import kotlin.test.assertFailsWith

class DeckEntryTest {

    @Test
    fun `valid entry is created successfully`() {
        DeckEntry(cardId = 1L, quantity = 4, isSideboard = false)
    }

    @Test
    fun `cardId zero throws InvalidDeckException`() {
        assertFailsWith<InvalidDeckException> {
            DeckEntry(cardId = 0L, quantity = 1)
        }
    }

    @Test
    fun `negative cardId throws InvalidDeckException`() {
        assertFailsWith<InvalidDeckException> {
            DeckEntry(cardId = -5L, quantity = 1)
        }
    }

    @Test
    fun `quantity zero throws InvalidDeckException`() {
        assertFailsWith<InvalidDeckException> {
            DeckEntry(cardId = 1L, quantity = 0)
        }
    }

    @Test
    fun `negative quantity throws InvalidDeckException`() {
        assertFailsWith<InvalidDeckException> {
            DeckEntry(cardId = 1L, quantity = -1)
        }
    }

    @Test
    fun `quantity 5 throws InvalidDeckException`() {
        assertFailsWith<InvalidDeckException> {
            DeckEntry(cardId = 1L, quantity = 5)
        }
    }

    @Test
    fun `quantity exactly 4 is valid`() {
        DeckEntry(cardId = 1L, quantity = 4)
    }
}
