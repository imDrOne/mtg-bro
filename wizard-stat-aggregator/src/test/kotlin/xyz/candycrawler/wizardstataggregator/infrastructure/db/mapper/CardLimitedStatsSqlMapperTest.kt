package xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.CardLimitedStatsRecord
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Transactional
class CardLimitedStatsSqlMapperTest(@Autowired private val sqlMapper: CardLimitedStatsSqlMapper) :
    AbstractIntegrationTest() {

    @Test
    fun `insertBatch persists all fields correctly`() {
        val record = buildRecord(mtgaId = 100)

        sqlMapper.upsertBatch(listOf(record))

        val result = sqlMapper.selectByMtgaIdAndMatchType(record.mtgaId, record.setCode, record.matchType)

        assertNotNull(result)
        assertNotNull(result.id)
        assertEquals(record.name, result.name)
        assertEquals(record.mtgaId, result.mtgaId)
        assertEquals(record.setCode, result.setCode)
        assertEquals(record.matchType, result.matchType)
        assertEquals(record.color, result.color)
        assertEquals(record.rarity, result.rarity)
        assertEquals(record.url, result.url)
        assertEquals(record.urlBack, result.urlBack)
        assertEquals(record.types, result.types)
        assertEquals(record.layout, result.layout)
        assertEquals(record.seenCount, result.seenCount)
        assertEquals(record.avgSeen, result.avgSeen)
        assertEquals(record.pickCount, result.pickCount)
        assertEquals(record.avgPick, result.avgPick)
        assertEquals(record.gameCount, result.gameCount)
        assertEquals(record.poolCount, result.poolCount)
        assertEquals(record.playRate, result.playRate)
        assertEquals(record.winRate, result.winRate)
        assertEquals(record.openingHandGameCount, result.openingHandGameCount)
        assertEquals(record.openingHandWinRate, result.openingHandWinRate)
        assertEquals(record.drawnGameCount, result.drawnGameCount)
        assertEquals(record.drawnWinRate, result.drawnWinRate)
        assertEquals(record.everDrawnGameCount, result.everDrawnGameCount)
        assertEquals(record.everDrawnWinRate, result.everDrawnWinRate)
        assertEquals(record.neverDrawnGameCount, result.neverDrawnGameCount)
        assertEquals(record.neverDrawnWinRate, result.neverDrawnWinRate)
        assertEquals(record.drawnImprovementWinRate, result.drawnImprovementWinRate)
    }

    @Test
    fun `insertBatch persists multiple records`() {
        val records = listOf(
            buildRecord(mtgaId = 200, matchType = "QuickDraft"),
            buildRecord(mtgaId = 201, matchType = "QuickDraft"),
            buildRecord(mtgaId = 202, matchType = "QuickDraft"),
        )

        sqlMapper.upsertBatch(records)

        val results = sqlMapper.selectByMatchType("QuickDraft")
        val insertedMtgaIds = results.map { it.mtgaId }
        assert(insertedMtgaIds.containsAll(listOf(200, 201, 202)))
    }

    @Test
    fun `insertBatch persists null nullable fields`() {
        val record = buildRecord(mtgaId = 300, avgSeen = null, avgPick = null)

        sqlMapper.upsertBatch(listOf(record))

        val result = sqlMapper.selectByMtgaIdAndMatchType(record.mtgaId, record.setCode, record.matchType)

        assertNotNull(result)
        assertNull(result.avgSeen)
        assertNull(result.avgPick)
    }

    @Test
    fun `selectByMatchType returns only records with given matchType`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(mtgaId = 400, matchType = "QuickDraft"),
                buildRecord(mtgaId = 401, matchType = "QuickDraft"),
                buildRecord(mtgaId = 402, matchType = "Sealed"),
            ),
        )

        val quickDraftResults = sqlMapper.selectByMatchType("QuickDraft")
        val sealedResults = sqlMapper.selectByMatchType("Sealed")

        assert(quickDraftResults.map { it.mtgaId }.containsAll(listOf(400, 401)))
        assert(quickDraftResults.none { it.mtgaId == 402 })
        assert(sealedResults.map { it.mtgaId }.contains(402))
        assert(sealedResults.none { it.mtgaId == 400 || it.mtgaId == 401 })
    }

    @Test
    fun `selectByMatchType returns empty list when no records match`() {
        val result = sqlMapper.selectByMatchType("NonExistentMatchType")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `selectByMtgaIdAndMatchType returns record when found`() {
        val record = buildRecord(mtgaId = 500, matchType = "Sealed")
        sqlMapper.upsertBatch(listOf(record))

        val result = sqlMapper.selectByMtgaIdAndMatchType(500, "DMU", "Sealed")

        assertNotNull(result)
        assertEquals(500, result.mtgaId)
        assertEquals("DMU", result.setCode)
        assertEquals("Sealed", result.matchType)
    }

    @Test
    fun `selectByMtgaIdAndMatchType returns null when mtgaId not found`() {
        val result = sqlMapper.selectByMtgaIdAndMatchType(999999, "DMU", "QuickDraft")

        assertNull(result)
    }

    @Test
    fun `selectByMtgaIdAndMatchType returns null when matchType does not match`() {
        sqlMapper.upsertBatch(listOf(buildRecord(mtgaId = 600, matchType = "QuickDraft")))

        val result = sqlMapper.selectByMtgaIdAndMatchType(600, "DMU", "Sealed")

        assertNull(result)
    }

    @Test
    fun `selectByMtgaIdAndMatchType returns null when setCode does not match`() {
        sqlMapper.upsertBatch(listOf(buildRecord(mtgaId = 700, matchType = "QuickDraft", setCode = "DMU")))

        val result = sqlMapper.selectByMtgaIdAndMatchType(700, "BRO", "QuickDraft")

        assertNull(result)
    }

    private fun buildRecord(
        mtgaId: Int,
        setCode: String = "DMU",
        matchType: String = "QuickDraft",
        avgSeen: Double? = 2.34,
        avgPick: Double? = 3.12,
    ): CardLimitedStatsRecord = CardLimitedStatsRecord(
        id = null,
        name = "Lightning Bolt",
        mtgaId = mtgaId,
        setCode = setCode,
        matchType = matchType,
        color = "R",
        rarity = "common",
        url = "https://example.com/card/$mtgaId",
        urlBack = "https://example.com/card/$mtgaId/back",
        types = listOf("Instant"),
        layout = "normal",
        seenCount = 1000,
        avgSeen = avgSeen,
        pickCount = 800,
        avgPick = avgPick,
        gameCount = 600,
        poolCount = 820,
        playRate = 0.73,
        winRate = 0.58,
        openingHandGameCount = 120,
        openingHandWinRate = 0.61,
        drawnGameCount = 300,
        drawnWinRate = 0.59,
        everDrawnGameCount = 420,
        everDrawnWinRate = 0.60,
        neverDrawnGameCount = 180,
        neverDrawnWinRate = 0.54,
        drawnImprovementWinRate = 0.06,
    )
}
