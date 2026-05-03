package xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.CardLimitedStatsRecord
import xyz.candycrawler.wizardstataggregator.infrastructure.db.table.CardLimitedStatsTable

@Component
class CardLimitedStatsSqlMapper {

    internal fun upsertBatch(records: List<CardLimitedStatsRecord>) {
        CardLimitedStatsTable.batchUpsert(
            records,
            keys = arrayOf(
                CardLimitedStatsTable.mtgaId,
                CardLimitedStatsTable.setCode,
                CardLimitedStatsTable.matchType,
            ),
            shouldReturnGeneratedValues = false,
        ) { record ->
            this[CardLimitedStatsTable.name] = record.name
            this[CardLimitedStatsTable.mtgaId] = record.mtgaId
            this[CardLimitedStatsTable.setCode] = record.setCode
            this[CardLimitedStatsTable.matchType] = record.matchType
            this[CardLimitedStatsTable.color] = record.color
            this[CardLimitedStatsTable.rarity] = record.rarity
            this[CardLimitedStatsTable.url] = record.url
            this[CardLimitedStatsTable.urlBack] = record.urlBack
            this[CardLimitedStatsTable.types] = record.types
            this[CardLimitedStatsTable.layout] = record.layout
            this[CardLimitedStatsTable.seenCount] = record.seenCount
            this[CardLimitedStatsTable.avgSeen] = record.avgSeen
            this[CardLimitedStatsTable.pickCount] = record.pickCount
            this[CardLimitedStatsTable.avgPick] = record.avgPick
            this[CardLimitedStatsTable.gameCount] = record.gameCount
            this[CardLimitedStatsTable.poolCount] = record.poolCount
            this[CardLimitedStatsTable.playRate] = record.playRate
            this[CardLimitedStatsTable.winRate] = record.winRate
            this[CardLimitedStatsTable.openingHandGameCount] = record.openingHandGameCount
            this[CardLimitedStatsTable.openingHandWinRate] = record.openingHandWinRate
            this[CardLimitedStatsTable.drawnGameCount] = record.drawnGameCount
            this[CardLimitedStatsTable.drawnWinRate] = record.drawnWinRate
            this[CardLimitedStatsTable.everDrawnGameCount] = record.everDrawnGameCount
            this[CardLimitedStatsTable.everDrawnWinRate] = record.everDrawnWinRate
            this[CardLimitedStatsTable.neverDrawnGameCount] = record.neverDrawnGameCount
            this[CardLimitedStatsTable.neverDrawnWinRate] = record.neverDrawnWinRate
            this[CardLimitedStatsTable.drawnImprovementWinRate] = record.drawnImprovementWinRate
        }
    }

    internal fun selectById(id: Long): CardLimitedStatsRecord? = CardLimitedStatsTable.selectAll()
        .where { CardLimitedStatsTable.id eq id }
        .map { it.toRecord() }
        .singleOrNull()

    internal fun selectByMatchType(matchType: String): List<CardLimitedStatsRecord> = CardLimitedStatsTable.selectAll()
        .where { CardLimitedStatsTable.matchType eq matchType }
        .map { it.toRecord() }

    internal fun selectByMtgaIdAndMatchType(mtgaId: Int, setCode: String, matchType: String): CardLimitedStatsRecord? =
        CardLimitedStatsTable.selectAll()
            .where {
                (CardLimitedStatsTable.mtgaId eq mtgaId) and
                    (CardLimitedStatsTable.setCode eq setCode) and
                    (CardLimitedStatsTable.matchType eq matchType)
            }
            .map { it.toRecord() }
            .singleOrNull()

    private fun ResultRow.toRecord(): CardLimitedStatsRecord = CardLimitedStatsRecord(
        id = this[CardLimitedStatsTable.id].value,
        name = this[CardLimitedStatsTable.name],
        mtgaId = this[CardLimitedStatsTable.mtgaId],
        setCode = this[CardLimitedStatsTable.setCode],
        matchType = this[CardLimitedStatsTable.matchType],
        color = this[CardLimitedStatsTable.color],
        rarity = this[CardLimitedStatsTable.rarity],
        url = this[CardLimitedStatsTable.url],
        urlBack = this[CardLimitedStatsTable.urlBack],
        types = this[CardLimitedStatsTable.types],
        layout = this[CardLimitedStatsTable.layout],
        seenCount = this[CardLimitedStatsTable.seenCount],
        avgSeen = this[CardLimitedStatsTable.avgSeen],
        pickCount = this[CardLimitedStatsTable.pickCount],
        avgPick = this[CardLimitedStatsTable.avgPick],
        gameCount = this[CardLimitedStatsTable.gameCount],
        poolCount = this[CardLimitedStatsTable.poolCount],
        playRate = this[CardLimitedStatsTable.playRate],
        winRate = this[CardLimitedStatsTable.winRate],
        openingHandGameCount = this[CardLimitedStatsTable.openingHandGameCount],
        openingHandWinRate = this[CardLimitedStatsTable.openingHandWinRate],
        drawnGameCount = this[CardLimitedStatsTable.drawnGameCount],
        drawnWinRate = this[CardLimitedStatsTable.drawnWinRate],
        everDrawnGameCount = this[CardLimitedStatsTable.everDrawnGameCount],
        everDrawnWinRate = this[CardLimitedStatsTable.everDrawnWinRate],
        neverDrawnGameCount = this[CardLimitedStatsTable.neverDrawnGameCount],
        neverDrawnWinRate = this[CardLimitedStatsTable.neverDrawnWinRate],
        drawnImprovementWinRate = this[CardLimitedStatsTable.drawnImprovementWinRate],
    )
}
