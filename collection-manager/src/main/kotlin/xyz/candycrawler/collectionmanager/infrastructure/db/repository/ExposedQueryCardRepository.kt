package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.QueryCardRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardRecordToCardMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper

@Repository
class ExposedQueryCardRepository(private val sqlMapper: CardSqlMapper, private val toDomain: CardRecordToCardMapper) :
    QueryCardRepository {

    @Transactional(readOnly = true)
    override fun findAllInCollection(userId: Long): List<Card> =
        sqlMapper.findAllInCollection(userId).map(toDomain::apply)
}
