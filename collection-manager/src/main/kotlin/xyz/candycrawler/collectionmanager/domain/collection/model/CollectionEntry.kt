package xyz.candycrawler.collectionmanager.domain.collection.model

import xyz.candycrawler.collectionmanager.domain.collection.exception.InvalidCollectionEntryException
import java.time.LocalDateTime

data class CollectionEntry(
    val id: Long? = null,
    val userId: Long,
    val cardId: Long,
    val quantity: Int,
    val foil: Boolean = false,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    init {
        fun invalid(reason: String): Nothing = throw InvalidCollectionEntryException(reason)

        if (userId <= 0) invalid("userId must be positive")
        if (cardId <= 0) invalid("cardId must be positive")
        if (quantity <= 0) invalid("quantity must be positive")
    }
}
