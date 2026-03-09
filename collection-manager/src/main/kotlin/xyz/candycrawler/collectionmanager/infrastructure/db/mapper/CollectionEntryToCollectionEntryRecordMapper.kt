package xyz.candycrawler.collectionmanager.infrastructure.db.mapper

import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.collection.model.CollectionEntry
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CollectionEntryRecord
import java.util.function.Function

@Component
class CollectionEntryToCollectionEntryRecordMapper : Function<CollectionEntry, CollectionEntryRecord> {

    override fun apply(entry: CollectionEntry): CollectionEntryRecord = CollectionEntryRecord(
        id = entry.id,
        cardId = entry.cardId,
        quantity = entry.quantity,
        foil = entry.foil,
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt,
    )
}
