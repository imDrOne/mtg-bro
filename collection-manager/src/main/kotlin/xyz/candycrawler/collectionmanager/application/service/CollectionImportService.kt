package xyz.candycrawler.collectionmanager.application.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.candycrawler.collectionmanager.application.parser.CollectionFileParser
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.mapper.ScryfallCardResponseToCardMapper
import xyz.candycrawler.scryfall.client.ScryfallApiClient
import xyz.candycrawler.scryfall.dto.request.ScryfallCardIdentifier
import xyz.candycrawler.scryfall.dto.request.ScryfallCollectionRequest
import xyz.candycrawler.scryfall.dto.response.ScryfallCollectionResponse

@Service
class CollectionImportService(
    private val scryfallApiClient: ScryfallApiClient,
    private val scryfallMapper: ScryfallCardResponseToCardMapper,
    private val persistenceService: CollectionPersistenceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun import(userId: Long, parser: CollectionFileParser, content: String): ImportResult {
        val entries = parser.parse(content)
        log.info("Parsed {} lines from file", entries.size)

        val merged = entries
            .groupBy { Triple(it.setCode, it.collectorNumber, it.foil) }
            .mapValues { (_, group) -> group.sumOf { it.quantity } }
        log.info("Merged into {} unique entries", merged.size)

        val cardKeys = merged.keys.map { it.first to it.second }.toSet()
        val responses = fetchFromScryfall(cardKeys)

        val cards = responses.flatMap { it.data.map(scryfallMapper::apply) }
        val notFound = responses.flatMap { it.notFound }

        if (notFound.isNotEmpty()) {
            log.warn("Scryfall did not find {} cards: {}", notFound.size, notFound)
        }

        val importedCount = withContext(Dispatchers.IO) {
            persistenceService.saveImportedData(userId, cards, merged)
        }

        return ImportResult(importedCount = importedCount, notFound = notFound)
    }

    private suspend fun fetchFromScryfall(keys: Set<Pair<String, String>>): List<ScryfallCollectionResponse> =
        coroutineScope {
            keys.chunked(SCRYFALL_BATCH_SIZE).map { batch ->
                async(Dispatchers.IO) {
                    val request = ScryfallCollectionRequest(
                        identifiers = batch.map {
                            ScryfallCardIdentifier(set = it.first, collectorNumber = it.second, id = null)
                        },
                    )
                    scryfallApiClient.fetchCollection(request)
                }
            }.awaitAll()
        }

    data class ImportResult(val importedCount: Int, val notFound: List<ScryfallCardIdentifier>)

    companion object {
        private const val SCRYFALL_BATCH_SIZE = 75
    }
}
