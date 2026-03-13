package xyz.candycrawler.collectionmanager.application.service

import org.springframework.stereotype.Service

/**
 * Converts a TCG Player export (.txt) into a Moxfield "Haves" CSV.
 *
 * Input line format:  `<quantity> <card name> [<set code>] <collector number>`
 * Output CSV columns: Count, Name, Edition, Condition, Language, Foil, Collector Number
 *
 * Foil is left empty — TCG Player exports do not carry foil information.
 * Duplicate lines (same set + collector number) are merged by summing quantities.
 */
@Service
class TcgPlayerToMoxfieldConverterService {

    fun convert(content: String): String {
        val rows = content.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { LINE_PATTERN.matchEntire(it.trim())?.toRow() }
            .mergeByKey()

        return buildString {
            appendLine(HEADER)
            rows.forEach { appendLine(it) }
        }.trimEnd()
    }

    private fun MatchResult.toRow(): Row {
        val rawName = groupValues[2].trim()
        val name = rawName.removeSurrounding("\"")
        return Row(
            quantity = groupValues[1].toInt(),
            name = name,
            setCode = groupValues[3].uppercase(),
            collectorNumber = groupValues[4],
        )
    }

    private fun List<Row>.mergeByKey(): List<String> =
        groupBy { it.setCode to it.collectorNumber }
            .map { (_, group) ->
                val first = group.first()
                val totalQuantity = group.sumOf { it.quantity }
                first.copy(quantity = totalQuantity).toCsvLine()
            }

    private data class Row(
        val quantity: Int,
        val name: String,
        val setCode: String,
        val collectorNumber: String,
    ) {
        fun toCsvLine(): String =
            "${quantity},${escapeCsv(name)},${setCode},Near Mint,English,,${collectorNumber}"
    }

    companion object {
        private val LINE_PATTERN = Regex("""^(\d+)\s+(.+?)\s+\[(\w+)]\s+(\d+)$""")
        private const val HEADER = "Count,Name,Edition,Condition,Language,Foil,Collector Number"

        private fun escapeCsv(value: String): String =
            if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}
