package xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSearchCriteria
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSortDirection
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSortOrder
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

    @Test
    fun `search filters by set code match type names mtga ids and win rate`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(
                    mtgaId = 800,
                    name = "Premium Common",
                    setCode = "DMU",
                    matchType = "QuickDraft",
                    winRate = 0.62,
                ),
                buildRecord(
                    mtgaId = 801,
                    name = "Medium Common",
                    setCode = "DMU",
                    matchType = "QuickDraft",
                    winRate = 0.55,
                ),
                buildRecord(
                    mtgaId = 802,
                    name = "Wrong Set",
                    setCode = "BRO",
                    matchType = "QuickDraft",
                    winRate = 0.65,
                ),
                buildRecord(mtgaId = 803, name = "Wrong Match", setCode = "DMU", matchType = "Sealed", winRate = 0.67),
            ),
        )

        val result = sqlMapper.search(
            CardLimitedStatsSearchCriteria(
                setCode = "dmu",
                matchType = "QuickDraft",
                names = listOf("premium common", "wrong set"),
                mtgaIds = listOf(800, 802),
                minWinRate = 0.60,
            ),
        )

        assertEquals(listOf(800), result.map { it.mtgaId })
    }

    @Test
    fun `search sorts paginates and counts results`() {
        sqlMapper.upsertBatch(
            listOf(
                buildRecord(mtgaId = 900, name = "Low", winRate = 0.51),
                buildRecord(mtgaId = 901, name = "High", winRate = 0.64),
                buildRecord(mtgaId = 902, name = "Middle", winRate = 0.57),
            ),
        )
        val criteria = CardLimitedStatsSearchCriteria(
            setCode = "DMU",
            matchType = "QuickDraft",
            order = CardLimitedStatsSortOrder.WIN_RATE,
            direction = CardLimitedStatsSortDirection.DESC,
            page = 1,
            pageSize = 2,
        )

        val result = sqlMapper.search(criteria)
        val count = sqlMapper.countSearch(criteria)

        assertEquals(listOf(901, 902), result.map { it.mtgaId })
        assertEquals(3, count)
    }

    private fun buildRecord(
        mtgaId: Int,
        name: String = "Lightning Bolt",
        setCode: String = "DMU",
        matchType: String = "QuickDraft",
        avgSeen: Double? = 2.34,
        avgPick: Double? = 3.12,
        winRate: Double = 0.58,
    ): CardLimitedStatsRecord = CardLimitedStatsRecord(
        id = null,
        name = name,
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
        winRate = winRate,
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
