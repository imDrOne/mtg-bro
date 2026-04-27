package xyz.candycrawler.collectionmanager.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.model.CardSearchCriteria
import xyz.candycrawler.collectionmanager.domain.card.model.CardWithCollectionPage
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository

@Service
@Transactional(readOnly = true)
class CardSearchService(private val cardRepository: CardRepository) {

    fun searchByUser(userId: Long, criteria: CardSearchCriteria): CardWithCollectionPage =
        cardRepository.searchByUser(userId, criteria)
}
