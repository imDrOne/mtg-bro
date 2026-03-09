package xyz.candycrawler.collectionmanager.application.parser

import xyz.candycrawler.collectionmanager.application.parser.dto.ParsedCollectionEntry

interface CollectionFileParser {
    fun parse(content: String): List<ParsedCollectionEntry>
}
