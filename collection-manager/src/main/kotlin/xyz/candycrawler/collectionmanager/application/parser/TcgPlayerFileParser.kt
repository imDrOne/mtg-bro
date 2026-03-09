package xyz.candycrawler.collectionmanager.application.parser

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.application.parser.dto.TcgPlayerEntry

@Component
class TcgPlayerFileParser {

    fun parse(content: String): List<TcgPlayerEntry> =
        content.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> LINE_PATTERN.matchEntire(line.trim())?.toEntry() }

    private fun MatchResult.toEntry() = TcgPlayerEntry(
        quantity = groupValues[1].toInt(),
        name = groupValues[2].trim(),
        setCode = groupValues[3].lowercase(),
        collectorNumber = groupValues[4],
    )

    companion object {
        private val LINE_PATTERN = Regex("""^(\d+)\s+(.+?)\s+\[(\w+)]\s+(\d+)$""")
    }
}
