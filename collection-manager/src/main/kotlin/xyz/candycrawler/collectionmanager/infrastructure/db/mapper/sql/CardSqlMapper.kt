package xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.anyFrom
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component
import xyz.candycrawler.collectionmanager.domain.card.model.CardSortOrder
import xyz.candycrawler.collectionmanager.infrastructure.db.table.CollectionEntriesTable
import xyz.candycrawler.collectionmanager.domain.card.model.SortDirection
import xyz.candycrawler.collectionmanager.infrastructure.db.entity.CardRecord
import xyz.candycrawler.collectionmanager.infrastructure.db.table.CardsTable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Component
class CardSqlMapper {

    internal fun upsertBatch(records: List<CardRecord>): List<CardRecord> {
        return CardsTable.batchUpsert(
            records,
            keys = arrayOf(CardsTable.setCode, CardsTable.collectorNumber, CardsTable.lang),
            shouldReturnGeneratedValues = true,
        ) { record ->
            this[CardsTable.scryfallId] = Uuid.parse(record.scryfallId.toString())
            this[CardsTable.oracleId] = Uuid.parse(record.oracleId.toString())
            this[CardsTable.name] = record.name
            this[CardsTable.lang] = record.lang
            this[CardsTable.layout] = record.layout
            this[CardsTable.manaCost] = record.manaCost
            this[CardsTable.cmc] = record.cmc
            this[CardsTable.typeLine] = record.typeLine
            this[CardsTable.oracleText] = record.oracleText
            this[CardsTable.colors] = record.colors
            this[CardsTable.colorIdentity] = record.colorIdentity
            this[CardsTable.keywords] = record.keywords
            this[CardsTable.power] = record.power
            this[CardsTable.toughness] = record.toughness
            this[CardsTable.loyalty] = record.loyalty
            this[CardsTable.setCode] = record.setCode
            this[CardsTable.setName] = record.setName
            this[CardsTable.collectorNumber] = record.collectorNumber
            this[CardsTable.rarity] = record.rarity
            this[CardsTable.releasedAt] = record.releasedAt
            this[CardsTable.imageUriSmall] = record.imageUriSmall
            this[CardsTable.imageUriNormal] = record.imageUriNormal
            this[CardsTable.imageUriLarge] = record.imageUriLarge
            this[CardsTable.imageUriPng] = record.imageUriPng
            this[CardsTable.imageUriArtCrop] = record.imageUriArtCrop
            this[CardsTable.imageUriBorderCrop] = record.imageUriBorderCrop
            this[CardsTable.priceUsd] = record.priceUsd
            this[CardsTable.priceUsdFoil] = record.priceUsdFoil
            this[CardsTable.priceEur] = record.priceEur
            this[CardsTable.priceEurFoil] = record.priceEurFoil
            this[CardsTable.flavorText] = record.flavorText
            this[CardsTable.artist] = record.artist
            this[CardsTable.mtgaId] = record.mtgaId
        }.map { it.toRecord() }
    }

    internal fun selectById(id: Long): CardRecord? =
        CardsTable.selectAll()
            .where { CardsTable.id eq id }
            .map { it.toRecord() }
            .singleOrNull()

    internal fun selectByIds(ids: List<Long>): List<CardRecord> =
        if (ids.isEmpty()) emptyList()
        else CardsTable.selectAll()
            .where { CardsTable.id inList ids }
            .map { it.toRecord() }

    internal fun selectByNames(names: List<String>): List<CardRecord> {
        if (names.isEmpty()) return emptyList()
        val lowerNames = names.map { it.lowercase() }
        return CardsTable.selectAll()
            .where { CardsTable.name.lowerCase() inList lowerNames }
            .orderBy(CardsTable.id to SortOrder.DESC)
            .map { it.toRecord() }
            .distinctBy { it.name.lowercase() }
    }

    internal fun selectBySetAndCollectorPairs(pairs: List<Pair<String, String>>): List<CardRecord> {
        if (pairs.isEmpty()) return emptyList()
        val condition = pairs
            .map { (set, num) -> (CardsTable.setCode eq set) and (CardsTable.collectorNumber eq num) }
            .reduce { acc, op -> acc or op }
        return CardsTable.selectAll().where { condition }.map { it.toRecord() }
    }

