package xyz.candycrawler.collectionmanager.application.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.candycrawler.collectionmanager.application.parser.TcgPlayerFileParser
import xyz.candycrawler.collectionmanager.application.parser.dto.TcgPlayerEntry
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.CardIdentifier
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.ScryfallCollectionRequest
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallCollectionResponse
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.mapper.ScryfallCardResponseToCardMapper

@Service
class TcgPlayerImportService(
    private val parser: TcgPlayerFileParser,
    private val scryfallApiClient: ScryfallApiClient,
    private val scryfallMapper: ScryfallCardResponseToCardMapper,
    private val persistenceService: CollectionPersistenceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun import(fileContent: String): ImportResult {
        val entries = parser.parse(fileContent)
        log.info("Parsed {} lines from TCG Player file", entries.size)

        val merged = mergeEntries(entries)
        log.info("Merged into {} unique cards", merged.size)

        val responses = fetchFromScryfall(merged.keys)

        val cards = responses.flatMap { it.data.map(scryfallMapper::apply) }
        val notFound = responses.flatMap { it.notFound }

        if (notFound.isNotEmpty()) {
            log.warn("Scryfall did not find {} cards: {}", notFound.size, notFound)
        }

        val importedCount = withContext(Dispatchers.IO) {
            persistenceService.saveImportedData(cards, merged)
        }

        return ImportResult(
            importedCount = importedCount,
            notFound = notFound,
        )
    }

    private suspend fun fetchFromScryfall(
        keys: Set<Pair<String, String>>,
    ): List<ScryfallCollectionResponse> = coroutineScope {
        keys.chunked(SCRYFALL_BATCH_SIZE).map { batch ->
            async(Dispatchers.IO) {
                val request = ScryfallCollectionRequest(
                    identifiers = batch.map { CardIdentifier(set = it.first, collectorNumber = it.second) }
                )
                scryfallApiClient.fetchCollection(request)
            }
        }.awaitAll()
    }

    private fun mergeEntries(entries: List<TcgPlayerEntry>): Map<Pair<String, String>, Int> =
        entries.groupBy { it.setCode to it.collectorNumber }
            .mapValues { (_, group) -> group.sumOf { it.quantity } }

    data class ImportResult(
        val importedCount: Int,
        val notFound: List<CardIdentifier>,
    )

    companion object {
        private const val SCRYFALL_BATCH_SIZE = 75
    }
}
