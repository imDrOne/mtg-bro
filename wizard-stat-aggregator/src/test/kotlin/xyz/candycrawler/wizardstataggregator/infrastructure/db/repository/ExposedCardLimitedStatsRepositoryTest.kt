package xyz.candycrawler.wizardstataggregator.infrastructure.db.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.exception.CardLimitedStatsNotFoundException
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStatsSearchCriteria
import xyz.candycrawler.wizardstataggregator.infrastructure.db.entity.CardLimitedStatsRecord
import xyz.candycrawler.wizardstataggregator.infrastructure.db.mapper.CardLimitedStatsSqlMapper
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class ExposedCardLimitedStatsRepositoryTest {

    @Mock
    private lateinit var sqlMapper: CardLimitedStatsSqlMapper

    @InjectMocks
    private lateinit var repository: ExposedCardLimitedStatsRepository

    // ---- saveAll ----

    @Test
    fun `saveAll delegates batch upsert to mapper`() {
        val stats = listOf(buildDomain(mtgaId = 1), buildDomain(mtgaId = 2))
        val expectedRecords = stats.map { it.toRecord() }

        repository.saveAll(stats)

        then(sqlMapper).should().upsertBatch(expectedRecords)
    }

    // ---- findById ----

    @Test
    fun `findById returns domain entity when record exists`() {
        val record = buildRecord(id = 10L)
        given(sqlMapper.selectById(10L)).willReturn(record)

        val result = repository.findById(10L)

        assertEquals(record.toDomain(), result)
    }

    @Test
    fun `findById throws CardLimitedStatsNotFoundException when record not found`() {
        given(sqlMapper.selectById(42L)).willReturn(null)

        assertFailsWith<CardLimitedStatsNotFoundException> {
            repository.findById(42L)
        }
    }

    // ---- findByMatchType ----

    @Test
    fun `findByMatchType returns mapped domain list`() {
        val records = listOf(buildRecord(mtgaId = 1), buildRecord(mtgaId = 2))
        given(sqlMapper.selectByMatchType("QuickDraft")).willReturn(records)

        val result = repository.findByMatchType("QuickDraft")

        assertEquals(records.map { it.toDomain() }, result)
    }

    @Test
    fun `findByMatchType returns empty list when no records exist`() {
        given(sqlMapper.selectByMatchType("Sealed")).willReturn(emptyList())

        val result = repository.findByMatchType("Sealed")

        assertEquals(emptyList(), result)
    }

    // ---- findByMtgaIdAndMatchType ----

    @Test
    fun `findByMtgaIdAndMatchType returns domain entity when record exists`() {
        val record = buildRecord(mtgaId = 5)
        given(sqlMapper.selectByMtgaIdAndMatchType(5, "DMU", "QuickDraft")).willReturn(record)

        val result = repository.findByMtgaIdAndMatchType(5, "DMU", "QuickDraft")

        assertEquals(record.toDomain(), result)
    }

    @Test
    fun `findByMtgaIdAndMatchType throws CardLimitedStatsNotFoundException when record not found`() {
        given(sqlMapper.selectByMtgaIdAndMatchType(99, "DMU", "Sealed")).willReturn(null)

        assertFailsWith<CardLimitedStatsNotFoundException> {
            repository.findByMtgaIdAndMatchType(99, "DMU", "Sealed")
        }
    }

    // ---- search ----

    @Test
    fun `search returns mapped page`() {
        val criteria = CardLimitedStatsSearchCriteria(setCode = "DMU", matchType = "QuickDraft", pageSize = 2)
        val records = listOf(buildRecord(mtgaId = 1), buildRecord(mtgaId = 2))
        given(sqlMapper.search(criteria)).willReturn(records)
        given(sqlMapper.countSearch(criteria)).willReturn(3)

        val result = repository.search(criteria)

        assertEquals(records.map { it.toDomain() }, result.stats)
        assertEquals(3, result.totalStats)
        assertEquals(true, result.hasMore)
        assertEquals(1, result.page)
        assertEquals(2, result.pageSize)
    }

    // ---- fixtures ----

    private fun buildRecord(
        id: Long? = null,
        mtgaId: Int = 1,
        setCode: String = "DMU",
        matchType: String = "QuickDraft",
        tier: String? = null,
    ): CardLimitedStatsRecord = CardLimitedStatsRecord(
        id = id,
        name = "Lightning Bolt",
        mtgaId = mtgaId,
        setCode = setCode,
        matchType = matchType,
        tier = tier,
        color = "R",
        rarity = "common",
        url = "https://example.com/card/$mtgaId",
        urlBack = "https://example.com/card/$mtgaId/back",
        types = listOf("Instant"),
        layout = "normal",
        seenCount = 1000,
        avgSeen = 2.34,
        pickCount = 800,
        avgPick = 3.12,
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

    private fun buildDomain(
        mtgaId: Int = 1,
        setCode: String = "DMU",
        matchType: String = "QuickDraft",
        tier: String? = null,
    ): CardLimitedStats = CardLimitedStats(
        id = null,
        name = "Lightning Bolt",
        mtgaId = mtgaId,
        setCode = setCode,
        matchType = matchType,
        tier = tier,
        color = "R",
        rarity = "common",
        url = "https://example.com/card/$mtgaId",
        urlBack = "https://example.com/card/$mtgaId/back",
        types = listOf("Instant"),
        layout = "normal",
        seenCount = 1000,
        avgSeen = 2.34,
        pickCount = 800,
        avgPick = 3.12,
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

    private fun CardLimitedStatsRecord.toDomain(): CardLimitedStats = CardLimitedStats(
        id = id,
        name = name,
        mtgaId = mtgaId,
        setCode = setCode,
        matchType = matchType,
        tier = tier,
        color = color,
        rarity = rarity,
        url = url,
        urlBack = urlBack,
        types = types,
        layout = layout,
        seenCount = seenCount,
        avgSeen = avgSeen,
        pickCount = pickCount,
        avgPick = avgPick,
        gameCount = gameCount,
        poolCount = poolCount,
        playRate = playRate,
        winRate = winRate,
        openingHandGameCount = openingHandGameCount,
        openingHandWinRate = openingHandWinRate,
        drawnGameCount = drawnGameCount,
        drawnWinRate = drawnWinRate,
        everDrawnGameCount = everDrawnGameCount,
        everDrawnWinRate = everDrawnWinRate,
        neverDrawnGameCount = neverDrawnGameCount,
        neverDrawnWinRate = neverDrawnWinRate,
        drawnImprovementWinRate = drawnImprovementWinRate,
    )

    private fun CardLimitedStats.toRecord(): CardLimitedStatsRecord = CardLimitedStatsRecord(
        id = id,
        name = name,
        mtgaId = mtgaId,
        setCode = setCode,
        matchType = matchType,
        tier = tier,
        color = color,
        rarity = rarity,
        url = url,
        urlBack = urlBack,
        types = types,
        layout = layout,
        seenCount = seenCount,
        avgSeen = avgSeen,
        pickCount = pickCount,
        avgPick = avgPick,
        gameCount = gameCount,
        poolCount = poolCount,
        playRate = playRate,
        winRate = winRate,
        openingHandGameCount = openingHandGameCount,
        openingHandWinRate = openingHandWinRate,
        drawnGameCount = drawnGameCount,
        drawnWinRate = drawnWinRate,
        everDrawnGameCount = everDrawnGameCount,
        everDrawnWinRate = everDrawnWinRate,
        neverDrawnGameCount = neverDrawnGameCount,
        neverDrawnWinRate = neverDrawnWinRate,
        drawnImprovementWinRate = drawnImprovementWinRate,
    )
}
