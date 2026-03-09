package xyz.candycrawler.collectionmanager.application.parser.dto

data class ParsedCollectionEntry(
    val quantity: Int,
    val setCode: String,
    val collectorNumber: String,
    val foil: Boolean,
)
