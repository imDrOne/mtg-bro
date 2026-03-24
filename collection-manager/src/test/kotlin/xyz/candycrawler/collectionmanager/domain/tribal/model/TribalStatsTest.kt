package xyz.candycrawler.collectionmanager.domain.tribal.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TribalStatsTest {

    @Test
    fun `can be constructed with valid values`() {
        val stats = TribalStats(
            tribe = "Merfolk",
            totalCards = 10,
            byCmc = mapOf("1" to 3, "2" to 5, "5+" to 2),
            creatures = 8,
            tribalSpells = 1,
            tribalSupport = 1,
            colorSpread = mapOf("U" to 8, "WU" to 2),
            hasLord = true,
            hasCommander = true,
            deckViability = "moderate",
        )

        assertEquals("Merfolk", stats.tribe)
        assertEquals(10, stats.totalCards)
        assertEquals(8, stats.creatures)
        assertEquals(1, stats.tribalSpells)
        assertEquals(1, stats.tribalSupport)
        assertEquals(true, stats.hasLord)
        assertEquals(true, stats.hasCommander)
        assertEquals("moderate", stats.deckViability)
    }

    @Test
    fun `blank tribe throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            TribalStats(
                tribe = "  ",
                totalCards = 0,
                byCmc = emptyMap(),
                creatures = 0,
                tribalSpells = 0,
                tribalSupport = 0,
                colorSpread = emptyMap(),
                hasLord = false,
                hasCommander = false,
                deckViability = "weak",
            )
        }
    }

    @Test
    fun `empty tribe throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            TribalStats(
                tribe = "",
                totalCards = 0,
                byCmc = emptyMap(),
                creatures = 0,
                tribalSpells = 0,
                tribalSupport = 0,
                colorSpread = emptyMap(),
                hasLord = false,
                hasCommander = false,
                deckViability = "weak",
            )
        }
    }

    @Test
    fun `negative totalCards throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            TribalStats(
                tribe = "Elf",
                totalCards = -1,
                byCmc = emptyMap(),
                creatures = 0,
                tribalSpells = 0,
                tribalSupport = 0,
                colorSpread = emptyMap(),
                hasLord = false,
                hasCommander = false,
                deckViability = "weak",
            )
        }
    }

    @Test
    fun `zero totalCards is valid`() {
        val stats = TribalStats(
            tribe = "Goblin",
            totalCards = 0,
            byCmc = emptyMap(),
            creatures = 0,
            tribalSpells = 0,
            tribalSupport = 0,
            colorSpread = emptyMap(),
            hasLord = false,
            hasCommander = false,
            deckViability = "weak",
        )

        assertEquals(0, stats.totalCards)
    }
}
