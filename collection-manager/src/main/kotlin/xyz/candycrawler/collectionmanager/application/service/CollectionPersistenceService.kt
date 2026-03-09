package xyz.candycrawler.collectionmanager.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.domain.collection.repository.CollectionEntryRepository

@Service
class CollectionPersistenceService(
    private val cardRepository: CardRepository,
    private val collectionEntryRepository: CollectionEntryRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveImportedData(
        cards: List<Card>,
        quantityByKey: Map<Pair<String, String>, Int>,
    ): Int {
        val savedCards = cardRepository.saveAll(cards)
        log.info("Upserted {} cards into DB", savedCards.size)

        val cardIdByKey = savedCards.associateBy(
            { it.setCode to it.collectorNumber },
            { it.id!! }
        )

        val collectionEntries = quantityByKey.mapNotNull { (key, quantity) ->
            cardIdByKey[key]?.let { cardId ->
                CollectionEntry(cardId = cardId, quantity = quantity)
            }
        }

        collectionEntryRepository.saveAll(collectionEntries)
        log.info("Upserted {} collection entries", collectionEntries.size)

        return collectionEntries.size
    }
}
