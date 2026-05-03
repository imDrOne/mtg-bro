package xyz.candycrawler.collectionmanager.application.parser

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.application.parser.dto.ParsedCollectionEntry

/**
 * Parses Moxfield "Haves" CSV export.
 *
 * Expected header (order may vary):
 *   Count,Name,Edition,Condition,Language,Foil,Collector Number,...
 *
 * The "Foil" column contains "foil" when the card is a foil copy, and is empty otherwise.
 */
@Component
class MoxfieldFileParser : CollectionFileParser {

    override fun parse(content: String): List<ParsedCollectionEntry> {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headers = parseCsvLine(lines.first())
        val countIdx = headers.indexOf("Count")
        val editionIdx = headers.indexOf("Edition")
        val collectorNumberIdx = headers.indexOf("Collector Number")
        val foilIdx = headers.indexOf("Foil")

        require(countIdx >= 0) { "Missing required column: Count" }
        require(editionIdx >= 0) { "Missing required column: Edition" }
        require(collectorNumberIdx >= 0) { "Missing required column: Collector Number" }

        return lines.drop(1).mapNotNull { line ->
            val cols = parseCsvLine(line)
            if (cols.size <= maxOf(countIdx, editionIdx, collectorNumberIdx)) return@mapNotNull null

            val quantity = cols[countIdx].toIntOrNull() ?: return@mapNotNull null
            val foil = foilIdx >= 0 && cols.getOrNull(foilIdx).equals("foil", ignoreCase = true)

            ParsedCollectionEntry(
                quantity = quantity,
                setCode = cols[editionIdx].lowercase(),
                collectorNumber = cols[collectorNumberIdx],
                foil = foil,
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes

                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }

                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
