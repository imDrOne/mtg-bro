package xyz.candycrawler.collectionmanager.infrastructure.db.mapper.sql

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component
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
        }.map { it.toRecord() }
    }

    internal fun selectById(id: Long): CardRecord? =
        CardsTable.selectAll()
            .where { CardsTable.id eq id }
            .map { it.toRecord() }
            .singleOrNull()

    internal fun selectBySetCodeAndCollectorNumber(setCode: String, collectorNumber: String): CardRecord? =
        CardsTable.selectAll()
            .where {
                (CardsTable.setCode eq setCode) and
                (CardsTable.collectorNumber eq collectorNumber)
            }
            .map { it.toRecord() }
            .singleOrNull()

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
    )
}
