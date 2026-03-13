package xyz.candycrawler.collectionmanager.application.parser

import org.junit.jupiter.api.Test
import xyz.candycrawler.collectionmanager.application.parser.dto.ParsedCollectionEntry
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TcgPlayerFileParserTest {

    private val parser = TcgPlayerFileParser()

    @Test
    fun `parses single valid line`() {
        val result = parser.parse("2 Eclipsed Elf [ECL] 218")

        assertEquals(1, result.size)
        assertEquals(
            ParsedCollectionEntry(quantity = 2, setCode = "ecl", collectorNumber = "218", foil = false),
            result.single(),
        )
    }

    @Test
    fun `parses multiple valid lines`() {
        val content = """
            2 Eclipsed Elf [ECL] 218
            1 Black Lotus [LEA] 232
            4 Lightning Bolt [M11] 149
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(3, result.size)
        assertEquals(ParsedCollectionEntry(2, "ecl", "218", false), result[0])
        assertEquals(ParsedCollectionEntry(1, "lea", "232", false), result[1])
        assertEquals(ParsedCollectionEntry(4, "m11", "149", false), result[2])
    }

    @Test
    fun `lowercases set code`() {
        val result = parser.parse("1 Some Card [MID] 001")

        assertEquals("mid", result.single().setCode)
    }

    @Test
    fun `foil is always false`() {
        val result = parser.parse("1 Some Card [MID] 001")

        assertEquals(false, result.single().foil)
    }

    @Test
    fun `parses card name with multiple spaces`() {
        val result = parser.parse("3 Jace the Mind Sculptor [WWK] 31")

        assertEquals(
            ParsedCollectionEntry(quantity = 3, setCode = "wwk", collectorNumber = "31", foil = false),
            result.single(),
        )
    }

    @Test
    fun `skips blank lines`() {
        val content = """
            2 Eclipsed Elf [ECL] 218

            1 Black Lotus [LEA] 232

        """.trimIndent()

        assertEquals(2, parser.parse(content).size)
    }

    @Test
    fun `skips lines that do not match the pattern`() {
        val content = """
            2 Eclipsed Elf [ECL] 218
            this is not valid
            also not valid [ABC]
            1 Valid Card [DSK] 001
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(2, result.size)
        assertEquals("ecl", result[0].setCode)
        assertEquals("dsk", result[1].setCode)
    }

    @Test
    fun `trims leading and trailing whitespace from lines`() {
        val result = parser.parse("   2 Eclipsed Elf [ECL] 218   ")

        assertEquals(1, result.size)
    }

    @Test
    fun `returns empty list for empty content`() {
        assertTrue(parser.parse("").isEmpty())
    }

    @Test
    fun `returns empty list for blank-only content`() {
        assertTrue(parser.parse("   \n   \n   ").isEmpty())
    }

    @Test
    fun `returns empty list when no lines match the pattern`() {
        val content = """
            not a valid line
            also invalid
        """.trimIndent()

        assertTrue(parser.parse(content).isEmpty())
    }
}
