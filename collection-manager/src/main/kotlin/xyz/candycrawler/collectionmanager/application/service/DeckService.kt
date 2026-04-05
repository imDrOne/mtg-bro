package xyz.candycrawler.collectionmanager.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.exception.CardNotFoundException
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.domain.deck.model.Deck
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckEntry
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckFormat
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckHeader
import xyz.candycrawler.collectionmanager.domain.deck.repository.DeckRepository
import xyz.candycrawler.collectionmanager.domain.deck.repository.QueryDeckRepository

@Service
class DeckService(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val queryDeckRepository: QueryDeckRepository,
) {

    @Transactional
    fun save(
        name: String,
        format: DeckFormat,
        comment: String?,
        mainboard: List<Triple<String, String, Int>>,
        sideboard: List<Triple<String, String, Int>>,
    ): Deck {
        val allPairs = (mainboard + sideboard).map { it.first to it.second }.distinct()
        val cards = cardRepository.findBySetAndCollectorPairs(allPairs)
        val cardMap = cards.associateBy { it.setCode to it.collectorNumber }

        val missingPairs = allPairs.filter { it !in cardMap.keys }
        if (missingPairs.isNotEmpty()) {
            val (set, num) = missingPairs.first()
            throw CardNotFoundException("with setCode='$set' collectorNumber='$num' not found")
        }

        val entries = buildList {
            mainboard.forEach { (set, num, qty) ->
                val card = cardMap[set to num]!!
                add(DeckEntry(cardId = card.id!!, quantity = qty, isSideboard = false))
            }
            sideboard.forEach { (set, num, qty) ->
                val card = cardMap[set to num]!!
                add(DeckEntry(cardId = card.id!!, quantity = qty, isSideboard = true))
            }
        }

        val colorIdentity = mainboard
            .mapNotNull { (set, num, _) -> cardMap[set to num] }
            .flatMap { it.colorIdentity }
            .distinct()
            .sorted()

        val deck = Deck(
            name = name,
            format = format,
            colorIdentity = colorIdentity,
            comment = comment,
            entries = entries,
        )

        return deckRepository.save(deck)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Deck = deckRepository.findById(id)

    @Transactional(readOnly = true)
    fun findAll(): List<DeckHeader> = queryDeckRepository.findHeaders()
}
