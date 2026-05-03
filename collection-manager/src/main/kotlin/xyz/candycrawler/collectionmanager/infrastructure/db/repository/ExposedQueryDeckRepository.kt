package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckFormat
import xyz.candycrawler.collectionmanager.domain.deck.model.DeckHeader
import xyz.candycrawler.collectionmanager.domain.deck.repository.QueryDeckRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.DeckSqlMapper

@Repository
@Transactional(readOnly = true)
class ExposedQueryDeckRepository(private val sqlMapper: DeckSqlMapper) : QueryDeckRepository {

    override fun findHeaders(userId: Long): List<DeckHeader> = sqlMapper.selectAllByUser(userId).map { record ->
        DeckHeader(
            id = record.id!!,
            name = record.name,
            format = DeckFormat.valueOf(record.format),
            colorIdentity = record.colorIdentity,
            comment = record.comment,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )
    }
}
