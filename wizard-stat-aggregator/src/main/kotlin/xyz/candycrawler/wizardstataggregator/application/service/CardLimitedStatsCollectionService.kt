package xyz.candycrawler.wizardstataggregator.application.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.repository.CardLimitedStatsRepository
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.Lands17ApiClient.MatchType
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.Lands17ApiClientFacade
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.mapper.CardStatsResponseMapper

@Service
class CardLimitedStatsCollectionService(
    private val lands17ApiClientFacade: Lands17ApiClientFacade,
    private val cardLimitedStatsRepository: CardLimitedStatsRepository,
    private val mapper: CardStatsResponseMapper,
    private val alertService: CardLimitedStatsAlertService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun collectAll(setCode: String) {
        log.info("Starting card limited stats collection for set={}", setCode)
        alertService.collectionStarted(setCode)

        val results = supervisorScope {
            listOf(
                async(Dispatchers.IO) { collectForMatchType(setCode, MatchType.QUICK_DRAFT) },
                async(Dispatchers.IO) { collectForMatchType(setCode, MatchType.SEALED) },
            ).awaitAll()
        }

        log.info("Card limited stats collection finished for set={}", setCode)
        alertService.collectionFinished(
            setCode = setCode,
            successfulMatchTypes = results.count { it },
            totalMatchTypes = results.size,
        )
    }

    private fun collectForMatchType(setCode: String, matchType: MatchType): Boolean {
        log.info("Fetching {} stats for set={}", matchType.value, setCode)

        val domain = runCatching {
            val responses = when (matchType) {
                MatchType.QUICK_DRAFT -> lands17ApiClientFacade.getDraftStatistic(setCode)
                MatchType.SEALED -> lands17ApiClientFacade.getSealedStatistic(setCode)
            }
            responses.map { mapper.toDomain(it, setCode, matchType.value) }
        }.onFailure {
            log.error("Failed to parse {} stats for set={}: {}", matchType.value, setCode, it.message, it)
            alertService.parsingFailed(setCode, matchType.value, it)
        }.getOrNull() ?: return false

        return runCatching {
            cardLimitedStatsRepository.saveAll(domain)
            log.info("Saved {} {} records for set={}", domain.size, matchType.value, setCode)
        }.onFailure {
            log.error("Failed to save {} stats for set={}: {}", matchType.value, setCode, it.message, it)
            alertService.savingFailed(setCode, matchType.value, domain.size, it)
        }.isSuccess
    }
}
