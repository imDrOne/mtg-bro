package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import java.util.function.Function

@Component
class CollectionEntryRecordToCollectionEntryMapper : Function<CollectionEntryRecord, CollectionEntry> {

    override fun apply(record: CollectionEntryRecord): CollectionEntry = CollectionEntry(
        id = record.id,
        cardId = record.cardId,
        quantity = record.quantity,
        createdAt = record.createdAt,
        updatedAt = record.updatedAt,
    )
}
