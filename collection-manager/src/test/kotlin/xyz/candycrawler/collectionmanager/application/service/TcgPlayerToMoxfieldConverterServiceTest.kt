package xyz.candycrawler.collectionmanager.application.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TcgPlayerToMoxfieldConverterServiceTest {

    private val service = TcgPlayerToMoxfieldConverterService()

    @Test
    fun `converts single line to moxfield csv`() {
        val result = service.convert("2 Eclipsed Elf [ECL] 218")

        val lines = result.lines()
        assertEquals("Count,Name,Edition,Condition,Language,Foil,Collector Number", lines[0])
        assertEquals("2,Eclipsed Elf,ECL,Near Mint,English,,218", lines[1])
    }

    @Test
    fun `converts multiple lines`() {
        val content = """
            2 Eclipsed Elf [ECL] 218
            1 Black Lotus [LEA] 232
        """.trimIndent()

        val lines = service.convert(content).lines()

        assertEquals(3, lines.size)
        assertEquals("2,Eclipsed Elf,ECL,Near Mint,English,,218", lines[1])
        assertEquals("1,Black Lotus,LEA,Near Mint,English,,232", lines[2])
    }

    @Test
    fun `uppercases set code in output`() {
        val result = service.convert("1 Some Card [mid] 001")

        assertTrue(result.contains(",MID,"))
    }

    @Test
    fun `merges duplicate entries by summing quantities`() {
        val content = """
            2 Eclipsed Elf [ECL] 218
            3 Eclipsed Elf [ECL] 218
        """.trimIndent()

        val lines = service.convert(content).lines()

        assertEquals(2, lines.size)
        assertEquals("5,Eclipsed Elf,ECL,Near Mint,English,,218", lines[1])
    }

    @Test
    fun `foil column is always empty`() {
        val result = service.convert("1 Lightning Bolt [M11] 149")

        val dataLine = result.lines()[1]
        val cols = dataLine.split(",")
        assertEquals(7, cols.size)
        assertEquals("", cols[5])
    }

    @Test
    fun `escapes card name containing comma`() {
        val result = service.convert("""1 "Last, First" [SET] 001""")

        val lines = result.lines()
        assertTrue(lines[1].startsWith("""1,"Last, First",SET"""), "Expected quoted name, got: ${lines[1]}")
    }

    @Test
    fun `skips lines that do not match the pattern`() {
        val content = """
            2 Eclipsed Elf [ECL] 218
            this is not valid
            1 Valid Card [DSK] 001
        """.trimIndent()

        val lines = service.convert(content).lines()

        assertEquals(3, lines.size)
    }

    @Test
    fun `skips blank lines`() {
        val content = "2 Eclipsed Elf [ECL] 218\n\n1 Black Lotus [LEA] 232\n"

        assertEquals(3, service.convert(content).lines().size)
    }

    @Test
    fun `returns only header for empty content`() {
        val result = service.convert("")

        assertEquals("Count,Name,Edition,Condition,Language,Foil,Collector Number", result.trim())
    }

    @Test
    fun `returns only header when no lines match the pattern`() {
        val result = service.convert("not valid\nalso not valid")

        assertEquals("Count,Name,Edition,Condition,Language,Foil,Collector Number", result.trim())
    }
}
