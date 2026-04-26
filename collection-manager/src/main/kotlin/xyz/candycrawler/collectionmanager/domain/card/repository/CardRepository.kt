package xyz.candycrawler.collectionmanager.domain.card.repository

import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.model.CardPage
import xyz.candycrawler.collectionmanager.domain.card.model.CardSearchCriteria

interface CardRepository {
    fun saveAll(cards: List<Card>): List<Card>
    fun findById(id: Long): Card
    fun findByIds(ids: List<Long>): List<Card>
    fun findByNames(names: List<String>): List<Card>
    fun findBySetAndCollectorPairs(pairs: List<Pair<String, String>>): List<Card>
    fun findBySetCodeAndCollectorNumber(setCode: String, collectorNumber: String): Card?
    fun search(criteria: CardSearchCriteria): CardPage
    fun findByTribe(userId: Long, tribe: String): List<Card>
}