    internal fun selectBySetCodeAndCollectorNumber(setCode: String, collectorNumber: String): CardRecord? =
        CardsTable.selectAll()
            .where {
                (CardsTable.setCode eq setCode) and
                (CardsTable.collectorNumber eq collectorNumber)
            }
            .map { it.toRecord() }
            .singleOrNull()

    internal fun search(
        queryText: String?,
        setCode: String?,
        collectorNumber: String?,
        colors: List<String>?,
        colorIdentity: List<String>?,
        type: String?,
        rarity: String?,
        order: CardSortOrder,
        direction: SortDirection,
        limit: Int,
        offset: Long,
    ): List<CardRecord> {
        val query = CardsTable.selectAll()

        buildSearchCondition(queryText, setCode, collectorNumber, colors, colorIdentity, type, rarity)?.let { condition ->
            query.where { condition }
        }

        val sortColumn = resolveOrderColumn(order)
        val sortOrder = resolveDirection(order, direction)

        return query
            .orderBy(sortColumn to sortOrder)
            .limit(limit)
            .offset(offset)
            .map { it.toRecord() }
    }

    internal fun findAllInCollection(userId: Long): List<CardRecord> =
        (CardsTable innerJoin CollectionEntriesTable)
            .selectAll()
            .where { CollectionEntriesTable.userId eq userId }
            .map { it.toRecord() }
            .distinctBy { it.id }

    internal fun searchByUser(
        userId: Long,
        queryText: String?,
        setCode: String?,
        collectorNumber: String?,
        colors: List<String>?,
        colorIdentity: List<String>?,
        type: String?,
        rarity: String?,
        order: CardSortOrder,
        direction: SortDirection,
        limit: Int,
        offset: Long,
    ): List<CardRecord> {
        val userCondition = CollectionEntriesTable.userId eq userId
        val searchCondition = buildSearchCondition(queryText, setCode, collectorNumber, colors, colorIdentity, type, rarity)
        val condition = searchCondition?.let { userCondition and it } ?: userCondition

        val sortColumn = resolveOrderColumn(order)
        val sortOrder = resolveDirection(order, direction)

        val allIds = (CardsTable innerJoin CollectionEntriesTable)
            .selectAll()
            .where { condition }
            .orderBy(sortColumn to sortOrder)
            .map { it[CardsTable.id].value }
            .distinct()

        val pageIds = allIds.drop(offset.toInt()).take(limit)
        if (pageIds.isEmpty()) return emptyList()

        val byId = CardsTable.selectAll()
            .where { CardsTable.id inList pageIds }
            .map { it.toRecord() }
            .associateBy { it.id }

        return pageIds.mapNotNull { byId[it] }
    }

    internal fun countSearchByUser(
        userId: Long,
        queryText: String?,
        setCode: String?,
        collectorNumber: String?,
        colors: List<String>?,
        colorIdentity: List<String>?,
        type: String?,
        rarity: String?,
    ): Long {
        val userCondition = CollectionEntriesTable.userId eq userId
        val searchCondition = buildSearchCondition(queryText, setCode, collectorNumber, colors, colorIdentity, type, rarity)
        val condition = searchCondition?.let { userCondition and it } ?: userCondition

        return (CardsTable innerJoin CollectionEntriesTable)
            .selectAll()
            .where { condition }
            .map { it[CardsTable.id].value }
            .distinct()
            .size
            .toLong()
    }

    internal fun findByTribe(userId: Long, tribe: String): List<CardRecord> {
        val pattern = "%${tribe.lowercase()}%"
        return (CardsTable innerJoin CollectionEntriesTable)
            .selectAll()
            .where {
                (CollectionEntriesTable.userId eq userId) and
                ((CardsTable.typeLine.lowerCase() like pattern) or
                (CardsTable.oracleText.lowerCase() like pattern))
            }
            .map { it.toRecord() }
            .distinctBy { it.id }
    }

    internal fun countSearch(
        queryText: String?,
        setCode: String?,
        collectorNumber: String?,
        colors: List<String>?,
        colorIdentity: List<String>?,
        type: String?,
        rarity: String?,
    ): Long {
        val query = CardsTable.selectAll()

        buildSearchCondition(queryText, setCode, collectorNumber, colors, colorIdentity, type, rarity)?.let { condition ->
            query.where { condition }
        }

        return query.count()
    }

