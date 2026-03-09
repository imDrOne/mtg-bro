package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.exception.CardNotFoundException
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardRecordToCardMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardToCardRecordMapper

@Repository
@Transactional
class ExposedCardRepository(
    private val sqlMapper: CardSqlMapper,
    private val toRecord: CardToCardRecordMapper,
    private val toDomain: CardRecordToCardMapper,
) : CardRepository {

    override fun saveAll(cards: List<Card>): List<Card> =
        sqlMapper.upsertBatch(cards.map(toRecord::apply)).map(toDomain::apply)

    override fun findById(id: Long): Card =
        sqlMapper.selectById(id)?.let(toDomain::apply)
            ?: throw CardNotFoundException(id)

    override fun findBySetCodeAndCollectorNumber(setCode: String, collectorNumber: String): Card? =
        sqlMapper.selectBySetCodeAndCollectorNumber(setCode, collectorNumber)?.let(toDomain::apply)
}
