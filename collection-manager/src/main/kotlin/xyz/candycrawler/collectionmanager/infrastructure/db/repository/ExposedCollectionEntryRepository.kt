package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.domain.collection.repository.CollectionEntryRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryRecordToCollectionEntryMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.CollectionEntryToCollectionEntryRecordMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.CollectionEntrySqlMapper

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

    override fun findByUserAndCardId(userId: Long, cardId: Long): List<CollectionEntry> =
        sqlMapper.selectByUserAndCardId(userId, cardId).map(toDomain::apply)

    override fun findByUserAndCardIds(userId: Long, cardIds: List<Long>): List<CollectionEntry> =
        sqlMapper.selectByUserAndCardIds(userId, cardIds).map(toDomain::apply)

    override fun findByUser(userId: Long): List<CollectionEntry> =
        sqlMapper.selectByUser(userId).map(toDomain::apply)
}
