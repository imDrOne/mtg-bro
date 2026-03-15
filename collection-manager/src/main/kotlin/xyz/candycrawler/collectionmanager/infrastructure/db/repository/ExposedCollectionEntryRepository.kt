package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.domain.collection.repository.CollectionEntryRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryRecordToCollectionEntryMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CollectionEntrySqlMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryToCollectionEntryRecordMapper

@Repository
@Transactional
class ExposedCollectionEntryRepository(
    private val sqlMapper: CollectionEntrySqlMapper,
    private val toRecord: CollectionEntryToCollectionEntryRecordMapper,
    private val toDomain: CollectionEntryRecordToCollectionEntryMapper,
) : CollectionEntryRepository {

    override fun saveAll(entries: List<CollectionEntry>) {
        sqlMapper.upsertBatch(entries.map(toRecord::apply))
    }

    override fun findByCardId(cardId: Long): CollectionEntry? =
        sqlMapper.selectByCardId(cardId)?.let(toDomain::apply)

    override fun findByCardIds(cardIds: List<Long>): List<CollectionEntry> =
        sqlMapper.selectByCardIds(cardIds).map(toDomain::apply)

    override fun findAll(): List<CollectionEntry> =
        sqlMapper.selectAll().map(toDomain::apply)
}
