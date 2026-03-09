package xyz.candycrawler.collectionmanager.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.application.parser.TcgPlayerFileParser
import xyz.candycrawler.collectionmanager.application.parser.dto.TcgPlayerEntry
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.domain.collection.repository.CollectionEntryRepository
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.CardIdentifier
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.ScryfallCollectionRequest
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.mapper.ScryfallCardResponseToCardMapper

@Service
class TcgPlayerImportService(
    private val parser: TcgPlayerFileParser,
    private val scryfallApiClient: ScryfallApiClient,
    private val scryfallMapper: ScryfallCardResponseToCardMapper,
    private val cardRepository: CardRepository,
    private val collectionEntryRepository: CollectionEntryRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun import(fileContent: String): ImportResult {
        val entries = parser.parse(fileContent)
        log.info("Parsed {} lines from TCG Player file", entries.size)

        val merged = mergeEntries(entries)
        log.info("Merged into {} unique cards", merged.size)

        val allCards = mutableListOf<Card>()
        val allNotFound = mutableListOf<CardIdentifier>()

        merged.keys.chunked(SCRYFALL_BATCH_SIZE).forEach { batch ->
            val request = ScryfallCollectionRequest(
                identifiers = batch.map { CardIdentifier(set = it.first, collectorNumber = it.second) }
            )
            val response = scryfallApiClient.fetchCollection(request)
            allCards.addAll(response.data.map(scryfallMapper::apply))
            allNotFound.addAll(response.notFound)
        }

        if (allNotFound.isNotEmpty()) {
            log.warn("Scryfall did not find {} cards: {}", allNotFound.size, allNotFound)
        }

        val savedCards = cardRepository.saveAll(allCards)
        log.info("Upserted {} cards into DB", savedCards.size)

        val cardIdByKey = savedCards.associateBy(
            { it.setCode to it.collectorNumber },
            { it.id!! }
        )

        val collectionEntries = merged.mapNotNull { (key, quantity) ->
            cardIdByKey[key]?.let { cardId ->
                CollectionEntry(cardId = cardId, quantity = quantity)
            }
        }

        collectionEntryRepository.saveAll(collectionEntries)
        log.info("Upserted {} collection entries", collectionEntries.size)

        return ImportResult(
            importedCount = collectionEntries.size,
            notFound = allNotFound,
        )
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
