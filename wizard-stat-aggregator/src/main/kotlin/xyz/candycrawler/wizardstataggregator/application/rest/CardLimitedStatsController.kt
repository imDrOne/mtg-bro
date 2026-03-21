package xyz.candycrawler.wizardstataggregator.application.rest

import kotlinx.coroutines.launch
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import xyz.candycrawler.wizardstataggregator.application.rest.dto.request.CollectCardLimitedStatsRequest
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.CardLimitedStatsResponse
import xyz.candycrawler.wizardstataggregator.application.rest.dto.response.CollectCardLimitedStatsResponse
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsCollectionService
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsQueryService
import xyz.candycrawler.wizardstataggregator.configuration.coroutine.ApplicationCoroutineScope
import xyz.candycrawler.wizardstataggregator.domain.stat.limited.model.MatchType

@RestController
@RequestMapping("/api/v1/card-limited-stats")
class CardLimitedStatsController(
    private val collectionService: CardLimitedStatsCollectionService,
    private val queryService: CardLimitedStatsQueryService,
    private val coroutineScope: ApplicationCoroutineScope,
) {

    @GetMapping
    fun getStats(
        @RequestParam name: String,
        @RequestParam setCode: String,
        @RequestParam matchType: MatchType,
    ): CardLimitedStatsResponse =
        CardLimitedStatsResponse.from(queryService.getByCard(name, setCode, matchType))

    @PostMapping("/collect")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun collect(@RequestBody request: CollectCardLimitedStatsRequest): CollectCardLimitedStatsResponse {
        coroutineScope.launch {
            collectionService.collectAll(request.setCode)
        }

        return CollectCardLimitedStatsResponse(
            setCode = request.setCode,
            message = "Collection started for set ${request.setCode}",
        )
    }
}
