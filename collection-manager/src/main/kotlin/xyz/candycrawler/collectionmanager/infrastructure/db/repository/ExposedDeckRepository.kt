package xyz.candycrawler.collectionmanager.infrastructure.db.repository

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.collectionmanager.domain.deck.exception.DeckNotFoundException
import xyz.candycrawler.collectionmanager.domain.deck.model.Deck
import xyz.candycrawler.collectionmanager.domain.deck.repository.DeckRepository
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckEntryRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.DeckRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.DeckRecordToDeckMapper
import xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql.DeckSqlMapper

@Repository
@Transactional
class ExposedDeckRepository(private val sqlMapper: DeckSqlMapper, private val toDomain: DeckRecordToDeckMapper) :
    DeckRepository {

    override fun save(deck: Deck): Deck {
        val record = DeckRecord(
            id = null,
            userId = deck.userId,
            name = deck.name,
            format = deck.format.name,
            colorIdentity = deck.colorIdentity,
            comment = deck.comment,
            createdAt = null,
            updatedAt = null,
        )
        val saved = sqlMapper.insert(record)
        val deckId = saved.id!!

        val entryRecords = deck.entries.map { e ->
            DeckEntryRecord(
                id = null,
                userId = deck.userId,
                deckId = deckId,
                cardId = e.cardId,
                quantity = e.quantity,
                isSideboard = e.isSideboard,
            )
        }
        sqlMapper.insertEntries(entryRecords)

        val savedEntries = sqlMapper.selectEntriesByDeckId(deckId)
        return toDomain.apply(saved, savedEntries)
    }

    override fun findByIdAndUser(id: Long, userId: Long): Deck {
        val record = sqlMapper.selectByIdAndUser(id, userId) ?: throw DeckNotFoundException(id)
        val entries = sqlMapper.selectEntriesByDeckId(id)
        return toDomain.apply(record, entries)
    }
}
