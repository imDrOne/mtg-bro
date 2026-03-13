package xyz.candycrawler.collectionmanager.application.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.candycrawler.collectionmanager.application.parser.dto.ParsedCollectionEntry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoxfieldFileParserTest {

    private val parser = MoxfieldFileParser()

    @Test
    fun `parses single non-foil entry`() {
        val content = """
            Count,Name,Edition,Condition,Language,Foil,Collector Number
            2,Eclipsed Elf,ECL,Near Mint,English,,218
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(1, result.size)
        assertEquals(
            ParsedCollectionEntry(quantity = 2, setCode = "ecl", collectorNumber = "218", foil = false),
            result.single(),
        )
    }

    @Test
    fun `parses foil entry when Foil column contains foil`() {
        val content = """
            Count,Name,Edition,Condition,Language,Foil,Collector Number
            1,Black Lotus,LEA,Near Mint,English,foil,232
        """.trimIndent()

        assertTrue(parser.parse(content).single().foil)
    }

    @Test
    fun `foil detection is case-insensitive`() {
        val content = """
            Count,Name,Edition,Condition,Language,Foil,Collector Number
            1,Black Lotus,LEA,Near Mint,English,FOIL,232
        """.trimIndent()

        assertTrue(parser.parse(content).single().foil)
    }

    @Test
    fun `non-foil entry has foil=false when Foil column is empty`() {
        val content = """
            Count,Name,Edition,Condition,Language,Foil,Collector Number
            2,Lightning Bolt,M11,Near Mint,English,,149
        """.trimIndent()

        assertFalse(parser.parse(content).single().foil)
    }

    @Test
    fun `foil defaults to false when Foil column is absent`() {
        val content = """
            Count,Name,Edition,Collector Number
            2,Lightning Bolt,M11,149
        """.trimIndent()

        assertFalse(parser.parse(content).single().foil)
    }

    @Test
    fun `lowercases set code`() {
        val content = """
            Count,Name,Edition,Collector Number
            1,Some Card,MID,001
        """.trimIndent()

        assertEquals("mid", parser.parse(content).single().setCode)
    }

    @Test
    fun `column order in header does not matter`() {
        val content = """
            Collector Number,Foil,Edition,Name,Count
            218,foil,ECL,Eclipsed Elf,3
        """.trimIndent()

        assertEquals(
            ParsedCollectionEntry(quantity = 3, setCode = "ecl", collectorNumber = "218", foil = true),
            parser.parse(content).single(),
        )
    }

    @Test
    fun `parses multiple entries`() {
        val content = """
            Count,Name,Edition,Collector Number,Foil
            2,Eclipsed Elf,ECL,218,
            1,Black Lotus,LEA,232,foil
            4,Lightning Bolt,M11,149,
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(3, result.size)
        assertFalse(result[0].foil)
        assertTrue(result[1].foil)
        assertFalse(result[2].foil)
    }

    @Test
    fun `parses quoted field containing comma`() {
        val content = """
            Count,Name,Edition,Collector Number
            1,"Last, First",SET,001
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(1, result.size)
        assertEquals("set", result.single().setCode)
        assertEquals("001", result.single().collectorNumber)
    }

    @Test
    fun `skips rows with too few columns`() {
        val content = """
            Count,Name,Edition,Collector Number
            2,Incomplete Row
        """.trimIndent()

        assertTrue(parser.parse(content).isEmpty())
    }

    @Test
    fun `skips rows where Count is not a number`() {
        val content = """
            Count,Name,Edition,Collector Number
            N/A,Some Card,SET,001
        """.trimIndent()

        assertTrue(parser.parse(content).isEmpty())
    }

    @Test
    fun `skips blank lines between data rows`() {
        val content = "Count,Name,Edition,Collector Number\n2,Card A,SET,001\n\n1,Card B,SET,002\n"

        assertEquals(2, parser.parse(content).size)
    }

    @Test
    fun `returns empty list for empty content`() {
        assertTrue(parser.parse("").isEmpty())
    }

    @Test
    fun `returns empty list for header-only content`() {
        val content = "Count,Name,Edition,Collector Number"

        assertTrue(parser.parse(content).isEmpty())
    }

    @Test
    fun `throws when Count column is missing`() {
        val content = """
            Name,Edition,Collector Number
            Eclipsed Elf,ECL,218
        """.trimIndent()

        assertThrows<IllegalArgumentException> { parser.parse(content) }
    }

    @Test
    fun `throws when Edition column is missing`() {
        val content = """
            Count,Name,Collector Number
            2,Eclipsed Elf,218
        """.trimIndent()

        assertThrows<IllegalArgumentException> { parser.parse(content) }
    }

    @Test
    fun `throws when Collector Number column is missing`() {
        val content = """
            Count,Name,Edition
            2,Eclipsed Elf,ECL
        """.trimIndent()

        assertThrows<IllegalArgumentException> { parser.parse(content) }
    }
}