    private fun buildSearchCondition(
        queryText: String?,
        setCode: String?,
        collectorNumber: String?,
        colors: List<String>?,
        colorIdentity: List<String>?,
        type: String?,
        rarity: String?,
    ): Op<Boolean>? {
        val conditions = mutableListOf<Op<Boolean>>()

        if (!queryText.isNullOrBlank()) {
            val pattern = "%${queryText.lowercase()}%"
            conditions.add(
                (CardsTable.name.lowerCase() like pattern) or
                    (CardsTable.typeLine.lowerCase() like pattern) or
                    (CardsTable.oracleText.lowerCase() like pattern),
            )
        }

        if (!setCode.isNullOrBlank()) {
            conditions.add(CardsTable.setCode eq setCode.lowercase())
        }

        if (!collectorNumber.isNullOrBlank()) {
            conditions.add(CardsTable.collectorNumber eq collectorNumber)
        }

        colors?.forEach { color ->
            conditions.add(stringParam(color.uppercase()) eq anyFrom(CardsTable.colors))
        }

        colorIdentity?.forEach { color ->
            conditions.add(stringParam(color.uppercase()) eq anyFrom(CardsTable.colorIdentity))
        }

        if (!type.isNullOrBlank()) {
            conditions.add(CardsTable.typeLine.lowerCase() like "%${type.lowercase()}%")
        }

        if (!rarity.isNullOrBlank()) {
            conditions.add(CardsTable.rarity eq rarity.lowercase())
        }

        return conditions.reduceOrNull { acc, op -> acc and op }
    }

    private fun resolveOrderColumn(order: CardSortOrder): Expression<*> = when (order) {
        CardSortOrder.NAME -> CardsTable.name
        CardSortOrder.SET -> CardsTable.setCode
        CardSortOrder.RELEASED -> CardsTable.releasedAt
        CardSortOrder.RARITY -> CardsTable.rarity
        CardSortOrder.COLOR -> CardsTable.colorIdentity
        CardSortOrder.USD -> CardsTable.priceUsd
        CardSortOrder.EUR -> CardsTable.priceEur
        CardSortOrder.CMC -> CardsTable.cmc
        CardSortOrder.POWER -> CardsTable.power
        CardSortOrder.TOUGHNESS -> CardsTable.toughness
        CardSortOrder.ARTIST -> CardsTable.artist
    }

    private fun resolveDirection(order: CardSortOrder, direction: SortDirection): SortOrder =
        when (direction) {
            SortDirection.ASC -> SortOrder.ASC
            SortDirection.DESC -> SortOrder.DESC
            SortDirection.AUTO -> when (order) {
                CardSortOrder.RELEASED -> SortOrder.DESC
                else -> SortOrder.ASC
            }
        }

    private fun ResultRow.toRecord(): CardRecord = CardRecord(
        id = this[CardsTable.id].value,
        scryfallId = UUID.fromString(this[CardsTable.scryfallId].toString()),
        oracleId = UUID.fromString(this[CardsTable.oracleId].toString()),
        name = this[CardsTable.name],
        lang = this[CardsTable.lang],
        layout = this[CardsTable.layout],
        manaCost = this[CardsTable.manaCost],
        cmc = this[CardsTable.cmc],
        typeLine = this[CardsTable.typeLine],
        oracleText = this[CardsTable.oracleText],
        colors = this[CardsTable.colors],
        colorIdentity = this[CardsTable.colorIdentity],
        keywords = this[CardsTable.keywords],
        power = this[CardsTable.power],
        toughness = this[CardsTable.toughness],
        loyalty = this[CardsTable.loyalty],
        setCode = this[CardsTable.setCode],
        setName = this[CardsTable.setName],
        collectorNumber = this[CardsTable.collectorNumber],
        rarity = this[CardsTable.rarity],
        releasedAt = this[CardsTable.releasedAt],
        imageUriSmall = this[CardsTable.imageUriSmall],
        imageUriNormal = this[CardsTable.imageUriNormal],
        imageUriLarge = this[CardsTable.imageUriLarge],
        imageUriPng = this[CardsTable.imageUriPng],
        imageUriArtCrop = this[CardsTable.imageUriArtCrop],
        imageUriBorderCrop = this[CardsTable.imageUriBorderCrop],
        priceUsd = this[CardsTable.priceUsd],
        priceUsdFoil = this[CardsTable.priceUsdFoil],
        priceEur = this[CardsTable.priceEur],
        priceEurFoil = this[CardsTable.priceEurFoil],
        flavorText = this[CardsTable.flavorText],
        artist = this[CardsTable.artist],
        mtgaId = this[CardsTable.mtgaId],
    )
}
