package xyz.candycrawler.wizardstataggregator.application.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.BDDMockito.willThrow
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.CardLimitedStats
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository.CardLimitedStatsRepository
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.Lands17ApiClientFacade
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.dto.response.CardStatsResponse
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.mapper.CardStatsResponseMapper

@ExtendWith(MockitoExtension::class)
class CardLimitedStatsCollectionServiceTest {

    @Mock
    private lateinit var lands17ApiClientFacade: Lands17ApiClientFacade

    @Mock
    private lateinit var cardLimitedStatsRepository: CardLimitedStatsRepository

    @Mock
    private lateinit var alertService: CardLimitedStatsAlertService

    private val mapper = CardStatsResponseMapper()

    @Test
    fun `collectAll sends start and finish alerts when all match types succeed`() = runBlocking {
        whenever(lands17ApiClientFacade.getDraftStatistic("BLB")).thenReturn(listOf(response("Draft Card", 1)))
        whenever(lands17ApiClientFacade.getSealedStatistic("BLB")).thenReturn(listOf(response("Sealed Card", 2)))
        val service = service()

        service.collectAll("BLB")

        then(alertService).should().collectionStarted("BLB")
        then(alertService).should().collectionFinished("BLB", successfulMatchTypes = 2, totalMatchTypes = 2)
        then(cardLimitedStatsRepository).should(atLeastOnce()).saveAll(any())
    }

    @Test
    fun `collectAll sends parsing alert and still finishes when one match type fails to parse`() = runBlocking {
        whenever(lands17ApiClientFacade.getDraftStatistic("BLB")).thenThrow(RuntimeException("17lands unavailable"))
        whenever(lands17ApiClientFacade.getSealedStatistic("BLB")).thenReturn(listOf(response("Sealed Card", 2)))
        val service = service()

        service.collectAll("BLB")

        then(alertService).should().parsingFailed(eq("BLB"), eq("QuickDraft"), any())
        then(alertService).should().collectionFinished("BLB", successfulMatchTypes = 1, totalMatchTypes = 2)
    }

    @Test
    fun `collectAll sends db alert when saving fails`() = runBlocking {
        whenever(lands17ApiClientFacade.getDraftStatistic("BLB")).thenReturn(listOf(response("Draft Card", 1)))
        whenever(lands17ApiClientFacade.getSealedStatistic("BLB")).thenReturn(listOf(response("Sealed Card", 2)))
        willThrow(RuntimeException("db down")).given(cardLimitedStatsRepository).saveAll(any<List<CardLimitedStats>>())
        val service = service()

        service.collectAll("BLB")

        then(alertService).should(atLeastOnce()).savingFailed(eq("BLB"), any(), eq(1), any())
        then(alertService).should().collectionFinished("BLB", successfulMatchTypes = 0, totalMatchTypes = 2)
    }

    private fun service() = CardLimitedStatsCollectionService(
        lands17ApiClientFacade = lands17ApiClientFacade,
        cardLimitedStatsRepository = cardLimitedStatsRepository,
        mapper = mapper,
        alertService = alertService,
    )

    private fun response(name: String, mtgaId: Int) = CardStatsResponse(
        name = name,
        mtgaId = mtgaId,
        color = "W",
        rarity = "common",
        url = "https://example.com/$mtgaId",
        urlBack = "",
        types = listOf("Creature"),
        layout = "normal",
        seenCount = 10,
        avgSeen = 1.0,
        pickCount = 5,
        avgPick = 2.0,
        gameCount = 3,
        poolCount = 4,
        playRate = 0.5,
        winRate = 0.5,
        openingHandGameCount = 1,
        openingHandWinRate = 0.5,
        drawnGameCount = 1,
        drawnWinRate = 0.5,
        everDrawnGameCount = 1,
        everDrawnWinRate = 0.5,
        neverDrawnGameCount = 1,
        neverDrawnWinRate = 0.5,
        drawnImprovementWinRate = 0.0,
    )
}
