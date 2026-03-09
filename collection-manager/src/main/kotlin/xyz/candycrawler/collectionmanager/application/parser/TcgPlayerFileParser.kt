package xyz.candycrawler.collectionmanager.application.parser

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.application.parser.dto.ParsedCollectionEntry

@Component
class TcgPlayerFileParser : CollectionFileParser {

    override fun parse(content: String): List<ParsedCollectionEntry> =
        content.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> LINE_PATTERN.matchEntire(line.trim())?.toEntry() }

    private fun MatchResult.toEntry() = ParsedCollectionEntry(
        quantity = groupValues[1].toInt(),
        setCode = groupValues[3].lowercase(),
        collectorNumber = groupValues[4],
        foil = false,
    )

    companion object {
        private val LINE_PATTERN = Regex("""^(\d+)\s+(.+?)\s+\[(\w+)]\s+(\d+)$""")
    }
}
