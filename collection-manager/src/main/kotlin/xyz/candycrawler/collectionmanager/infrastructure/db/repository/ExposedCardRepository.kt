package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.card.exception.CardNotFoundException
import xyz.candycrawler.collectionmanager.domain.card.model.Card
import xyz.candycrawler.collectionmanager.domain.card.model.CardPage
import xyz.candycrawler.collectionmanager.domain.card.model.CardSearchCriteria
import xyz.candycrawler.collectionmanager.domain.card.repository.CardRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardRecordToCardMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CardToCardRecordMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CardSqlMapper

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

    override fun findByIds(ids: List<Long>): List<Card> =
        sqlMapper.selectByIds(ids).map(toDomain::apply)

    override fun findByNames(names: List<String>): List<Card> =
        sqlMapper.selectByNames(names).map(toDomain::apply)

    override fun findBySetAndCollectorPairs(pairs: List<Pair<String, String>>): List<Card> =
        sqlMapper.selectBySetAndCollectorPairs(pairs).map(toDomain::apply)

    override fun findBySetCodeAndCollectorNumber(setCode: String, collectorNumber: String): Card? =
        sqlMapper.selectBySetCodeAndCollectorNumber(setCode, collectorNumber)?.let(toDomain::apply)

    @Transactional(readOnly = true)
    override fun findByTribe(userId: Long, tribe: String): List<Card> =
        sqlMapper.findByTribe(userId, tribe).map(toDomain::apply)

    @Transactional(readOnly = true)
    override fun search(criteria: CardSearchCriteria): CardPage {
        val offset = ((criteria.page - 1) * criteria.pageSize).toLong()

        val records = sqlMapper.search(
            queryText = criteria.query,
            setCode = criteria.setCode,
            collectorNumber = criteria.collectorNumber,
            colors = criteria.colors,
            colorIdentity = criteria.colorIdentity,
            type = criteria.type,
            rarity = criteria.rarity,
            order = criteria.order,
            direction = criteria.direction,
            limit = criteria.pageSize,
            offset = offset,
        )

        val totalCards = sqlMapper.countSearch(
            queryText = criteria.query,
            setCode = criteria.setCode,
            collectorNumber = criteria.collectorNumber,
            colors = criteria.colors,
            colorIdentity = criteria.colorIdentity,
            type = criteria.type,
            rarity = criteria.rarity,
        )

        return CardPage(
            cards = records.map(toDomain::apply),
            totalCards = totalCards,
            hasMore = offset + criteria.pageSize < totalCards,
            page = criteria.page,
            pageSize = criteria.pageSize,
        )
    }
}
